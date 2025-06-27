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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.common.util.Pair;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Provisioning;

import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.store.StoreManager;

import com.zimbra.cs.store.file.FileBlobStore;
import com.zimbra.cs.volume.Volume;
import com.zimbra.cs.volume.VolumeManager;

import com.zimbra.soap.admin.message.MoveBlobsRequest;
import com.zimbra.soap.admin.message.MoveBlobsResponse;

import com.zimbra.soap.admin.type.CosSelector;
import com.zimbra.soap.admin.type.CosSelector.CosBy;
import com.zimbra.soap.type.AccountSelector;
import com.zimbra.soap.type.ZmBoolean;

import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;

import com.zimbra.cs.service.admin.AdminDocumentHandler;
import com.zimbra.cs.service.admin.AdminRightCheckPoint;



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

        Integer numBlobsMovedMockup = Integer.valueOf(1);
        resp.setNumBlobsMoved(numBlobsMovedMockup);
        Long numBytesMovedMockup = Long.valueOf(2);
        resp.setNumBytesMoved(numBytesMovedMockup);
        Integer totalMailboxesMockup = Integer.valueOf(3);
        resp.setTotalMailboxes(totalMailboxesMockup);

        return zsc.jaxbToElement(resp);
    }
}
