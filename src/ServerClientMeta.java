import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

public class ServerClientMeta {
    private InetAddress address;
    private int port;
    private int randomInteger;
    private int windowBegin;
    private int windowEnd;
    private long fileSize;
    private String filename;
    private byte[] fileBuffer;
    private long timeStamps[];
    private boolean finished;

    ServerClientMeta(InetAddress address, int port, int randomInteger, long fileSize) {
        this.address = address;
        this.port = port;
        this.randomInteger = randomInteger;
        this.windowBegin = 0;
        this.windowEnd = ProtocolUtil.getWindowEnd(fileSize);
        this.fileSize = fileSize;
        this.filename = address.getHostName() + port + System.currentTimeMillis();
        this.fileBuffer = new byte[ProtocolUtil.BLOCK_SIZE * (windowEnd + 1)];
        this.timeStamps = new long[Math.toIntExact(fileSize / ProtocolUtil.BLOCK_SIZE)];
        this.finished = false;
        Arrays.fill(timeStamps, -1);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServerClientMeta that = (ServerClientMeta) o;
        return port == that.port &&
                randomInteger == that.randomInteger &&
                Objects.equals(address, that.address);
    }

    public int hashCode() {

        return Objects.hash(address, port, randomInteger);
    }

    void handlePacket(int packetIndex, byte[] fileData) {
        if (packetIndex >= windowBegin && packetIndex <= windowEnd && !finished) {
            if (packetIndex == windowBegin) {
                try {
                    writeDataAndMoveWindow(fileData);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                while (timeStamps[windowBegin] >= 0 && !finished) {
                    try {
                        System.arraycopy(
                                fileBuffer,
                                windowBegin * ProtocolUtil.BLOCK_SIZE,
                                fileData,
                                0,
                                fileData.length);
                        writeDataAndMoveWindow(fileData);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                System.arraycopy(
                        fileData,
                        0,
                        fileBuffer,
                        ProtocolUtil.BLOCK_SIZE * packetIndex,
                        fileData.length);
                timeStamps[packetIndex] = System.currentTimeMillis();
            }
            boolean success;
            do {
                success = sendAcknowledge(packetIndex);
            } while (!success);
        }
    }

    private boolean sendAcknowledge(int packetIndex) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putInt(randomInteger);
        buffer.putInt(packetIndex);
        byte[] packetData = buffer.array();
        DatagramPacket packet = new DatagramPacket(
                packetData,
                packetData.length,
                address,
                port);
        try {
            DatagramSocket socket = new DatagramSocket();
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void writeDataAndMoveWindow(byte[] fileData) throws IOException {
        FileOutputStream outputStream = new FileOutputStream(filename, true);
        outputStream.write(fileData);
        outputStream.close();
        windowBegin++;
        // TODO Der er noget her der ikke virker
        windowEnd = Math.min(windowEnd + 1, ProtocolUtil.getWindowEnd(fileSize));
        if (windowBegin > windowEnd) {
            finished = true;
        }
    }
}