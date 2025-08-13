/*
 * ***** BEGIN LICENSE BLOCK *****
 * Maldua Zimbra HSM Extension
 * Copyright (C) 2025 BTACTIC, S.C.C.L.
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

package com.btactic.hsm;

import java.io.IOException;

public class ScheduleSMPolicy {

    private boolean enabled = false;
    private String error = null;
    private int startTime = 0;

    public boolean isEnabled() {
        return enabled;
    }

    public String getError() {
        return error;
    }

    public int getStartTime() {
        return startTime;
    }

    public void setSchedule(int smSchedulePolicyStartTime) throws IOException {
        this.startTime = smSchedulePolicyStartTime;
    }

    public void enable() throws IOException {
        this.enabled = true;
    }

    public void disable() throws IOException {
        this.enabled = false;
    }
}
