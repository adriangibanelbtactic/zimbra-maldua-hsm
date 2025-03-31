/*
 * ***** BEGIN LICENSE BLOCK *****
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
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.btactic.hsm;

import com.zimbra.common.service.ServiceException;

import com.zimbra.common.soap.Element;

import com.zimbra.common.util.ZimbraLog;

import com.zimbra.cs.account.soap.SoapProvisioning;

import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.DbConnection;

import com.zimbra.cs.mailbox.Mailbox;

import com.zimbra.cs.util.IOUtil;

import com.zimbra.soap.JaxbUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

public class DbBlobFilter {

    public void addMailItemItemsByLocation (List<MovedItemInfo> filteredItemInfos, DbConnection dbConnection, Mailbox mailbox, List<Integer> zimbraQueryPreFilterItemsChunk, String validOriginLocatorsString, boolean dumpster) throws ServiceException {

        // Items matching our origin locators
        // Also extra data to avoid querying so much the database
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT mi.id, mi.locator, mi.mod_content, mi.blob_digest FROM ");
        sql.append(DbMailItem.getMailItemTableName(mailbox, "mi", dumpster));
        sql.append(" WHERE ");
            sql.append(" mi.locator IN ");
            sql.append("(");
            sql.append(validOriginLocatorsString);
            sql.append(")");
        sql.append(" AND ");
            sql.append(" mi.id IN ");
            sql.append("(");
            sql.append(StringUtils.join(zimbraQueryPreFilterItemsChunk, ","));
            sql.append(")");

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = dbConnection.getConnection();
            stmt = conn.prepareStatement(sql.toString());
            rs = stmt.executeQuery();
            while (rs.next()) {
                int id = rs.getInt(1);
                short locator = rs.getShort(2);
                int modContent = rs.getInt(3);
                String blobDigest = rs.getString(4);
                MovedItemInfo movedItemInfo = new MovedItemInfo(id, locator, modContent, blobDigest);
                filteredItemInfos.add(movedItemInfo);
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("ZetaHsm: Failed to filter blobs", e);
        } finally {
            if (dbConnection != null) {
                dbConnection.closeQuietly(rs);
                dbConnection.closeQuietly(stmt);
            }
        }

    }

    public void addRevisionItemsByLocation (List<MovedItemInfo> filteredItemInfos, DbConnection dbConnection, Mailbox mailbox, List<Integer> zimbraQueryPreFilterItemsChunk, String validOriginLocatorsString, boolean dumpster) throws ServiceException {

        // Revisions matching our origin locators
        // Also extra data to avoid querying so much the database
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT mi.item_id, mi.locator, mi.mod_content, mi.blob_digest FROM ");
        sql.append(DbMailItem.getRevisionTableName(mailbox, "mi", dumpster));
        sql.append(" WHERE ");
            sql.append(" mi.locator IN ");
            sql.append("(");
            sql.append(validOriginLocatorsString);
            sql.append(")");
        sql.append(" AND ");
            sql.append(" mi.item_id IN ");
            sql.append("(");
            sql.append(StringUtils.join(zimbraQueryPreFilterItemsChunk, ","));
            sql.append(")");

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = dbConnection.getConnection();
            stmt = conn.prepareStatement(sql.toString());
            rs = stmt.executeQuery();
            while (rs.next()) {
                int id = rs.getInt(1);
                short locator = rs.getShort(2);
                int modContent = rs.getInt(3);
                String blobDigest = rs.getString(4);
                MovedItemInfo movedItemInfo = new MovedItemInfo(id, locator, modContent, blobDigest);
                filteredItemInfos.add(movedItemInfo);
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("ZetaHsm: Failed to filter blobs", e);
        } finally {
            if (dbConnection != null) {
                dbConnection.closeQuietly(rs);
                dbConnection.closeQuietly(stmt);
            }
        }

    }

    public List<MovedItemInfo> filterItemsByLocation (DbConnection dbConnection, Mailbox mailbox, List<Integer> zimbraQueryPreFilterItemsChunk, String validOriginLocatorsString) throws ServiceException {
        List<MovedItemInfo> filteredItemInfos = new ArrayList<MovedItemInfo>();

        addMailItemItemsByLocation(filteredItemInfos, dbConnection, mailbox, zimbraQueryPreFilterItemsChunk, validOriginLocatorsString, false); // Default table
        addMailItemItemsByLocation(filteredItemInfos, dbConnection, mailbox, zimbraQueryPreFilterItemsChunk, validOriginLocatorsString, true); // Dumpster table
        addRevisionItemsByLocation(filteredItemInfos, dbConnection, mailbox, zimbraQueryPreFilterItemsChunk, validOriginLocatorsString, false); // Default table
        addRevisionItemsByLocation(filteredItemInfos, dbConnection, mailbox, zimbraQueryPreFilterItemsChunk, validOriginLocatorsString, true); // Dumpster table

        return filteredItemInfos;

    }

}
