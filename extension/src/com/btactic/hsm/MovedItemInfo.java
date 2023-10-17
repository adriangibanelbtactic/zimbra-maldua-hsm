/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra OSE HSM Extension
 * Copyright (C) 2024 BTACTIC, S.C.C.L.
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

public class MovedItemInfo {
    
    private int id; 
    private short locator;
    private int modContent;
    private String blobDigest;
    
    MovedItemInfo(int id, short locator, int modContent, String blobDigest) {
        this.id = id;
        this.locator = locator;
        this.modContent = modContent;
        this.blobDigest = blobDigest;
    }
    
    public int getId() {
        return id;
    }
    
    public short getLocator() {
        return locator;
    }
    
    public int getModContent() {
        return modContent;
    }
    
    public String getBlobDigest() {
        return blobDigest;
    }
}
