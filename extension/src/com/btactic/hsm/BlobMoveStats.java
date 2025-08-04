public class BlobMoveStats {
    private int numBlobsMoved;
    private long numBytesMoved;
    private int numMailboxesMoved;

    public BlobMoveStats() {
        this.numBlobsMoved = 0;
        this.numBytesMoved = 0L;
        this.numMailboxesMoved = 0;
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
}
