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

import com.btactic.hsm.soap.ZetaHsmAdminConstants;

import com.btactic.hsm.soap.ZetaHsm;
import com.btactic.hsm.service.admin.MoveBlobs;

public class ZetaHsmAdminService extends AdminService implements DocumentService {

    public void registerHandlers(DocumentDispatcher dispatcher) {
        dispatcher.registerHandler(ZetaHsmAdminConstants.ZETA_HSM_REQUEST, new ZetaHsm());
        dispatcher.registerHandler(HsmConstants.MOVE_BLOBS_REQUEST, new MoveBlobs());
    }

}
