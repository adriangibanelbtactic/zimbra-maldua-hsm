/*
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
 * If not, see <http://www.gnu.org/licenses/>.
 */
package com.btactic.hsm;

import com.zimbra.common.soap.HsmConstants;
import com.zimbra.soap.DocumentDispatcher;
import com.zimbra.soap.DocumentService;
import com.zimbra.cs.service.admin.AdminService;

import com.btactic.hsm.service.admin.GetHsmStatus;
import com.btactic.hsm.service.admin.Hsm;
import com.btactic.hsm.service.admin.MoveBlobs;

public class ZetaHsmAdminService extends AdminService implements DocumentService {

    public void registerHandlers(DocumentDispatcher dispatcher) {
        dispatcher.registerHandler(HsmConstants.GET_HSM_STATUS_REQUEST, new GetHsmStatus());
        dispatcher.registerHandler(HsmConstants.HSM_REQUEST, new Hsm());
        dispatcher.registerHandler(HsmConstants.MOVE_BLOBS_REQUEST, new MoveBlobs());
    }

}
