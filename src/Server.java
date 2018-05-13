import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class Server {

    public static void main(String[] args) {
        List<ServerClientMeta> clients = new ArrayList<>();
        DatagramSocket datagramSocket = null;
        try {
            datagramSocket = new DatagramSocket(4450);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        if (datagramSocket == null) {
            return;
        }
        while (true) {
            byte[] receivedBuffer = new byte[ProtocolUtil.BLOCK_SIZE + 16];
            DatagramPacket datagramPacket = new DatagramPacket(
                    receivedBuffer,
                    receivedBuffer.length);
            try {
                datagramSocket.receive(datagramPacket);
                byte[] packetData = datagramPacket.getData();
                ByteBuffer wrappedPacketData = ByteBuffer.wrap(packetData);
                ServerClientMeta client = new ServerClientMeta(
                        datagramPacket.getAddress(),
                        datagramPacket.getPort(),
                        wrappedPacketData.getInt(),
                        wrappedPacketData.getLong());
                int indexOfClient = clients.indexOf(client);
                if (indexOfClient >= 0) {
                    client = clients.get(indexOfClient);
                } else {
                    clients.add(client);
                }
                int packetIndex = wrappedPacketData.getInt();
                byte[] fileData = new byte[wrappedPacketData.remaining()];
                wrappedPacketData.get(fileData);
                client.handlePacket(packetIndex, fileData);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
