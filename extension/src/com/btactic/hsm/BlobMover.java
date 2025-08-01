/*
 * ***** BEGIN LICENSE BLOCK *****
 * Maldua Zimbra HSM Extension
 * Copyright (C) 2023 BTACTIC, S.C.C.L.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.btactic.hsm;

import com.zimbra.common.service.ServiceException;

import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapProtocol;

import com.zimbra.common.util.ZimbraLog;

import com.zimbra.cs.account.soap.SoapProvisioning;

import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.DbConnection;

import com.zimbra.cs.index.SearchParams;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.index.ZimbraQuery;
import com.zimbra.cs.index.ZimbraQueryResults;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxMaintenance;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.OperationContext;

import com.zimbra.cs.store.file.FileBlobStore;
import com.zimbra.cs.store.MailboxBlob;
import com.zimbra.cs.store.StoreManager;

import com.zimbra.cs.util.IOUtil;

import com.zimbra.cs.volume.Volume;

import com.zimbra.soap.admin.message.GetAllMailboxesRequest;
import com.zimbra.soap.admin.message.GetAllMailboxesResponse;
import com.zimbra.soap.admin.message.GetAllVolumesRequest;
import com.zimbra.soap.admin.message.GetAllVolumesResponse;

import com.zimbra.soap.admin.type.MailboxInfo;
import com.zimbra.soap.admin.type.VolumeInfo;

import com.zimbra.soap.JaxbUtil;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

public class BlobMover {

    private HashMap<String, MailboxBlob> mAllDestinationBlobs = null;
    private FileBlobStore mStore = (FileBlobStore) StoreManager.getInstance();

    private List<Integer> getAllMailboxIds(SoapProvisioning prov)
    throws ServiceException {
        List<Integer> ids = new ArrayList<Integer>();
        GetAllMailboxesRequest request = new GetAllMailboxesRequest();
        Element requestElement = JaxbUtil.jaxbToElement(request);
        Element respElem = prov.invoke(requestElement);
        GetAllMailboxesResponse response = JaxbUtil.elementToJaxb(respElem);
        for (MailboxInfo mailboxInfo : response.getMboxes()) {
            ids.add(mailboxInfo.getId());
        }
        return ids;
    }

    private List<Short> getValidOriginLocators(SoapProvisioning prov, int destinationLocator) throws ServiceException {
        List<Short> validOriginLocators = new ArrayList<Short>();

        GetAllVolumesRequest request = new GetAllVolumesRequest();
        Element requestElement = JaxbUtil.jaxbToElement(request);
        Element respElem = prov.invoke(requestElement);
        GetAllVolumesResponse response = JaxbUtil.elementToJaxb(respElem);

        for (VolumeInfo volumeInfo : response.getVolumes()) {

            if (volumeInfo.getId() == destinationLocator) {
                break;
            }

            if (volumeInfo.getType() == Volume.TYPE_INDEX) {
                break;
            }

            if ((Volume.StoreType.getStoreTypeBy(volumeInfo.getStoreType()).equals(Volume.StoreType.INTERNAL)) && (volumeInfo.getStoreManagerClass().equals("com.zimbra.cs.store.file.FileBlobStore"))) {
                validOriginLocators.add(volumeInfo.getId());
            }
        }

        return validOriginLocators;
    }

    private void filterAndAddToFilteredItemIds(DbConnection dbConnection, Mailbox mbox, List<Integer> zimbraQueryPreFilterItemsChunk, List<MovedItemInfo> zimbraQueryPostFilterItemsInfos, String validOriginLocatorsString) throws ServiceException {
        if (!(zimbraQueryPreFilterItemsChunk.isEmpty())) {
            DbBlobFilter dbBlobFilter = new DbBlobFilter ();
            List<MovedItemInfo> filteredItemsInfos = dbBlobFilter.filterItemsByLocation(dbConnection, mbox, zimbraQueryPreFilterItemsChunk, validOriginLocatorsString);
            zimbraQueryPostFilterItemsInfos.addAll(filteredItemsInfos);
        }
    }

    public void moveItems(Mailbox mbox, Integer mboxId, String hsmTypesString, String hsmSearchQueryString, short destinationLocator, String validOriginLocatorsString) throws ServiceException {
        DbConnection dbConnection = null;

        SearchParams params = new SearchParams();
        params.setQueryString(hsmSearchQueryString);
        params.setSortBy(SortBy.NONE);
        params.setTypes(hsmTypesString);
        params.setFetchMode(SearchParams.Fetch.IDS);

        ZimbraQuery query = new ZimbraQuery(new OperationContext(mbox), SoapProtocol.Soap12, mbox, params);
        ZimbraQueryResults result = query.execute();

        List<Integer> zimbraQueryPreFilterItemsChunk = new ArrayList<Integer>();
        List<MovedItemInfo> zimbraQueryPostFilterItemsInfos = new ArrayList<MovedItemInfo>();
        int zimbraQueryPreFilterChunkSize = 100; // TODO: Optional parametre that you can set to speed up queries
        int zimbraQueryPreFilterCounter = 0;

        try {
            dbConnection = DbPool.getConnection(mbox);
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("ZetaHsm: Failed to get a dbConnection (filter)", e);
        }

        try {
            while (result.hasNext()) {
                zimbraQueryPreFilterCounter = zimbraQueryPreFilterCounter + 1;
                int itemId = result.getNext().getItemId();
                zimbraQueryPreFilterItemsChunk.add(itemId);
                if (zimbraQueryPreFilterCounter == zimbraQueryPreFilterChunkSize) {
                    filterAndAddToFilteredItemIds (dbConnection, mbox, zimbraQueryPreFilterItemsChunk, zimbraQueryPostFilterItemsInfos, validOriginLocatorsString);
                    zimbraQueryPreFilterItemsChunk = new ArrayList<Integer>();
                    zimbraQueryPreFilterCounter = 0;
                }
                // ZimbraLog.misc.info("DEBUG: mailboxId (Pre Filter): " + mboxId + " ItemId: '" + itemId + "'" + ".");
            }
            filterAndAddToFilteredItemIds (dbConnection, mbox, zimbraQueryPreFilterItemsChunk, zimbraQueryPostFilterItemsInfos, validOriginLocatorsString);
            zimbraQueryPreFilterItemsChunk = new ArrayList<Integer>();
            zimbraQueryPreFilterCounter = 0;

            IOUtil.closeQuietly(result);
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("ZetaHsm: Unknown DB problem (filter)", e);
        } finally {
            DbPool.quietClose(dbConnection);
        }

        for (MovedItemInfo zimbraQueryPostFilterItemsInfo : zimbraQueryPostFilterItemsInfos) {
            ZimbraLog.misc.info("DEBUG: mailboxId (Post Filter): " + mboxId + " ItemId: '" + zimbraQueryPostFilterItemsInfo.getId() + "'" + ".");
        }

        moveItems(mbox, destinationLocator, zimbraQueryPostFilterItemsInfos);

    }

    public void moveItems(SoapProvisioning prov, String hsmTypesString, String hsmSearchQueryString, short destinationLocator, long maximumBytes) throws ServiceException {
        mAllDestinationBlobs = new HashMap<String, MailboxBlob>();
        List<Short> validOriginLocators = getValidOriginLocators(prov, destinationLocator);

        if (validOriginLocators.isEmpty()) {
            ZimbraLog.misc.info("No valid origin volume Ids for this zimbraHsmPolicy. Skipping.");
            return;
        }

        String validOriginLocatorsString = StringUtils.join(validOriginLocators, ",");
        ZimbraLog.misc.info("DEBUG: validOriginLocatorsString: '" + validOriginLocatorsString + "'" + ".");

        List<Integer> mailboxIds = getAllMailboxIds(prov);
        for (int mboxId : mailboxIds) {
            ZimbraLog.misc.info("DEBUG: mailbox: " + mboxId + " - hsmTypesString: '" + hsmTypesString + "' - hsmSearchQueryString: '" + hsmSearchQueryString + "' - destinationLocator: " + destinationLocator + ".");

            Mailbox mbox = MailboxManager.getInstance().getMailboxById(mboxId);
            moveItems(mbox, mboxId, hsmTypesString, hsmSearchQueryString, destinationLocator, validOriginLocatorsString, maximumBytes);
        }
    }

    public void moveItems(SoapProvisioning prov, String hsmTypesString, String hsmSearchQueryString, short destinationLocator) throws ServiceException {
        moveItems(prov, hsmTypesString, hsmSearchQueryString, destinationLocator, 0L);
    }

    private void moveItems(Mailbox mbox, short destinationLocator, List<MovedItemInfo> itemsToMigrateInfos) throws ServiceException {
        DbConnection dbConnection = null;
        Iterator itemsToMigrateInfosIter = itemsToMigrateInfos.iterator();
        List<MovedItemInfo> itemsInfosToMigrateChunk = new ArrayList<MovedItemInfo>();
        int movedItemInfoChunkSize = 100; // TODO: Optional parametre that you can set to speed up queries
        int movedItemInfoCounter = 0;

        try {
            dbConnection = DbPool.getConnection(mbox);
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("ZetaHsm: Failed to get a dbConnection (move)", e);
        }

        try {
            while (itemsToMigrateInfosIter.hasNext()) {
                movedItemInfoCounter = movedItemInfoCounter + 1;
                MovedItemInfo movedItemInfo = (MovedItemInfo) itemsToMigrateInfosIter.next();
                itemsInfosToMigrateChunk.add(movedItemInfo);
                if (movedItemInfoCounter == movedItemInfoChunkSize) {
                    moveChunkItems(dbConnection, mbox, destinationLocator, itemsInfosToMigrateChunk);
                    itemsInfosToMigrateChunk = new ArrayList<MovedItemInfo>();
                    movedItemInfoCounter = 0;
                }
            }
            if (itemsInfosToMigrateChunk.size() >= 1) {
                moveChunkItems(dbConnection, mbox, destinationLocator, itemsInfosToMigrateChunk);
            }
            itemsInfosToMigrateChunk = new ArrayList<MovedItemInfo>();
            movedItemInfoCounter = 0;
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("ZetaHsm: Unknown DB problem (move)", e);
        } finally {
            DbPool.quietClose(dbConnection);
        }
    }

    private void moveChunkItems(DbConnection dbConnection, Mailbox mbox, short destinationLocator, List<MovedItemInfo> itemsToMigrateInfos) throws ServiceException {

        List<MailboxBlob> originBlobs = new ArrayList<MailboxBlob>();
        ZimbraLog.misc.info("DEBUG: Moving " + itemsToMigrateInfos.size() + " messages.");
        MailboxBlob originBlob = null;

        Map<String, MailboxBlob> destinationBlobMap = new HashMap<String, MailboxBlob>(); // Fast lookup by digest
        List<MailboxBlob> destinationBlobList = new ArrayList<MailboxBlob>(); // Deletion in case of error
        MailboxMaintenance maintenance = null;

        try {
            maintenance = MailboxManager.getInstance().beginMaintenance(mbox.getAccountId(), mbox.getId());

            Iterator itemsToMigrateInfosIter = itemsToMigrateInfos.iterator();
            while (itemsToMigrateInfosIter.hasNext()) {
                MovedItemInfo movedItemInfo = (MovedItemInfo) itemsToMigrateInfosIter.next();

                // Copy blob to destination
                originBlob = mStore.getMailboxBlob(mbox, movedItemInfo.getId(), movedItemInfo.getModContent(), String.valueOf(movedItemInfo.getLocator()));
                if (originBlob != null) {
                    MailboxBlob destinationBlob = null;

                    try {
                        // Link to the copy if the original is already there
                        MailboxBlob linkSource = (MailboxBlob) mAllDestinationBlobs.get(movedItemInfo.getBlobDigest());
                        if (linkSource == null) {
                            linkSource = (MailboxBlob) destinationBlobMap.get(movedItemInfo.getBlobDigest());
                            if (linkSource == null) {
                                linkSource = originBlob;
                            }
                        }

                        // Blob link is created
                        destinationBlob = mStore.link(linkSource.getLocalBlob(), mbox, movedItemInfo.getId(), movedItemInfo.getModContent(), destinationLocator);
                    } catch (IOException e) {
                        throw ServiceException.FAILURE(
                            "Unable to copy " + originBlob + " to location: " + destinationLocator, e);
                    }

                    originBlobs.add(originBlob);
                    destinationBlobMap.put(movedItemInfo.getBlobDigest(), destinationBlob);
                    destinationBlobList.add(destinationBlob);
                } else {
                    ZimbraLog.misc.warn("Could not find blob for message " + movedItemInfo.getId() + ", revision " + movedItemInfo.getModContent());
                    itemsToMigrateInfosIter.remove(); // We do not want to change original locator if we don't find a file
                }
            }

            // Update messages in the database
            DbBlobMover.alterVolume(dbConnection, mbox, destinationLocator, itemsToMigrateInfos);

            // Update global map, now that we know that all ops have succeeded
            mAllDestinationBlobs.putAll(destinationBlobMap);

            // Delete origin blobs
            Iterator originBlobsIter = originBlobs.iterator();
            while (originBlobsIter.hasNext()) {
                MailboxBlob originMboxBlob = (MailboxBlob) originBlobsIter.next();
                try {
                    mStore.delete(originMboxBlob);
                } catch (IOException e) {
                    ZimbraLog.misc.error("Unable to delete " + originMboxBlob + ": " + e);
                }
            }
        } catch (ServiceException e) {
            // Delete destination blobs on failure.
            // As the database changes were not committed this is safe to do.
            Iterator destinationBlobListIter = destinationBlobList.iterator();
            while (destinationBlobListIter.hasNext()) {
                MailboxBlob destinationBlob = (MailboxBlob) destinationBlobListIter.next();
                try {
                    mStore.delete(destinationBlob);
                } catch (IOException ioe) {
                    ZimbraLog.misc.error("Unable to delete " + destinationBlob + ": " + ioe);
                }
            }
        } finally {
            if (maintenance != null) {
                MailboxManager.getInstance().endMaintenance(maintenance, true, true);
            }
        }
    }

}
