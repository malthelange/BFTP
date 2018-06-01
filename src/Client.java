import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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
    private Map<Integer, byte[]> fileData;
    DatagramSocket socket;
    private int serverPort = 4450;
    boolean[] received;
    long[] timestamps;
    private InetAddress remoteHost;
    private long timeout = 250;

    public static void main(String[] args) {
        new Thread(new Client(args[0], args[1])).start();
    }

    public Client(String filename, String hostString) {
        try {
            this.remoteHost = InetAddress.getByName(hostString);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
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
        received = new boolean[(int) Math.floor(fileSize / ProtocolUtil.BLOCK_SIZE) + 1];
        timestamps = new long[(int) Math.floor(fileSize / ProtocolUtil.BLOCK_SIZE) + 1];
        Arrays.fill(received, false);
        fileData = new HashMap<>();
        try {
            dataInputStream = new DataInputStream(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        if (!readWindowFromFile(windowBegin, windowEnd)) return;
        socket = null;
        try {
            socket = new DatagramSocket(port);

        } catch (SocketException e) {
            System.out.println("Couldn't initialize socket");
            e.printStackTrace();
            return;
        }
        if (!sendPackets()) {
            System.out.println("Error in sending packets");
            return;
        }
        new Thread(new AcknowledgeListener(this)).start();
        while (!done) {
            checkTimestamps();
            try {
                Thread.sleep(timeout);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean sendPackets() {
        for (int packetIndex = windowBegin; packetIndex <= windowEnd; packetIndex++) {
            if (!received[packetIndex]) {
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
                    remoteHost,
                    serverPort);
            if (random.nextDouble() > ProtocolUtil.PLP) {
                socket.send(datagramPacket);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean readWindowFromFile(int readWindowBegin, int readWindowEnd) {
        readWindowEnd = Math.toIntExact(Math.min(readWindowEnd, fileSize / ProtocolUtil.BLOCK_SIZE));
        for (int packetIndex = readWindowBegin; packetIndex <= readWindowEnd; packetIndex++) {
            // Size of the packet or the remaining data in the file.
            int newPacketSize = Math.toIntExact(Math.min(
                    ProtocolUtil.BLOCK_SIZE,
                    fileSize - packetIndex * ProtocolUtil.BLOCK_SIZE));
            byte[] newPacket = new byte[newPacketSize];
            int read;
            try {
                read = dataInputStream.read(newPacket);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            if (read != newPacketSize) {
                return false;
            }
            fileData.put(packetIndex, newPacket);
        }
        return true;
    }

    private synchronized void checkTimestamps() {
        for (int packetIndex = windowBegin; packetIndex <= windowEnd; packetIndex++) {
            long currentTimeMillis = System.currentTimeMillis();
            if (!received[packetIndex] && currentTimeMillis - timestamps[packetIndex] >= timeout) {
                sendPacket(packetIndex);
            }
        }
    }

    public synchronized void updateWindow() {
        //Ryk vinduet så langt som vi har fået receivet, stop hvis vi er færdige, læs vinduet,
        //send det der mangler
        int difference = 0;
        while (received[windowBegin]) {
            fileData.remove(windowBegin);
            windowBegin++;
            difference++;
            if (windowBegin > fileSize / ProtocolUtil.BLOCK_SIZE) {
                done = true;
                return;
            }
        }
        if (!readWindowFromFile(windowEnd + 1, windowEnd + difference)) {
            System.out.println("Error reading window");
        }
        windowEnd = ProtocolUtil.getWindowEnd(fileSize, windowBegin);
        sendPackets();
    }

    private byte[] getPacketAtIndex(
            int randomInt,
            int packetIndex,
            long fileSize,
            Map<Integer, byte[]> fileData) {
        int dataSentFromWindowAtIndex = ProtocolUtil.BLOCK_SIZE * (packetIndex - windowBegin);
        int remainingData = fileData.get(packetIndex).length;
        ByteBuffer resultPacket = ByteBuffer.allocate(4 + 8 + 4 + remainingData);
        resultPacket.putInt(randomInt);
        resultPacket.putLong(fileSize);
        resultPacket.putInt(packetIndex);
        resultPacket.put(fileData.get(packetIndex));
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
