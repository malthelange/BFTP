import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Random;

public class Client implements Runnable {
    int port;
    private String fileName;
    private File file;
    private Random random;
    private int randomInt;
    boolean done;
    private int windowBegin;
    private long fileSize;
    private int windowEnd;
    private DataInputStream dataInputStream;
    private byte[] fileData;
    private DatagramSocket datagramSocket;

    public static void main(String[] args) {
        new Thread(new Client(args[0])).start();
    }

    public Client(String filename) {
        this.port = 4451;
        this.fileName = filename;
        this.file = new File(fileName);
        this.random = new Random();
        this.randomInt = random.nextInt();
        this.done = false;
        this.windowBegin = 0;
        this.fileSize = file.length();
        this.windowEnd = ProtocolUtil.getWindowEnd(fileSize, windowBegin);
        this.dataInputStream = null;
    }

    @Override
    public void run() {
        if (!readWindowFromFile()) return;
        datagramSocket = null;
        try {
            datagramSocket = new DatagramSocket();

        } catch (SocketException e) {
            System.out.println("Couldn't initialize socket");
            e.printStackTrace();
            return;
        }
        for (int packetIndex = windowBegin; packetIndex <= windowEnd; packetIndex++) {
            byte[] packetData = getPacketAtIndex(randomInt, packetIndex, fileSize, fileData);
            DatagramPacket datagramPacket;
            try {
                datagramPacket = new DatagramPacket(
                        packetData,
                        packetData.length,
                        InetAddress.getLocalHost(),
                        port);
                datagramSocket.send(datagramPacket);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
        while (!done) {
        }
    }

    private boolean readWindowFromFile() {
        try {
            dataInputStream = new DataInputStream(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        long readSize = Math.min(fileSize, ProtocolUtil.BLOCK_SIZE * (windowEnd + 1));
        fileData = new byte[Math.toIntExact(readSize)];
        int read = 0;
        try {
            read = dataInputStream.read(fileData,
                    windowBegin * ProtocolUtil.BLOCK_SIZE,
                    fileData.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (read != readSize) {
            System.out.println("Couldn't read file data");
            return false;
        }
        return true;
    }

    private synchronized static void checkTimestamps() {
    }

    public synchronized static void updateWindow() {
    }

    private byte[] getPacketAtIndex(
            int randomInt,
            int packetIndex,
            long fileSize,
            byte[] fileData) {
        int packetDataSize = Math.min(
                fileData.length - ProtocolUtil.BLOCK_SIZE * packetIndex, // the remaining data
                ProtocolUtil.BLOCK_SIZE);
        ByteBuffer resultPacket = ByteBuffer.allocate(4 + 8 + 4 + packetDataSize);
        resultPacket.putInt(randomInt);
        resultPacket.putLong(fileSize);
        resultPacket.putInt(packetIndex);
        resultPacket.put(fileData, packetIndex * ProtocolUtil.BLOCK_SIZE, packetDataSize);
        return resultPacket.array();
    }

    byte[] getPacket(int randomInteger, long fileSize, int packetNumber, byte[] packetData) {
        ByteBuffer resultBuffer = ByteBuffer.allocate(4 + 8 + 4 + packetData.length);
        resultBuffer.putInt(randomInteger);
        resultBuffer.putLong(fileSize);
        resultBuffer.putInt(packetNumber);
        resultBuffer.put(packetData);
        return resultBuffer.array();
    }

}
