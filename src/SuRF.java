import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class SuRF {
  private LoudsDense loudsDense;
  private LoudsSparse loudsSparse;
  private SuRFBuilder builder;
  private SuRFSimpleBuilder simpleBuilder;


  SuRF() {
  }

  SuRF(ArrayList<String> keys) {
    create(keys, SuRFConfig.kIncludeDense, SuRFConfig.kSparseDenseRatio, SuffixType.kNone, 0, 0);
  }

  SuRF(ArrayList<String> keys, SuffixType suffixType, int hashSuffixLen, int realSuffixLen) {
    create(keys, SuRFConfig.kIncludeDense, SuRFConfig.kSparseDenseRatio, suffixType, hashSuffixLen, realSuffixLen);
  }

  SuRF(ArrayList<String> keys, boolean includeDense, int sparseDenseRatio, SuffixType suffixType,
       int hashSuffixLen, int realSuffixLen) {
    create(keys, includeDense, sparseDenseRatio, suffixType, hashSuffixLen, realSuffixLen);
  }

  SuRF(boolean includeDense, int sparseDenseRatio, SuffixType suffixType,
       int hashSuffixLen, int realSuffixLen) {
    create(includeDense, sparseDenseRatio, suffixType, hashSuffixLen, realSuffixLen);
  }

  public void create(ArrayList<String> keys, boolean includeDense, int sparseDenseRadio, SuffixType suffixType,
                     int hashSuffixLen, int realSuffixLen) {
    builder = new SuRFBuilder(includeDense, sparseDenseRadio, suffixType, hashSuffixLen, realSuffixLen);
    builder.build(keys);
    // loudsDense = new LoudsDense(builder);
    // loudsSparse = new LoudsSparse(builder);
    // iter = new Iter(this);
  }

  public void create(boolean includeDense, int sparseDenseRadio, SuffixType suffixType,
                     int hashSuffixLen, int realSuffixLen) {
    simpleBuilder = new SuRFSimpleBuilder(includeDense, sparseDenseRadio, suffixType, hashSuffixLen, realSuffixLen);
  }

  public void insertSingleKey(byte[] value) {
    simpleBuilder.insertSingleKey(value);
  }

  public void finishBuilding() {
    simpleBuilder.finishBuilding();
    loudsSparse = new LoudsSparse(simpleBuilder);
    loudsDense = new LoudsDense(simpleBuilder);
  }

//    public void generateFinalFilter() {
//        loudsSparse = new LoudsSparse(simpleBuilder);
//        // loudsDense = new LoudsDense(simpleBuilder);
//    }

  public boolean lookupKey(String key) {
    // byte[] keyBytes = key.getBytes();
    int connectNodeNum = 0;
    if (!loudsDense.lookupKey(key.getBytes(), connectNodeNum))
      return false;
    else if (connectNodeNum != 0)
      return loudsSparse.lookupKey(key.getBytes(), connectNodeNum);
    else
      return true;
  }

  public boolean lookupKey(byte[] key) {
    //  TODO: connectNodeNum start from zero or one???
    int conncetNodeNum = 1;
    if (!loudsDense.lookupKey(key, conncetNodeNum)) {
      return false;
    } else if (conncetNodeNum != 0) {
      return loudsSparse.lookupKey(key, conncetNodeNum);
    } else {
      return true;
    }
  }

  public boolean lookupRange(byte[] leftKey, boolean leftInclusive,
                             byte[] rightKey, boolean rightInclusive) {
    int connectNodeNum = 1;
    // FastIntArray path = new FastIntArray();
    ArrayList<Integer>path = new ArrayList<Integer>();
    ArrayList<Byte>element = new ArrayList<>();
    if (leftKey != null) {
      if (loudsSparse.getLowerBoundPosition(leftKey, connectNodeNum, path)) {
        return true;
      } else {
        if (loudsSparse.findNextSatisfiedKey(path, element, rightKey)) {
          return true;
        } else {
          return false;
        }
      }
    } else {
      // ArrayList<Byte>element = new ArrayList<Byte>();
      loudsSparse.preOrderTrav(0, element, 0);
      if (element.toString().compareTo(rightKey.toString()) < 0 ) {
        return true;
      } else {
        return false;
      }
    }
  }

  public long getSerializedSize() {
    return loudsSparse.getSerializedSize(); // + loudsDense.getSerializedSize();
  }

  public long getMemorySize() {
    return loudsSparse.getMemorySize(); // + loudsDense.getSerializedSize();
  }

  public void serialize(OutputStream out) throws IOException {
    // loudsDense.serialize(out);
    loudsSparse.serialize(out);
  }

  public void deSerialize(ByteBuffer buffer) throws IOException {
    loudsDense = new LoudsDense();
    // loudsDense.deSerialize(buffer);

    loudsSparse = new LoudsSparse();
    loudsSparse.deSerialize(buffer);
  }

  public void printPerf() {
    System.out.println("loudsSparse elements: " + loudsSparse.getAllElementSize());
    System.out.println("loudsSparse disk size: " + loudsSparse.getAllElementSize() * 1.25);
  }
}


enum SuffixType {
  kNone,
  kHash,
  kReal,
  kMixed
}