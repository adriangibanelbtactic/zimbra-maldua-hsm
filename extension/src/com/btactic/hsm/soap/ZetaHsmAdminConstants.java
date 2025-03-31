/*
 * ***** BEGIN LICENSE BLOCK *****
 * Maldua Zimbra HSM Extension
 * Copyright (C) 2023 BTACTIC, S.C.C.L.
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2021, 2022, 2023 Synacor, Inc.
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

import org.dom4j.Namespace;
import org.dom4j.QName;

public final class ZetaHsmAdminConstants {

    public static final String ADMIN_SERVICE_URI = "/service/admin/soap/";

    public static final String NAMESPACE_STR = "urn:zimbraAdmin";
    public static final Namespace NAMESPACE = Namespace.get(NAMESPACE_STR);

    public static final String E_ZETA_HSM_REQUEST = "ZetaHsmRequest";
    public static final QName ZETA_HSM_REQUEST = QName.get(E_ZETA_HSM_REQUEST, NAMESPACE);

    public static final String E_ZETA_HSM_RESPONSE = "ZetaHsmResponse";
    public static final QName ZETA_HSM_RESPONSE = QName.get(E_ZETA_HSM_RESPONSE, NAMESPACE);

}
