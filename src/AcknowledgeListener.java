import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;

public class AcknowledgeListener implements Runnable {
    private Client client;
    DatagramSocket socket;

    public AcknowledgeListener(Client client) {
        this.client = client;
    }

    public void run() {
        while (!client.done) {
            try {
                socket = new DatagramSocket(client.port);
                byte[] receivedBuffer = new byte[ProtocolUtil.BLOCK_SIZE + 16];
                DatagramPacket datagramPacket = new DatagramPacket(
                        receivedBuffer,
                        receivedBuffer.length);
                socket.receive(datagramPacket);
                byte[] packetData = datagramPacket.getData();
                ByteBuffer wrappedPacketData = ByteBuffer.wrap(packetData);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
