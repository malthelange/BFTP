public class ProtocolUtil {
    static final int WINDOW_SIZE = 10;
    static int BLOCK_SIZE = 1000;
    static final double PLP = 0;

    static int getWindowEnd(long fileSize, int windowBegin) {
        int endOfWindow = ProtocolUtil.WINDOW_SIZE - 1 + windowBegin;
        int endOfFile = (int) (Math.floor(fileSize / (double) ProtocolUtil.BLOCK_SIZE));
        return Math.min(endOfWindow, endOfFile);
    }
}
