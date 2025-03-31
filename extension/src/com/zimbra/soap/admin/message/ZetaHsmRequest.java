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

package com.zimbra.soap.admin.message;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.MoreObjects;
import com.zimbra.common.service.ServiceException;
import com.zimbra.soap.admin.message.AutoProvTaskControlRequest.Action;
import com.zimbra.soap.admin.type.IntIdAttr;

import com.zimbra.common.soap.AdminConstants;
import com.btactic.hsm.soap.ZetaHsmAdminConstants;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Perform HSM.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=ZetaHsmAdminConstants.E_ZETA_HSM_REQUEST)
public class ZetaHsmRequest {

    @XmlEnum
    public static enum HsmAction {
        start,
        status,
        stop;

        public static Action fromString(String action) throws ServiceException {
            try {
                return Action.valueOf(action);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown action: " + action, e);
            }
        }
    }

    /**
     * @zm-api-field-description Action to perform - one of <b>start|status|stop</b>
     */
    @XmlAttribute(name=AdminConstants.E_ACTION, required=true)
    private final HsmAction action;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ZetaHsmRequest() {
        this((HsmAction)null);
    }

    public ZetaHsmRequest(HsmAction action) {
        this.action = action;
    }

    public HsmAction getAction() {
        return action;
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("action", action);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
