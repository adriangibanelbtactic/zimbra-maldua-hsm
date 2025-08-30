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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.common.util.Pair;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.store.StoreManager;

import com.zimbra.cs.store.file.FileBlobStore;
import com.zimbra.cs.volume.Volume;
import com.zimbra.cs.volume.VolumeManager;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;

import com.zimbra.cs.service.admin.AdminDocumentHandler;
import com.zimbra.cs.service.admin.AdminRightCheckPoint;

import com.zimbra.soap.admin.message.GetHsmStatusResponse;

import com.btactic.hsm.BlobMoveStats;

public final class GetHsmStatus extends AdminDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        checkRight(zsc, context, null, AdminRight.PR_SYSTEM_ADMIN_ONLY);
        StoreManager sm = StoreManager.getInstance();
        if (!(sm instanceof FileBlobStore)) {
            throw ServiceException.INVALID_REQUEST(sm.getClass().getName()
                    + " is not supported", null);
        }

        com.btactic.hsm.ZetaHsm zetahsm = com.btactic.hsm.ZetaHsm.getInstance();

        boolean isRunning = zetahsm.isRunning();
        GetHsmStatusResponse resp = new GetHsmStatusResponse(isRunning);

        Long startDate = zetahsm.getStartDate();
        resp.setStartDate(startDate);
        if (!(isRunning)) {
            Long endDate = zetahsm.getEndDate();
            resp.setEndDate(endDate);
        }

        boolean wasAborted = zetahsm.wasAborted();
        resp.setWasAborted(wasAborted);

        boolean isAborting = zetahsm.isAborting();
        resp.setAborting(isAborting);

        String error = zetahsm.getError();
        resp.setError(error);

        int numBlobsMoved = zetahsm.getNumBlobsMoved();
        if (!(numBlobsMoved == -1)) {
            resp.setNumBlobsMoved(numBlobsMoved);
        } else {
            resp.setNumBlobsMoved(0);
        }

        long numBytesMoved = zetahsm.getNumBytesMoved();
        if (!(numBytesMoved == -1L)) {
            resp.setNumBytesMoved(numBytesMoved);
        } else {
            resp.setNumBytesMoved(0L);
        }

        int numMailboxesMoved = zetahsm.getNumMailboxesMoved();
        if (!(numMailboxesMoved == -1)) {
            resp.setNumMailboxes(numMailboxesMoved);
        } else {
            resp.setNumMailboxes(0);
        }

        int totalMailboxes = zetahsm.getTotalMailboxes();
        if (!(totalMailboxes == -1)) {
            resp.setTotalMailboxes(totalMailboxes);
        } else {
            resp.setTotalMailboxes(0);
        }

        short destinationVolumeId = zetahsm.getDestinationVolumeId();
        if (!(destinationVolumeId == -1)) {
            resp.setDestVolumeId(destinationVolumeId);
        }

        String query = zetahsm.getQuery();
        if (!(query == null)) {
            resp.setQuery(query);
        } else {
            resp.setQuery("");
        }

        return zsc.jaxbToElement(resp);
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add(AdminRightCheckPoint.Notes.SYSTEM_ADMINS_ONLY);
    }
}
