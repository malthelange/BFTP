import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.*;

public class ServerClient {
    private InetAddress address;
    private int port;
    private int randomInteger;
    private int windowBegin;
    private int windowEnd;
    private long fileSize;
    private String filename;
    private Map<Integer,byte[]> fileBuffer;
    private long timeStamps[];
    private boolean finished;

    ServerClient(InetAddress address, int port, int randomInteger, long fileSize) {
        this.address = address;
        this.port = port;
        this.randomInteger = randomInteger;
        this.windowBegin = 0;
        this.windowEnd = ProtocolUtil.getWindowEnd(fileSize, windowBegin);
        this.fileSize = fileSize;
        this.filename = address.getHostName() + port + System.currentTimeMillis();
        this.fileBuffer = new HashMap<>();
        this.timeStamps = new long[(int) (Math.ceil(fileSize / ProtocolUtil.BLOCK_SIZE)) + 1];
        this.finished = false;
        Arrays.fill(timeStamps, -1);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServerClient that = (ServerClient) o;
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
                while (!finished && timeStamps[windowBegin] >= 0) {
                    try {
                        writeDataAndMoveWindow(fileBuffer.get(windowBegin));
                        fileBuffer.remove(windowBegin);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                // TODO: might want to only put when the timestamp is empty.
                fileBuffer.put(packetIndex, fileData);
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
        if(windowBegin != fileSize / ProtocolUtil.BLOCK_SIZE) {
            outputStream.write(fileData);
        } else {
            int remainder = Math.toIntExact(fileSize % ProtocolUtil.BLOCK_SIZE);
            outputStream.write(fileData, 0, remainder);
        }
        outputStream.close();
        windowBegin++;
        windowEnd = ProtocolUtil.getWindowEnd(fileSize, windowBegin);
        if (windowBegin > windowEnd) {
            finished = true;
        }
    }
}