public class SuRFConfig {
    public static int kFanout = 256;
    public static int kWordSize = 64;
    public static long kMsbMask = 0x8000000000000000L;
    public static long kOneMask = 0xFFFFFFFFFFFFFFFFL;

    public static boolean kIncludeDense = true;
    public static int kSparseDenseRatio = 16;
    public static char kTerminator = 255;

    public static int kHashShift = 7;

    public static int kCouldBePositive = 2018; // used in suffix comparision

    public static int positionSize = 4; // byte
    public static int levelSize = 4; // byte
    public static int wordSize = 4; // byte
    public static int labelSize = 1; // byte


//  public static void align(byte[] ptr) {
//    ptr =
//  }

    public static int sizeAlign(int size) {
        return (size + 7) & ~((int)7);
    }

    public static long sizeAlign(long size) {
        return (size + 7) & ~((long)7);
    }

}
