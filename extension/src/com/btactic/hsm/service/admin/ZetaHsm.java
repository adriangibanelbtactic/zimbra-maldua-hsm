/*
 * ***** BEGIN LICENSE BLOCK *****
 * Maldua Zimbra HSM Extension
 * Copyright (C) 2023 BTACTIC, S.C.C.L.
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

import com.zimbra.soap.admin.message.ZetaHsmRequest;
import com.zimbra.soap.admin.message.ZetaHsmResponse;

public final class ZetaHsm extends AdminDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        checkRight(zsc, context, null, AdminRight.PR_SYSTEM_ADMIN_ONLY);
        StoreManager sm = StoreManager.getInstance();
        if (!(sm instanceof FileBlobStore)) {
            throw ServiceException.INVALID_REQUEST(sm.getClass().getName()
                    + " is not supported", null);
        }
        // Workaround in order to be able to use elementToJaxb with non standard Zimbra classes
        // Make sure your custom Request class is inside the com.zimbra.soap.admin.message package
        ZetaHsmRequest req = JaxbUtil.elementToJaxb(request, ZetaHsmRequest.class);
        com.btactic.hsm.ZetaHsm zetahsm = com.btactic.hsm.ZetaHsm.getInstance();
        ZetaHsmResponse resp = new ZetaHsmResponse();

        if (req.getAction() == ZetaHsmRequest.HsmAction.start) {
            // Optional initial reset
        }

        if (req.getAction() == ZetaHsmRequest.HsmAction.start) {
                try {
                    zetahsm.process();
                } catch (IOException e) {
                    throw ServiceException.FAILURE("error while performing ZetaHsm", e);
                }
        } else if (req.getAction() == ZetaHsmRequest.HsmAction.stop) {
            zetahsm.stopProcessing();
        }

        // return the stats for all actions.
        boolean isRunning = zetahsm.isRunning();
        if (isRunning) {
            resp.setStatus(ZetaHsmResponse.HsmStatus.running);
        } else {
            resp.setStatus(ZetaHsmResponse.HsmStatus.stopped);
        }

        // Setting:
        //  removePrefixes to true
        //  useContextMarshaller to false
        // and passing a class inside the com.zimbra.soap.admin.message package
        // (classes that you can make yourself in the Extension)
        // let's you use this JaxbUtil.jaxbToElement method to reply Soap queries from the
        // zimbraAdmin endpoint quite nicely.
        return JaxbUtil.jaxbToElement(resp, XMLElement.mFactory, true, false);
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add(AdminRightCheckPoint.Notes.SYSTEM_ADMINS_ONLY);
    }
}
