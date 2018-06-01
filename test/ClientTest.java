import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Random;

import static org.junit.Assert.*;

public class ClientTest {

    @Test
    public void getPacket() {
        Random random = new Random();
        int R = random.nextInt();
        long S = random.nextLong();
        int index = random.nextInt();
        byte[] packetData = new byte[ProtocolUtil.BLOCK_SIZE];
        random.nextBytes(packetData);
        byte[] resBuffer = new Client("whatever", "127.0.0.1").getPacket(R, S, index, packetData);
        ByteBuffer resWrapped = ByteBuffer.wrap(resBuffer);
        Assert.assertThat(resBuffer.length, IsEqual.equalTo(ProtocolUtil.BLOCK_SIZE + 16));
        Assert.assertThat(resWrapped.getInt(), IsEqual.equalTo(R));
        Assert.assertThat(resWrapped.getLong(), IsEqual.equalTo(S));
        Assert.assertThat(resWrapped.getInt(), IsEqual.equalTo(index));
        byte[] resPacketData = new byte[resWrapped.remaining()];
        resWrapped.get(resPacketData);
        Assert.assertThat(resPacketData, IsEqual.equalTo(packetData));
    }
}