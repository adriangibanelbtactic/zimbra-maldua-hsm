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

package com.btactic.hsm.soap;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;

import com.zimbra.cs.account.Provisioning;

import com.zimbra.soap.admin.message.MoveBlobsRequest;
import com.zimbra.soap.admin.message.MoveBlobsResponse;

import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;

import com.zimbra.cs.service.admin.AdminDocumentHandler;



public class MoveBlobs extends AdminDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        String defaultMoveBlobsQuery = "is:anywhere";

        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        MoveBlobsRequest req = JaxbUtil.elementToJaxb(request);
        MoveBlobsResponse resp = new MoveBlobsResponse();

        String types = req.getTypes();
        String sourceVolumeIds = req.getSourceVolumeIds();
        Short destVolumeId = req.getDestVolumeId();
        Long maxBytes = req.getMaxBytes();
        String query = req.getQuery();

        if (types == null) {
            throw ServiceException.INVALID_REQUEST("must specify types", null);
        }
        if (sourceVolumeIds == null) {
            throw ServiceException.INVALID_REQUEST("must specify sourceVolumeIds", null);
        }
        if (destVolumeId == null) {
            throw ServiceException.INVALID_REQUEST("must specify destVolumeId", null);
        }

        if (query == null) {
            query = defaultMoveBlobsQuery;
        }

        // TODO: Manage loops based on maxBytes being null (no maximum value) or not (with maximum value)
        // TODO: Move some blobs

        boolean moveNextBlob = true;
        Long currentMaximumBytes = Long.valueOf(0);
        Long blobBytes;
        blobBytes = Long.valueOf(0);

        while (moveNextBlob) {
            if (!(maxBytes == null)) {
                // TODO: Get blob size and save it in blobBytes (if any)
                Long expectedMaximumBytes = currentMaximumBytes + blobBytes;
                if (expectedMaximumBytes > maxBytes) {
                    moveNextBlob = false;
                } else {
                    currentMaximumBytes = currentMaximumBytes + blobBytes;
                }
            }
            // TODO: Move the blob
        }

        Integer numBlobsMovedMockup = Integer.valueOf(1);
        resp.setNumBlobsMoved(numBlobsMovedMockup);
        Long numBytesMovedMockup = Long.valueOf(2);
        resp.setNumBytesMoved(numBytesMovedMockup);
        Integer totalMailboxesMockup = Integer.valueOf(3);
        resp.setTotalMailboxes(totalMailboxesMockup);

        return zsc.jaxbToElement(resp);
    }
}
