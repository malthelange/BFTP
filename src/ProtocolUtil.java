public class ProtocolUtil {
    static final int WINDOW_SIZE = 1;
    static int BLOCK_SIZE = 500;

    static int getWindowEnd(long fileSize, int windowBegin) {
        int endOfWindow = ProtocolUtil.WINDOW_SIZE - 1 + windowBegin;
        int endOfFile = (int) (Math.ceil(fileSize / (double) ProtocolUtil.BLOCK_SIZE)) - 1;
        return Math.min(endOfWindow, endOfFile);
    }
}
