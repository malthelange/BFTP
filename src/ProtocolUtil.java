public class ProtocolUtil {
    static final int WINDOW_SIZE = 20;
    static int BLOCK_SIZE = 500;

    static int getWindowEnd(long fileSize, int windowBegin) {
        return Math.min(
                ProtocolUtil.WINDOW_SIZE - 1 + windowBegin,
                (int) Math.ceil(fileSize / (double) ProtocolUtil.BLOCK_SIZE));
    }
}
