import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

class LoudsSparse {
  private FastByteArray labels;
  private FastBitArray childIndicatorBits;
  private FastBitArray loudsBits;

  static int kRankBasicBlockSize = 512;
  static int kSelectSampleInterval = 64;

  private int height;
  private int startLevel;
  private int nodeCountDense;
  private int childCountDense;

  LoudsSparse(SuRFSimpleBuilder builder) {
    this.labels = new FastByteArray();
    this.childIndicatorBits = new FastBitArray();
    this.loudsBits = new FastBitArray();

    // get bit/byte array from builder
    for (int level = 0; level < builder.getTrieHeight(); level++) {
      for (int i = 0; i < builder.labels[level].getSize(); i++) {
        this.labels.add(builder.labels[level].get(i));
        this.childIndicatorBits.addBit(builder.childIndicatorBits[level].getBit(i));
        this.loudsBits.addBit(builder.loudsBits[level].getBit(i));
      }
    }

    this.childIndicatorBits.generateRankAndSelect();
    this.loudsBits.generateRankAndSelect();

    this.startLevel = builder.getSparseStartLevel();
    this.nodeCountDense = 0;
    for (int i = 0; i < startLevel; i++) {
      nodeCountDense += builder.getNodeCountsInLevel(i);
    }
    this.height = builder.getTrieHeight();
  }

  LoudsSparse() {
  }

  boolean lookupKey(byte[] key, int inNodeNum) {
    int pos = getFirstLabelPos(inNodeNum);
    for (int level = startLevel; level < key.length; level++) {
      // int nodeSize = nodeSize(pos);
      int nodeEndPos = getNodeEndPosition(pos);
      int getKeyPos = search(key[level], pos, nodeEndPos);

      // can not find this byte
      if (getKeyPos == -1)
        return false;

      // this node has no child node and key has bytes left.
      if (level != (key.length - 1) && !childIndicatorBits.getBit(getKeyPos))
        return false;

      // if trie branch terminates
      // should include some suffix
      // return suffixes.check(leftKey);

      if (level == height)
        break;

      pos = loudsBits.select1(childIndicatorBits.rank1(getKeyPos) + 1);
    }
    return true;
  }

  boolean getLowerBoundPosition(byte[] key, int nodeNum, ArrayList<Integer> path) {
    int pos = getFirstLabelPos(nodeNum);

    for (int level = startLevel; level < key.length; level++) {
      int nodeEndPos = getNodeEndPosition(pos);
      int getKeyPos = search(key[level], pos, nodeEndPos);

      // can not find this byte
      // then find lower bound. If lower bound exist , add to path
      if (getKeyPos == -1) {
        int lowerBound = linerSearch(key[level], pos, nodeEndPos);
        // if (lowerBound != -1)
        // Adding -1 to path means to trace upper level
        path.add(lowerBound);
        return false;
      } else {
        path.add(getKeyPos);
      }

      // this node has no child and key has bytes left.
      if (level != (key.length - 1) && !childIndicatorBits.getBit(getKeyPos))
        return false;
      pos = loudsBits.select1(childIndicatorBits.rank1(getKeyPos) + 1);
    }
    return true;
  }

  boolean findNextSatisfiedKey(ArrayList<Integer> path, ArrayList<Byte> element, byte[] key) {
    // add label to element according to path except last position
    for (int i = 0; i < path.size() - 1; i++) {
      element.add(labels.get(path.get(i)));
    }

    if (path.get(path.size() - 1) == -1) {
      path.remove(path.size() - 1);
      findPreLevelSatisfiedKey(path, element);
    } else {
      int startPos = path.get(path.size() - 1);
      preOrderTrav(startPos, element, element.size());
    }
    return (element.size() > 0) && SuRFUtils.isLessThanRight(element, key);
  }

  private void findPreLevelSatisfiedKey(ArrayList<Integer> path, ArrayList<Byte> element) {
    if (path.isEmpty()) {
      return;
    } else {
      int lastIdx = path.get(path.size() - 1);
      if ((lastIdx + 1 < loudsBits.getSize()) && !loudsBits.getBit(lastIdx + 1)) {
        element.remove(element.size() - 1);
        preOrderTrav(lastIdx + 1, element, element.size());
      } else {
        path.remove(path.size() - 1);
        element.remove(element.size() - 1);
        findPreLevelSatisfiedKey(path, element);
      }
    }
  }

