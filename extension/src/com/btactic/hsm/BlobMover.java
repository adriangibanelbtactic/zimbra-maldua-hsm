/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra OSE HSM Extension
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

    private HashMap<String, MailboxBlob> mAllNewBlobs = null;
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

    private List<Short> getValidOriginVolumeIds(SoapProvisioning prov, int destinationVolumeId) throws ServiceException {
        List<Short> validOriginVolumeIds = new ArrayList<Short>();

        GetAllVolumesRequest request = new GetAllVolumesRequest();
        Element requestElement = JaxbUtil.jaxbToElement(request);
        Element respElem = prov.invoke(requestElement);
        GetAllVolumesResponse response = JaxbUtil.elementToJaxb(respElem);

        for (VolumeInfo volumeInfo : response.getVolumes()) {

            if (volumeInfo.getId() == destinationVolumeId) {
                break;
            }

            if (volumeInfo.getType() == Volume.TYPE_INDEX) {
                break;
            }

            if ((Volume.StoreType.getStoreTypeBy(volumeInfo.getStoreType()).equals(Volume.StoreType.INTERNAL)) && (volumeInfo.getStoreManagerClass().equals("com.zimbra.cs.store.file.FileBlobStore"))) {
                validOriginVolumeIds.add(volumeInfo.getId());
            }
        }

        return validOriginVolumeIds;
    }

    private void filterAndAddToFilteredItemIds(SoapProvisioning prov, Mailbox mbox, List<Integer> zimbraQueryPreFilterItemsChunk, List<MovedItemInfo> zimbraQueryPostFilterItemsInfos, String validOriginVolumeIdsString) throws ServiceException {
        if (!(zimbraQueryPreFilterItemsChunk.isEmpty())) {
            DbBlobFilter dbBlobFilter = new DbBlobFilter ();
            List<MovedItemInfo> filteredItemsInfos = dbBlobFilter.filterItemsByVolume(prov, mbox, zimbraQueryPreFilterItemsChunk, validOriginVolumeIdsString);
            zimbraQueryPostFilterItemsInfos.addAll(filteredItemsInfos);
        }
    }

    public void moveItems(SoapProvisioning prov, String hsmTypesString, String hsmSearchQueryString, short destinationVolumeId) throws ServiceException {
        mAllNewBlobs = new HashMap<String, MailboxBlob>();
        List<Short> validOriginVolumeIds = getValidOriginVolumeIds(prov, destinationVolumeId);

        if (validOriginVolumeIds.isEmpty()) {
            ZimbraLog.misc.info("No valid origin volume Ids for this zimbraHsmPolicy. Skipping.");
            return;
        }

        String validOriginVolumeIdsString = StringUtils.join(validOriginVolumeIds, ",");
        ZimbraLog.misc.info("DEBUG: validOriginVolumeIdsString: '" + validOriginVolumeIdsString + "'" + ".");

        List<Integer> mailboxIds = getAllMailboxIds(prov);
        for (int mboxId : mailboxIds) {
            ZimbraLog.misc.info("DEBUG: mailbox: " + mboxId + " - hsmTypesString: '" + hsmTypesString + "' - hsmSearchQueryString: '" + hsmSearchQueryString + "' - destinationVolumeId: " + destinationVolumeId + ".");

            Mailbox mbox = MailboxManager.getInstance().getMailboxById(mboxId);

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

            while (result.hasNext()) {
                zimbraQueryPreFilterCounter = zimbraQueryPreFilterCounter + 1;
                int itemId = result.getNext().getItemId();
                zimbraQueryPreFilterItemsChunk.add(itemId);
                if (zimbraQueryPreFilterCounter == zimbraQueryPreFilterChunkSize) {
                    filterAndAddToFilteredItemIds (prov, mbox, zimbraQueryPreFilterItemsChunk, zimbraQueryPostFilterItemsInfos, validOriginVolumeIdsString);
                    zimbraQueryPreFilterItemsChunk = new ArrayList<Integer>();
                    zimbraQueryPreFilterCounter = 0;
                }
                // ZimbraLog.misc.info("DEBUG: mailboxId (Pre Filter): " + mboxId + " ItemId: '" + itemId + "'" + ".");
            }
            filterAndAddToFilteredItemIds (prov, mbox, zimbraQueryPreFilterItemsChunk, zimbraQueryPostFilterItemsInfos, validOriginVolumeIdsString);
            zimbraQueryPreFilterItemsChunk = new ArrayList<Integer>();
            zimbraQueryPreFilterCounter = 0;

            IOUtil.closeQuietly(result);


            for (MovedItemInfo zimbraQueryPostFilterItemsInfo : zimbraQueryPostFilterItemsInfos) {
                ZimbraLog.misc.info("DEBUG: mailboxId (Post Filter): " + mboxId + " ItemId: '" + zimbraQueryPostFilterItemsInfo.getId() + "'" + ".");
            }

            moveItems(mbox, destinationVolumeId, zimbraQueryPostFilterItemsInfos);

        }
    }

    private void moveItems(Mailbox mbox, short destinationVolumeId, List<MovedItemInfo> itemsToMigrateInfos) throws ServiceException {
        Iterator itemsToMigrateInfosIter = itemsToMigrateInfos.iterator();
        List<MovedItemInfo> itemsInfosToMigrateChunk = new ArrayList<MovedItemInfo>();
        int movedItemInfoChunkSize = 100; // TODO: Optional parametre that you can set to speed up queries
        int movedItemInfoCounter = 0;

        while (itemsToMigrateInfosIter.hasNext()) {
            movedItemInfoCounter = movedItemInfoCounter + 1;
            MovedItemInfo info = (MovedItemInfo) itemsToMigrateInfosIter.next();
            itemsInfosToMigrateChunk.add(info);
            if (movedItemInfoCounter == movedItemInfoChunkSize) {
                moveChunkItems(mbox, destinationVolumeId, itemsInfosToMigrateChunk);
                itemsInfosToMigrateChunk = new ArrayList<MovedItemInfo>();
                movedItemInfoCounter = 0;
            }
        }
        moveChunkItems(mbox, destinationVolumeId, itemsInfosToMigrateChunk);
        itemsInfosToMigrateChunk = new ArrayList<MovedItemInfo>();
        movedItemInfoCounter = 0;
    }

    private void moveChunkItems(Mailbox mbox, short destinationVolumeId, List<MovedItemInfo> itemsToMigrateInfos) throws ServiceException {

        List<MailboxBlob> oldBlobs = new ArrayList<MailboxBlob>();
        ZimbraLog.misc.info("DEBUG: Moving " + itemsToMigrateInfos.size() + " messages.");
        MailboxBlob oldBlob = null;

        Map<String, MailboxBlob> newBlobMap = new HashMap<String, MailboxBlob>(); // Fast lookup by digest
        List<MailboxBlob> newBlobList = new ArrayList<MailboxBlob>(); // Deletion in case of error
        MailboxMaintenance maintenance = null;

        try {
            maintenance = MailboxManager.getInstance().beginMaintenance(mbox.getAccountId(), mbox.getId());

            Iterator itemsToMigrateInfosIter = itemsToMigrateInfos.iterator();
            while (itemsToMigrateInfosIter.hasNext()) {
                MovedItemInfo info = (MovedItemInfo) itemsToMigrateInfosIter.next();

                // Copy blob to new volume
                oldBlob = mStore.getMailboxBlob(mbox, info.getId(), info.getModContent(), String.valueOf(info.getLocator()));
                if (oldBlob != null) {
                    MailboxBlob newBlob = null;

                    try {
                        // If we've already copied this blob, link to the copy,
                        // rather than copying the original
                        MailboxBlob linkSource = (MailboxBlob) mAllNewBlobs.get(info.getBlobDigest());
                        if (linkSource == null) {
                            linkSource = (MailboxBlob) newBlobMap.get(info.getBlobDigest());
                            if (linkSource == null) {
                                linkSource = oldBlob;
                            }
                        }

                        // Create the link
                        newBlob = mStore.link(linkSource.getLocalBlob(), mbox, info.getId(), info.getModContent(), destinationVolumeId);
                    } catch (IOException e) {
                        throw ServiceException.FAILURE(
                            "Unable to copy " + oldBlob + " to volume " + destinationVolumeId, e);
                    }

                    oldBlobs.add(oldBlob);
                    newBlobMap.put(info.getBlobDigest(), newBlob);
                    newBlobList.add(newBlob);
                } else {
                    ZimbraLog.misc.warn("Could not find blob for message " + info.getId() + ", revision " + info.getModContent());
                }
            }

            // Update messages in the database
            DbBlobMover.alterVolume(mbox, destinationVolumeId, itemsToMigrateInfos);

            // Update global map, now that we know that all ops have succeeded
            mAllNewBlobs.putAll(newBlobMap);

            // Delete old blobs
            Iterator oldBlobsIter = oldBlobs.iterator();
            while (oldBlobsIter.hasNext()) {
                MailboxBlob oldMboxBlob = (MailboxBlob) oldBlobsIter.next();
                try {
                    mStore.delete(oldMboxBlob);
                } catch (IOException e) {
                    ZimbraLog.misc.error("Unable to delete " + oldMboxBlob + ": " + e);
                }
            }
        } catch (ServiceException e) {
            // Delete new blobs on failure.  It's safe to do this, since we know
            // the database changes were not committed.
            Iterator newBlobListIter = newBlobList.iterator();
            while (newBlobListIter.hasNext()) {
                MailboxBlob newBlob = (MailboxBlob) newBlobListIter.next();
                try {
                    mStore.delete(newBlob);
                } catch (IOException ioe) {
                    ZimbraLog.misc.error("Unable to delete " + newBlob + ": " + ioe);
                }
            }
        } finally {
            if (maintenance != null) {
                MailboxManager.getInstance().endMaintenance(maintenance, true, true);
            }
        }
    }

}
