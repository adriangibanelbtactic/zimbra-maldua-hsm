/*
 * ***** BEGIN LICENSE BLOCK *****
 * Maldua Zimbra HSM Extension
 * Copyright (C) 2025 BTACTIC, S.C.C.L.
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.btactic.hsm.service.admin;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;

import com.zimbra.common.util.ZimbraLog;

import com.zimbra.cs.account.soap.SoapProvisioning;

import com.zimbra.cs.volume.Volume;

import com.zimbra.soap.admin.message.GetAllVolumesRequest;
import com.zimbra.soap.admin.message.GetAllVolumesResponse;
import com.zimbra.soap.admin.message.MoveBlobsRequest;
import com.zimbra.soap.admin.message.MoveBlobsResponse;

import com.zimbra.soap.admin.type.VolumeInfo;

import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;

import com.zimbra.cs.service.admin.AdminDocumentHandler;

import com.btactic.hsm.BlobMover;
import com.btactic.hsm.BlobMoveStats;
import com.btactic.hsm.ZetaHsmLog;
import com.btactic.hsm.storage.VolumeUtil;


public class MoveBlobs extends AdminDocumentHandler {

    private void printMoveBlobsRequestDetails (String types, String query, String sourceVolumeIdsString, Short destVolumeId, Long maxBytes) {
        ZetaHsmLog.info("                Types: '" + types + "'");
        ZetaHsmLog.info("                Query: '" + query + "'");
        ZetaHsmLog.info("sourceVolumeIdsString: '" + sourceVolumeIdsString + "'");
        ZetaHsmLog.info("         destVolumeId: '" + String.valueOf(destVolumeId) + "'");
        ZetaHsmLog.info("             maxBytes: '" + String.valueOf(maxBytes) + "'");
    }

    private void printBlobMoveStatsDetails (BlobMoveStats blobMoveStats) {
        ZetaHsmLog.info("    NumberOfBlobsMoved: '" + blobMoveStats.getNumBlobsMoved() + "'");
        ZetaHsmLog.info("    NumberOfBytesMoved: '" + blobMoveStats.getNumBytesMoved() + "'");
        ZetaHsmLog.info("NumberOfMailboxesMoved: '" + blobMoveStats.getNumMailboxesMoved() + "'");
    }

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        String defaultMoveBlobsQuery = "is:anywhere";

        ZimbraSoapContext zsc = getZimbraSoapContext(context);

        SoapProvisioning prov = SoapProvisioning.getAdminInstance();
        prov.soapZimbraAdminAuthenticate();

        MoveBlobsRequest req = JaxbUtil.elementToJaxb(request);
        MoveBlobsResponse resp = new MoveBlobsResponse();

        String types = req.getTypes();
        String sourceVolumeIdsString = req.getSourceVolumeIds();
        Short destVolumeId = req.getDestVolumeId();
        Long maxBytes = req.getMaxBytes();
        String query = req.getQuery();

        // Basic checks
        if (types == null) {
            throw ServiceException.INVALID_REQUEST("must specify types", null);
        }
        if (sourceVolumeIdsString == null) {
            throw ServiceException.INVALID_REQUEST("must specify sourceVolumeIds", null);
        }
        if (destVolumeId == null) {
            throw ServiceException.INVALID_REQUEST("must specify destVolumeId", null);
        }

        // Set default values if needed
        if (query == null) {
            query = defaultMoveBlobsQuery;
        }
        if ("all".equals(types)) {
            types="message,document,task,appointment,contact"; // TODO: Do not hardcode this and get it from somewhere else.
        }
        if (maxBytes == null) {
            maxBytes = 0L;
        }

        // ADVANCED CHECKS
        // types: No need to check if they are valid types. If they don't exist the search will not give results for them
        // maxbytes: No need to check. If maxbytes is not a number execution fails long before.
        // query: No need to check. Either no results (for being empty) or an error for its syntax not being correct

        // destVolumeId
        if (!(VolumeUtil.isValidLocator(prov, destVolumeId))) {
            throw ServiceException.INVALID_REQUEST("destVolumeId: '" + destVolumeId + "' is not a valid destination Volume ID", null);
        }

        // sourceVolumeIds
        String[] sourceVolumeIdsStringArray = sourceVolumeIdsString.split(",");
        ArrayList<String> sourceVolumeIds = new ArrayList<>(Arrays.asList(sourceVolumeIdsStringArray));
        List<Short> validOriginLocators = VolumeUtil.getValidOriginLocators(prov, destVolumeId);
        for (String sourceVolumeId : sourceVolumeIds) {
            try {
                Short sourceVolumeIdShort = Short.parseShort(sourceVolumeId);
                if (!validOriginLocators.contains(sourceVolumeIdShort)) {
                    throw ServiceException.INVALID_REQUEST("sourceVolumeId: '" + sourceVolumeId + "' is not a valid source Volume ID", null);
                }
            } catch (NumberFormatException e) {
                throw ServiceException.INVALID_REQUEST("Invalid sourceVolumeId format: '" + sourceVolumeId + "' is not a number", e);
            }
        }

        ZetaHsmLog.info("MoveBlobsRequest has started.");
        printMoveBlobsRequestDetails (types, query, sourceVolumeIdsString, destVolumeId, maxBytes);

        BlobMover blobMover = new BlobMover();
        BlobMoveStats blobMoveStats = blobMover.moveItems(prov, types, query, destVolumeId, maxBytes, sourceVolumeIdsString);

        ZetaHsmLog.info("MoveBlobsRequest has ended.");
        printMoveBlobsRequestDetails (types, query, sourceVolumeIdsString, destVolumeId, maxBytes);

        if (blobMoveStats == null) {
            blobMoveStats = new BlobMoveStats(); // Set all values to 0.
        }

        ZetaHsmLog.info("MoveBlobsRequest stats summary:");
        printBlobMoveStatsDetails (blobMoveStats);

        resp.setNumBlobsMoved(blobMoveStats.getNumBlobsMoved());
        resp.setNumBytesMoved(blobMoveStats.getNumBytesMoved());
        resp.setTotalMailboxes(blobMoveStats.getNumMailboxesMoved());

        return zsc.jaxbToElement(resp);
    }
}
