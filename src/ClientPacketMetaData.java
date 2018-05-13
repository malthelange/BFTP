public class ClientPacketMetaData {
    boolean acknowledged;
    long timestamp;

    public ClientPacketMetaData() {
        this.acknowledged = false;
        this.timestamp = System.currentTimeMillis();
    }
}
