/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra OSE HSM Extension
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

    public List<MovedItemInfo> filterItemsByVolume (Mailbox mailbox, List<Integer> zimbraQueryPreFilterItemsChunk, String validOriginVolumeIdsString) throws ServiceException {
        List<MovedItemInfo> filteredItemInfos = new ArrayList<MovedItemInfo>();

        // TODO: Do one query for non-dumpster table and another one for dumpster table and add them together
        // TODO: Check if that takes less time to execute than current algorithm based on UNION query

        // non-dumpster items and dumpster items UNION query
        // Also extra data to avoid querying so much the database
        StringBuilder sql = new StringBuilder();
        sql.append("(");
            sql.append("SELECT mi.id, mi.locator, mi.mod_content, mi.blob_digest FROM ");
            sql.append(DbMailItem.getMailItemTableName(mailbox, "mi", false));
            sql.append(" WHERE ");
                sql.append(" mi.locator IN ");
                sql.append("(");
                sql.append(validOriginVolumeIdsString);
                sql.append(")");
            sql.append(" AND ");
                sql.append(" mi.id IN ");
                sql.append("(");
                sql.append(StringUtils.join(zimbraQueryPreFilterItemsChunk, ","));
                sql.append(")");
        sql.append(")");
        sql.append(" UNION ");
        sql.append("(");
            sql.append("SELECT mi.id, mi.locator, mi.mod_content, mi.blob_digest FROM ");
            sql.append(DbMailItem.getMailItemTableName(mailbox, "mi", true));
            sql.append(" WHERE ");
                sql.append(" mi.locator IN ");
                sql.append("(");
                sql.append(validOriginVolumeIdsString);
                sql.append(")");
            sql.append(" AND ");
                sql.append(" mi.id IN ");
                sql.append("(");
                sql.append(StringUtils.join(zimbraQueryPreFilterItemsChunk, ","));
                sql.append(")");
        sql.append(")");

        Connection conn = null;
        DbConnection dbConnection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            dbConnection = DbPool.getConnection(mailbox);
            conn = dbConnection.getConnection();
            stmt = conn.prepareStatement(sql.toString());
            rs = stmt.executeQuery();
            while (rs.next()) {
                int id = rs.getInt(1);
                short locator = rs.getShort(2);
                int modContent = rs.getInt(3);
                String blobDigest = rs.getString(4);
                MovedItemInfo info = new MovedItemInfo(id, locator, modContent, blobDigest);
                filteredItemInfos.add(info);
            }
        } catch (ServiceException e) {
            throw ServiceException.FAILURE("ZetaHsm: Failed to filter blobs", e);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("ZetaHsm: Failed to filter blobs", e);
        } finally {
            if (dbConnection != null) {
                dbConnection.closeQuietly(rs);
                dbConnection.closeQuietly(stmt);
            }
            DbPool.quietClose(dbConnection);
        }

        return filteredItemInfos;

    }

}
