import java.util.ArrayList;
import java.util.BitSet;
import java.util.Random;

public class SuRFUtils {
  public static byte[] intToByte(int number) {
    int temp = number;
    byte[] b = new byte[4];
    for (int i = 0; i < b.length; i++) {
      b[i] = new Integer(temp & 0xff).byteValue();
      temp = temp >> 8;
    }
    return b;
  }

  public static byte[] shortToByteArray(short[] src) {

    int count = src.length;
    byte[] dest = new byte[count << 1];
    for (int i = 0; i < count; i++) {
      dest[i * 2] = (byte) (src[i] >> 8);
      dest[i * 2 + 1] = (byte) (src[i] >> 0);
    }

    return dest;
  }

  public static short[] byteToShortArray(byte[] src) {

    int count = src.length >> 1;
    short[] dest = new short[count];
    for (int i = 0; i < count; i++) {
      dest[i] = (short) (src[i * 2] << 8 | src[2 * i + 1] & 0xff);
    }
    return dest;
  }

  public static byte[] byteMerger(byte[] bt1, byte[] bt2) {
    byte[] bt3 = new byte[bt1.length + bt2.length];
    System.arraycopy(bt1, 0, bt3, 0, bt1.length);
    System.arraycopy(bt2, 0, bt3, bt1.length, bt2.length);
    return bt3;
  }

  public static int byteToInt(byte[] b) {
    int s = 0;
    int s0 = b[0] & 0xff;
    int s1 = b[1] & 0xff;
    int s2 = b[2] & 0xff;
    int s3 = b[3] & 0xff;
    s3 <<= 24;
    s2 <<= 16;
    s1 <<= 8;
    s = s0 | s1 | s2 | s3;
    return s;
  }

  public static int[] byteArrayToIntArray(byte[] src) {
    int count = src.length / 4;
    int[] ret = new int[count];
    for (int i = 0; i < count; i++) {
      ret[i] = (src[i * 4] & 0xff) |
        (src[i * 4] & 0xff) << 8 |
        (src[i * 4] & 0xff) << 16 |
        (src[i * 4] & 0xff) << 24;
    }
    return ret;
  }

  public static byte[] longToByte(long number) {
    long temp = number;
    byte[] b = new byte[8];
    for (int i = 0; i < 8; i++) {
      b[i] = (byte) (temp);
      temp >>>= 8;
    }
//    for (int i = 0; i < b.length; i++) {
//      b[i] = new Long(temp & 0xff).byteValue();
//      temp >>= 8;
//    }
    return b;
  }

  public static long byteToLong(byte[] b) {
    long s = 0;
    long s0 = b[0] & 0xff;
    long s1 = b[1] & 0xff;
    long s2 = b[2] & 0xff;
    long s3 = b[3] & 0xff;
    long s4 = b[4] & 0xff;
    long s5 = b[5] & 0xff;
    long s6 = b[6] & 0xff;
    long s7 = b[7] & 0xff;

    s1 <<= 8;
    s2 <<= 16;
    s3 <<= 24;
    s4 <<= 8 * 4;
    s5 <<= 8 * 5;
    s6 <<= 8 * 6;
    s7 <<= 8 * 7;
    s = s0 | s1 | s2 | s3 | s4 | s5 | s6 | s7;
    return s;
  }


  public static byte[] longArrayToByte(long[] src) {
    int count = src.length;
    byte[] ret = new byte[count * 8];
    for (int i = 0; i < count; i++) {
      System.arraycopy(longToByte(src[i]), 0, ret, i * 8, 8);
    }
    return ret;
  }

  public static long[] byteArrayToLongArray(byte[] src) {
    int count = src.length / 8;
    long[] ret = new long[count];
    for (int i = 0; i < count; i++) {
      ret[i] = (long) (src[i * 8] & 0xff) |
        (long) (src[i * 8] & 0xff) << 8 |
        (long) (src[i * 8] & 0xff) << 16 |
        (long) (src[i * 8] & 0xff) << 24 |
        (long) (src[i * 8] & 0xff) << 32 |
        (long) (src[i * 8] & 0xff) << 40 |
        (long) (src[i * 8] & 0xff) << 48 |
        (long) (src[i * 8] & 0xff) << 56;
    }
    return ret;
  }

  public static byte[] intArrayToByte(int[] src) {
    int count = src.length;
    byte[] ret = new byte[count * 4];
    for (int i = 0; i < count; i++) {
      System.arraycopy(intToByte(src[i]), 0, ret, i * 4, 4);
    }
    return ret;
  }


  public static boolean isLessThanRight(ArrayList<Byte> left, byte[] right) {
    if (right == null)
      return true;

    int minLen = (left.size() < right.length)? left.size():right.length;
    for (int i = 0 ; i < minLen ; i++){
      if (left.get(i) == right[i])
        continue;
      else if (left.get(i) < right[i])
        return true;
      else
        return false;
    }

    if (left.size() > right.length)
      return false;
    else
      return true;
  }

  public static String generateString(Random random, String characters, int length) {
    if (length == 0)
      length++;
    char[] text = new char[length];
    for (int i = 0; i < length; i++) {
      text[i] = characters.charAt(random.nextInt(characters.length()));
    }
    return new String(text);
  }

  public static BitSet getBitSetFromByteArray(byte[] bytes) {
    BitSet bits = new BitSet();
    for (int i=0; i<bytes.length*8; i++) {
      if ((bytes[bytes.length-i/8-1]&(1<<(i%8))) > 0) {
        bits.set(i);
      }
    }
    return bits;
  }
}
