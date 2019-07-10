import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;

public class Main {


  public static void main(String[] args) {
    System.out.println("Hello World!");


    SuRF test = new SuRF(false, 64, SuffixType.kNone, 0, 0);
    String SOURCES = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    Random rand = new Random(System.currentTimeMillis());
    ArrayList<String> words = new ArrayList<String>();
    ArrayList<String> words2 = new ArrayList<String>();


    for (int i = 0; i < 65000; i++) {
      // test.insertSingleKey(SuRFUtils.generateString(rand, SOURCES, rand.).getBytes());
      words.add(SuRFUtils.generateString(rand, SOURCES,5));
      // System.out.println("generate: " + words.get(i));
    }

    HashSet<String>distinctWords = new HashSet<>();

    Collections.sort(words, new Comparator<String>() {
      @Override
      public int compare(String lhs, String rhs) {
        int i = lhs.compareTo(rhs);
        if (i > 0) {
          return 1;
        } else {
          return -1;
        }
      }
    });

    for (int i = 0 ; i < 65000; i++) {
      distinctWords.add(words.get(i));
    }

    // System.out.println(distinctWords);

    System.out.println("words num: 65000; distinct val is" + distinctWords.size() );

    for (Iterator<String> it = distinctWords.iterator(); it.hasNext();) {
      words2.add(it.next());
//      test.insertSingleKey(it.next().getBytes());
    }

    Collections.sort(words2, new Comparator<String>() {
      @Override
      public int compare(String lhs, String rhs) {
        int i = lhs.compareTo(rhs);
        if (i > 0) {
          return 1;
        } else {
          return -1;
        }
      }
    });
//    for (int i = 0; i < 65000; i++) {
//      test.insertSingleKey(words.get(i).getBytes());
//    }
    // System.out.println(words2);
    for (int i = 0 ; i < 10000; i++) {
      test.insertSingleKey(words2.get(i).getBytes());
    }




//    String[] words = {"f", "far", "fas", "fast", "fat", "s", "top", "toy", "trie", "trip", "try"};
//    for (int i = 0; i < words.length; i++) {
//      test.insertSingleKey(words[i].getBytes());
//    }
    test.finishBuilding();
    test.printPerf();

    System.out.println(test.lookupKey("toy".getBytes()));
    System.out.println(test.lookupKey("told".getBytes()));
    System.out.println(test.lookupKey("to".getBytes()));
    System.out.println(test.lookupKey("tom".getBytes()));

    System.out.println("tom ~ toy:" + test.lookupRange("tom".getBytes(), true, "toy".getBytes(), true));
    System.out.println("faz ~ happy:" + test.lookupRange("faz".getBytes(), true, "happy".getBytes(), true));
    System.out.println("trz ~ zzz:" + test.lookupRange("trz".getBytes(), true, "zzz".getBytes(), true));

    try {
      // ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(new File("/tmp/test.surf")));
      FileOutputStream fos = new FileOutputStream(new File("/tmp/test.surf"));
      test.serialize(fos);
      fos.close();

      // RandomAccessFile raf = new RandomAccessFile("/tmp/test.surf", "rw");


      SuRF disk = new SuRF();

      File surfFile = new File("/tmp/test.surf");
      FileChannel fc = new FileInputStream(new File("/tmp/test.surf")).getChannel();
      ByteBuffer byteBuffer = ByteBuffer.allocate((int) surfFile.length());
      System.out.println("Real size :" + surfFile.length());
      fc.read(byteBuffer);
      byteBuffer.flip();
      disk.deSerialize(byteBuffer);

      System.out.println(disk.lookupKey("toy".getBytes()));
      System.out.println(disk.lookupKey("told".getBytes()));
      System.out.println(disk.lookupKey("to".getBytes()));
      System.out.println(disk.lookupKey("tom".getBytes()));

      System.out.println("tom ~ toy:" + disk.lookupRange("tom".getBytes(), true, "toy".getBytes(), true));
      System.out.println("faz ~ happy:" + disk.lookupRange("faz".getBytes(), true, "happy".getBytes(), true));
      System.out.println("trz ~ zzz:" + disk.lookupRange("trz".getBytes(), true, "zzz".getBytes(), true));

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