  void preOrderTrav(int startPos, ArrayList<Byte> element, int level) {
    // int nodeEndPos = getNodeEndPosition(startPos);
    if (labels.get(startPos) != (byte) 255) {
      element.add(labels.get(startPos));
      level++;
    } else {
      return;
    }

    if (childIndicatorBits.getBit(startPos) && level < (this.height - 1)) {
      int pos = loudsBits.select1(childIndicatorBits.rank1(startPos) + 1);
      preOrderTrav(pos, element, level + 1);
    }

  }

  private int linerSearch(byte key, int begin, int end) {
    for (int i = begin; i <= end; i++) {
      if (key < labels.get(i))
        return i;
    }
    return -1;
  }

  private int search(byte key, int begin, int end) {
    int mid;
    while (begin <= end) {
      mid = (begin + end) / 2;
      if (labels.get(mid) == key)
        return mid;
      else if (labels.get(mid) < key)
        begin = mid + 1;
      else
        end = mid - 1;
    }
    return -1;
  }

  private int getFirstLabelPos(int nodeNum) {
    return this.loudsBits.select1(nodeNum);
  }

  private int getNodeEndPosition(int pos) {
    pos++;
    while (!loudsBits.getBit(pos) && pos < loudsBits.getSize()) {
      pos++;
    }
    return (pos - 1);
  }

  public long getSerializedSize() {
    return this.labels.getSize() * 10 / 8 + SuRFConfig.labelSize * 4;
  }

  public long getMemorySize() {
    return 0;
  }

  public int getAllElementSize() {
    return labels.getSize();
  }

  public void serialize(OutputStream out) throws IOException {
    DataOutputStream dos = new DataOutputStream(out);
    dos.writeInt(height);
    dos.writeInt(startLevel);
    dos.writeInt(loudsBits.getSize());
    dos.writeInt(loudsBits.currentPos);
    int setNum = 0;
    HashSet<Integer> lengths = new HashSet<>();
    HashMap<Integer, AtomicInteger> trie = new HashMap<>();

    for (int i = 0 ; i < loudsBits.getSize(); ) {
      BitSet tmp = new BitSet(128);
      FastBitArray bitsets = new FastBitArray(128);
      int count = 1;
      int j = i + 1;
      for (; j < loudsBits.getSize() ;j ++) {
        if (loudsBits.getBit(j))
          break;
        else
          count ++;
      }
      // if one node has more than 17 path, we put them in a bitSet which is 8 + 128 bit
      // the first byte(8 bit) is flag to mark next 16byte(128 bit) is a bitSet
      lengths.add(count);
      if (trie.containsKey(count)) {
        trie.get(count).incrementAndGet();
      } else {
        trie.put(count, new AtomicInteger(1));
      }
      if (count >= 17) {
        setNum ++;
        dos.writeByte(255);
        for (int index = i; index < j ; index++) {
          bitsets.setBitToTrue(labels.get(index));
        }
        dos.write(bitsets.elementData, 0, 16);
      } else {
        dos.write(labels.elementData, i, j-i);
      }
      i = j;
    }
    // dos.write(labels.elementData, 0, labels.getSize());
    // dos.writeChars(labels.toString());
    dos.write(childIndicatorBits.elementData, 0, childIndicatorBits.currentPos);
    dos.write(loudsBits.elementData, 0, loudsBits.currentPos);
    System.out.println("write " + setNum + " bitset into file");
    System.out.println(lengths);
    System.out.println(trie);
  }

  public void deSerialize(ByteBuffer buffer) throws IOException {
    this.height = buffer.getInt();
    this.startLevel = buffer.getInt();
    int size = buffer.getInt();
    int bitPosition = buffer.getInt();

    this.labels = new FastByteArray();
    int count = 0;
    // get labels in buffer
    for (int i = 0 ; i < size ; ) {
      byte tmp = buffer.get();
      if (tmp != (byte)255) {
        this.labels.add(tmp);
        i++;
      } else {
        count ++;
        byte[] bitset = new byte[16];
        buffer.get(bitset, 0, 16);
        for (int element = 0; element < 128; element++) {
          if (((bitset[element/8] & (1<<(7 - element%8))) != 0)) {
            this.labels.add((byte)element);
            i++;
          }
        }

      }
    }
    System.out.println("Get " + count + " bitset from file.");

    this.childIndicatorBits = new FastBitArray();
    this.loudsBits = new FastBitArray();

    buffer.get(childIndicatorBits.elementData, 0, bitPosition);
    childIndicatorBits.size = size;
    childIndicatorBits.generateRankAndSelect();
    buffer.get(loudsBits.elementData, 0, bitPosition);
    loudsBits.size = size;
    loudsBits.generateRankAndSelect();
  }

}
