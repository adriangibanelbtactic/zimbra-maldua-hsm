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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.DbConnection;

import com.zimbra.cs.mailbox.Mailbox;

import org.apache.commons.lang.StringUtils;

public class DbBlobMover {

    private static void alterMailItemVolume(DbConnection dbConnection, Mailbox mbox, short destinationVolumeId, List<MovedItemInfo> itemsToMigrateInfos, boolean dumpster) throws ServiceException {

        List<Integer> itemsToMigrateInfosIds = new ArrayList<Integer>();
        for (MovedItemInfo itemsToMigrateInfo : itemsToMigrateInfos) {
            itemsToMigrateInfosIds.add(itemsToMigrateInfo.getId());
        }

        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ");
        sql.append(DbMailItem.getMailItemTableName(mbox, dumpster));
        sql.append(" SET locator = ");
        sql.append(destinationVolumeId);
        sql.append(" WHERE ");
        sql.append(" id IN ");
        sql.append("(");
        sql.append(StringUtils.join(itemsToMigrateInfosIds, ","));
        sql.append(")");

        ZimbraLog.misc.info("DEBUG: MoveQuery: '" + sql.toString() + "'" + ".");

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = dbConnection.getConnection();
            stmt = conn.prepareStatement(sql.toString());
            stmt.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("ZetaHsm: Failed to update blobs in DB", e);
        } finally {
            if (dbConnection != null) {
                dbConnection.closeQuietly(stmt);
            }
        }

    }

    private static void alterRevisionVolume(DbConnection dbConnection, Mailbox mbox, short destinationVolumeId, List<MovedItemInfo> itemsToMigrateInfos, boolean dumpster) throws ServiceException {

        List<Integer> itemsToMigrateInfosIds = new ArrayList<Integer>();
        for (MovedItemInfo itemsToMigrateInfo : itemsToMigrateInfos) {
            itemsToMigrateInfosIds.add(itemsToMigrateInfo.getId());
        }

        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ");
        sql.append(DbMailItem.getRevisionTableName(mbox, dumpster));
        sql.append(" SET locator = ");
        sql.append(destinationVolumeId);
        sql.append(" WHERE ");
        sql.append(" item_id IN ");
        sql.append("(");
        sql.append(StringUtils.join(itemsToMigrateInfosIds, ","));
        sql.append(")");

        ZimbraLog.misc.info("DEBUG: MoveQuery: '" + sql.toString() + "'" + ".");

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = dbConnection.getConnection();
            stmt = conn.prepareStatement(sql.toString());
            stmt.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("ZetaHsm: Failed to update blobs in DB", e);
        } finally {
            if (dbConnection != null) {
                dbConnection.closeQuietly(stmt);
            }
        }

    }

    public static void alterVolume(DbConnection dbConnection, Mailbox mbox, short destinationVolumeId, List<MovedItemInfo> itemsToMigrateInfos) throws ServiceException {

        alterMailItemVolume(dbConnection, mbox, destinationVolumeId, itemsToMigrateInfos, false); // Default table
        alterMailItemVolume(dbConnection, mbox, destinationVolumeId, itemsToMigrateInfos, true); // Dumpster table
        alterRevisionVolume(dbConnection, mbox, destinationVolumeId, itemsToMigrateInfos, false); // Default table
        alterRevisionVolume(dbConnection, mbox, destinationVolumeId, itemsToMigrateInfos, true); // Dumpster table

    }

}
