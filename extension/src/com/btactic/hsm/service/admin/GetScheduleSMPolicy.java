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
import com.zimbra.soap.admin.type.Name;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;

import com.zimbra.cs.service.admin.AdminDocumentHandler;
import com.zimbra.cs.service.admin.AdminRightCheckPoint;

import com.zimbra.soap.admin.message.GetScheduleSMPolicyRequest;
import com.zimbra.soap.admin.message.GetScheduleSMPolicyResponse;

public final class GetScheduleSMPolicy extends AdminDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        GetScheduleSMPolicyRequest req = JaxbUtil.elementToJaxb(request);

        Name server = req.getServer();

        checkRight(zsc, context, null, AdminRight.PR_SYSTEM_ADMIN_ONLY);

        // TODO: Proxy to the correct server if we are not in the right server

        com.btactic.hsm.ScheduleSMPolicy scheduleSMPolicy = com.btactic.hsm.ScheduleSMPolicy();
        boolean isEnabled;
        String error;
        Integer startTime;

        try {
            isEnabled = scheduleSMPolicy.isEnabled();
            error = scheduleSMPolicy.getError();
            startTime = scheduleSMPolicy.getStartTime();
        } catch (IOException e) {
            throw ServiceException.FAILURE("error while performing GetScheduleSMPolicy", e);
        }

        GetScheduleSMPolicyResponse resp = new GetScheduleSMPolicyResponse(isEnabled);
        resp.setError(error);
        resp.setSmScheduleStartTime(startTime);

        return zsc.jaxbToElement(resp);
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add(AdminRightCheckPoint.Notes.SYSTEM_ADMINS_ONLY);
    }
}
