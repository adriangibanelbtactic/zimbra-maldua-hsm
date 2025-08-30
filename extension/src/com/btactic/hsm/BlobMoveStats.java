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

public class BlobMoveStats {
    private int numBlobsMoved;
    private long numBytesMoved;
    private int numMailboxesMoved;
    private int totalMailboxes;
    private short destinationVolumeId;

    public BlobMoveStats() {
        this.numBlobsMoved = 0;
        this.numBytesMoved = 0L;
        this.numMailboxesMoved = 0;
        this.totalMailboxes = 0;
        this.destinationVolumeId = -1;
    }

    public void addBlobs(int count) {
        this.numBlobsMoved += count;
    }

    public void incrementBlobs() {
        this.numBlobsMoved++;
    }

    public void addBytes(long bytes) {
        this.numBytesMoved += bytes;
    }

    public void incrementMailboxes() {
        this.numMailboxesMoved++;
    }

    public int getNumBlobsMoved() {
        return numBlobsMoved;
    }

    public long getNumBytesMoved() {
        return numBytesMoved;
    }

    public int getNumMailboxesMoved() {
        return numMailboxesMoved;
    }

    public int getTotalMailboxes() {
        return totalMailboxes;
    }

    public void setTotalMailboxes(int totalMailboxes) {
        this.totalMailboxes = totalMailboxes;
    }

    public short getDestinationVolumeId() {
        return destinationVolumeId;
    }

    public void setDestinationVolumeId(short destinationVolumeId) {
        this.destinationVolumeId = destinationVolumeId;
    }
}
