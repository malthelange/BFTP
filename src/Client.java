import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

public class Client implements Runnable {
    int port;
    private String fileName;
    private File file;
    private Random random;
    int randomInteger;
    boolean done;
    int windowBegin;
    private long fileSize;
    private int windowEnd;
    private DataInputStream dataInputStream;
    private byte[] fileData;
    private DatagramSocket datagramSocket;
    boolean[] received = new boolean[Math.toIntExact(fileSize / ProtocolUtil.BLOCK_SIZE)];
    long[] timestamps = new long[Math.toIntExact(fileSize / ProtocolUtil.BLOCK_SIZE)];

    public static void main(String[] args) {
        new Thread(new Client(args[0])).start();
    }

    public Client(String filename) {
        this.port = 4451;
        this.fileName = filename;
        this.file = new File(fileName);
        this.random = new Random();
        this.randomInteger = random.nextInt();
        this.done = false;
        this.windowBegin = 0;
        this.fileSize = file.length();
        this.windowEnd = ProtocolUtil.getWindowEnd(fileSize, windowBegin);
        this.dataInputStream = null;
        Arrays.fill(received, false);
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
        if (!sendPackets()){
            System.out.println("Error in sending packets");
            return;
        }
        new Thread(new AcknowledgeListener(this)).start();
        while (!done) {
            checkTimestamps();
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean sendPackets() {
        for (int packetIndex = windowBegin; packetIndex <= windowEnd; packetIndex++) {
            if(!received[packetIndex]) {
                if (!sendPacket(packetIndex)) return false;
                timestamps[packetIndex] = System.currentTimeMillis();
            }
        }
        return true;
    }

    private boolean sendPacket(int packetIndex) {
        byte[] packetData = getPacketAtIndex(randomInteger, packetIndex, fileSize, fileData);
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
            return false;
        }
        return true;
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

    private synchronized void checkTimestamps() {
        for (int packetIndex = windowBegin; packetIndex <= windowEnd; packetIndex++) {
            long currentTimeMillis = System.currentTimeMillis();
            if(!received[packetIndex] && currentTimeMillis - timestamps[packetIndex] >= 250) {
                sendPacket(packetIndex);
            }
        }
    }

    public synchronized void updateWindow() {
        //Ryk vinduet så langt som vi har fået receivet, stop hvis vi er færdige, læs vinduet,
        //send det der mangler
        while (received[windowBegin]) {
            windowBegin++;
            if (windowBegin > fileSize / ProtocolUtil.BLOCK_SIZE) {
                done = true;
                return;
            }
        }
        windowEnd = ProtocolUtil.getWindowEnd(fileSize, windowBegin);
        if(!readWindowFromFile()) {
            System.out.println("Error reading window");
        }
        sendPackets();
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
        resultPacket.put(fileData, (packetIndex - windowBegin) * ProtocolUtil.BLOCK_SIZE, packetDataSize);
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
