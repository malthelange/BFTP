public class ProtocolUtil {
    static final int WINDOW_SIZE = 20;
    static int BLOCK_SIZE = 500;

    static int getWindowEnd(double fileSize) {
        return Math.min(
                ProtocolUtil.WINDOW_SIZE - 1,
                (int) Math.ceil(fileSize / (double) ProtocolUtil.BLOCK_SIZE));
    }
}
