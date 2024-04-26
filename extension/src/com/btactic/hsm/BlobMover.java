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
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.OperationContext;

import com.zimbra.cs.util.IOUtil;

import com.zimbra.cs.volume.Volume;

import com.zimbra.soap.admin.message.GetAllMailboxesRequest;
import com.zimbra.soap.admin.message.GetAllMailboxesResponse;
import com.zimbra.soap.admin.message.GetAllVolumesRequest;
import com.zimbra.soap.admin.message.GetAllVolumesResponse;

import com.zimbra.soap.admin.type.MailboxInfo;
import com.zimbra.soap.admin.type.VolumeInfo;

import com.zimbra.soap.JaxbUtil;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

public class BlobMover {

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
        DbConnection conn = null;
        try {

            List<Short> validOriginVolumeIds = getValidOriginVolumeIds(prov, destinationVolumeId);

            if (validOriginVolumeIds.isEmpty()) {
                ZimbraLog.misc.info("No valid origin volume Ids for this zimbraHsmPolicy. Skipping.");
                return;
            }

            String validOriginVolumeIdsString = StringUtils.join(validOriginVolumeIds, ",");
            ZimbraLog.misc.info("DEBUG: validOriginVolumeIdsString: '" + validOriginVolumeIdsString + "'" + ".");

            conn = DbPool.getConnection();

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

                moveItems(conn, mbox, destinationVolumeId, zimbraQueryPostFilterItemsInfos);

            }
        } finally {
            DbPool.quietClose(conn);
        }
    }

    private void moveItems(DbConnection conn, Mailbox mbox, short destinationVolumeId, List<MovedItemInfo> itemsToMigrateInfos) throws ServiceException {
        List movedBlobs = new ArrayList();
    }

}
