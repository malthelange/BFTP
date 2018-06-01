import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;

public class AcknowledgeListener implements Runnable {
    private Client client;

    public AcknowledgeListener(Client client) {
        this.client = client;
    }

    public void run() {
        while (!client.done) {
            try {
                byte[] receivedBuffer = new byte[ProtocolUtil.BLOCK_SIZE + 16];
                DatagramPacket datagramPacket = new DatagramPacket(
                        receivedBuffer,
                        receivedBuffer.length);
                client.socket.receive(datagramPacket);
                byte[] packetData = datagramPacket.getData();
                ByteBuffer wrappedPacketData = ByteBuffer.wrap(packetData);
                int randomInteger = wrappedPacketData.getInt();
                int packetIndex = wrappedPacketData.getInt();
                if(client.randomInteger == randomInteger) {
                    client.received[packetIndex] = true;
                }
                if(packetIndex == client.windowBegin) {
                    client.updateWindow();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
