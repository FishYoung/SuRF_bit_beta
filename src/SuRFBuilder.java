import java.util.ArrayList;

public class SuRFBuilder {
  // trie level < sparse_start_level_: LOUDS-Dense
  // trie level >= sparse_start_level_: LOUDS-Sparse
  private boolean includeDense;
  private int sparseDenseRatio;
  private int sparseStartLevel;

  // LOUDS-Sparse bit/byte vectors
  ArrayList<ArrayList<Byte>> labels;
  ArrayList<ArrayList<Long>> childIndicatorBits;
  ArrayList<ArrayList<Long>> loudsBits;

  // LOUDS-Dense bit vectors
  ArrayList<ArrayList<Long>> bitmapLabels;
  ArrayList<ArrayList<Long>> bitmapChildIndicatorBits;
  ArrayList<ArrayList<Long>> prefixkeyIndicatorBits;

  SuffixType suffixType;
  int hashSuffixLen;
  int realSuffixLen;
  ArrayList<ArrayList<Long>> suffixes;
  ArrayList<Integer> suffixCounts;

  // auxiliary per level bookkeeping vectors
  ArrayList<Integer> nodeCounts;
  ArrayList<Boolean> isLastItemTerminator;

  SuRFBuilder(boolean includeDense, int sparseDenseRatio, SuffixType suffixType, int hashSuffixLen,
              int realSuffixLen) {
    this.includeDense = includeDense;
    this.sparseDenseRatio = sparseDenseRatio;
    this.suffixType = suffixType;
    this.hashSuffixLen = hashSuffixLen;
    this.realSuffixLen = realSuffixLen;
  }

  static boolean readBit(ArrayList<Long> bits, int pos) {
    if (pos >= (bits.size() * SuRFConfig.kWordSize))
      throw new RuntimeException("readBit failed.");
    int wordId = pos / SuRFConfig.kWordSize;
    int offset = pos % SuRFConfig.kWordSize;

    return (bits.get(wordId).longValue() & (SuRFConfig.kMsbMask >> offset)) != 0L;
  }

  // Fills in the LOUDS-dense and sparse vectors (members of this class)
  // through a single scan of the sorted key list.
  // After build, the member vectors are used in SuRF constructor.
  // REQUIRED: provided key list must be sorted.
  void build(ArrayList<String> keys) {
    if (keys.size() <= 0) {
      throw new RuntimeException("SuRF needs keys before build.");
    }
    buildSparse(keys);
    if (includeDense) {
      determineCutoffLevel();
      buildDense();
    }
  }

  void buildSparse(ArrayList<String> keys) {
    for (int i = 0; i < keys.size(); i++) {
      int level = skipCommonPrefix(keys.get(i));
      int curpos = i;
      while ((i + 1 < keys.size()) && isSameKey(keys.get(curpos), keys.get(i + 1)))
        i++;
      if (i < keys.size() - 1)
        level = insertKeyBytesToTrieUntilUnique(keys.get(curpos), keys.get(i + 1), level);
      else
        level = insertKeyBytesToTrieUntilUnique(keys.get(curpos), "", level);
      insertSuffix(keys.get(curpos), level);
    }
  }

  void buildDense() {
    for (int level = 0; level < sparseStartLevel; level++) {
      initDenseVectors(level);
      if (getNumItems(level) == 0) continue;

      int nodeNum = 0;
      if (isTerminator(level, 0)) {
        setBit(prefixkeyIndicatorBits.get(level), 0);
      } else {
        setLabelAndChildIndicatorBitmap(level, nodeNum, 0);
      }
      for (int pos = 1; pos < getNumItems(level); pos++) {
        if (isStartOfNode(level, pos)) {
          nodeNum++;
          if (isTerminator(level, pos)) {
            setBit(prefixkeyIndicatorBits.get(level), nodeNum);
            continue;
          }
        }
        setLabelAndChildIndicatorBitmap(level, nodeNum, pos);
      }
    }
  }

  void initDenseVectors(int level) {
    bitmapLabels.add(new ArrayList<Long>());
    bitmapChildIndicatorBits.add(new ArrayList<Long>());
    prefixkeyIndicatorBits.add(new ArrayList<Long>());

    for (int nc = 0; nc < nodeCounts.get(level); nc++) {
      for (int i = 0; i < SuRFConfig.kFanout; i += SuRFConfig.kWordSize) {
        bitmapLabels.get(level).add(0L);
        bitmapChildIndicatorBits.get(level).add(0L);
      }
      if (nc % SuRFConfig.kWordSize == 0) {
        prefixkeyIndicatorBits.get(level).add(0L);
      }
    }
  }

  int skipCommonPrefix(String key) {
    int level = 0;
    while (level < key.length() && isCharCommonPrefix(key.charAt(level), level)) {
      setBit(childIndicatorBits.get(level), getNumItems(level) - 1);
      level++;
    }
    return level;
  }

  void insertSuffix(String key, int level) {
    if (level >= getTreeHeight())
      addLevel();
    if (level - 1 < suffixes.size())
      throw new RuntimeException("insert suffix failed");

    // TODO: class BitvectorSuffix not finished yet.
    // Long suffixWord = BitvectorSuffix.constructSuffix(suffixType, key, hashSuffixLen, level, realSuffixLen);
    // storeSuffix(level, suffixWord);
  }

  final void storeSuffix(int level, long suffix) {
    int suffixLen = getSuffixLen();
    int pos = suffixCounts.get(level - 1) * suffixLen;
    if (pos > (suffixes.get(level - 1).size() * SuRFConfig.kWordSize))
      throw new RuntimeException("store suffix failed");
    if (pos == (suffixes.get(level - 1).size() * SuRFConfig.kWordSize))
      suffixes.get(level - 1).add(0L);
    int wordId = pos / SuRFConfig.kWordSize;
    int offset = pos % SuRFConfig.kWordSize;
    int wordRemainingLen = SuRFConfig.kWordSize - offset;

    if (suffixLen <= wordRemainingLen) {
      long shiftedSuffix = suffix << (suffixLen - wordRemainingLen);
      // TODO: Optimize this
      suffixes.get(level - 1).set(wordId, suffixes.get(level - 1).get(wordId) + shiftedSuffix);
    } else {
      long suffixLeftPart = suffix >> (suffixLen - wordRemainingLen);
      suffixes.get(level - 1).set(wordId, suffixes.get(level - 1).get(wordId) + suffixLeftPart);
      suffixes.get(level - 1).add(0L);
      wordId++;
      long suffixRightPart = suffix << (SuRFConfig.kWordSize - (suffixLen - wordRemainingLen));
      suffixes.get(level - 1).set(wordId, suffixes.get(level - 1).get(wordId) + suffixRightPart);
    }
    suffixCounts.set(level - 1, suffixCounts.get(level - 1) + 1);
  }

  final boolean isCharCommonPrefix(char c, int level) {
    return (level < getTreeHeight()) && (!isLastItemTerminator.get(level))
      && (c == labels.get(level).get(labels.size() - 1));
  }

  int insertKeyBytesToTrieUntilUnique(String key, String next_key, int startLevel) {
    if (startLevel >= key.length()) {
      throw new RuntimeException("Input should be sorted.");
    }

    int level = startLevel;
    boolean isStartOfNode = false;
    boolean isTerm = false;

    // If it is the start of level, the louds bit needs to be set.
    if (isLevelEmpty(level))
      isStartOfNode = true;

    // After skipping the common prefix, the first following byte
    // should be in an the node as the previous key.
    insertKeyByte(key.charAt(level), level, isStartOfNode, isTerm);
    level++;
    if (level > next_key.length() || !isSameKey(key.substring(0, level), next_key.substring(0, level)))
      return level;

    // All the following bytes inserted must be the start of a new node.
    isStartOfNode = true;
    while (level < key.length() && level < next_key.length() && key.charAt(level) == next_key.charAt(level)) {
      insertKeyByte(key.charAt(level), level, isStartOfNode, isTerm);
      level++;
    }

    // The last byte inserted makes key unique in the trie.
    if (level < key.length()) {
      insertKeyByte(key.charAt(level), level, isStartOfNode, isTerm);
    } else {
      isTerm = true;
      insertKeyByte(SuRFConfig.kTerminator, level, isStartOfNode, isTerm);
    }
    level++;

    return level;
  }

  int getTreeHeight() {
    return labels.size();
  }

  int getNumItems(int level) {
    return labels.get(level).size();
  }

  void insertKeyByte(char c, int level, boolean isStartOfNode, boolean isTerm) {
    // level should be at most equal to tree height
    if (level >= getTreeHeight())
      addLevel();

    if (level >= getTreeHeight())
      throw new RuntimeException("Insert key byte failed");

    // sets parent node's child indicator
    if (level > 0) {
      setBit(childIndicatorBits.get(level - 1), getNumItems(level - 1) - 1);
    }

    labels.get(level).add((byte) c);
    if (isStartOfNode) {
      setBit(loudsBits.get(level), getNumItems(level) - 1);
      // TODO: Optimize this
      nodeCounts.set(level, nodeCounts.get(level) + 1);
    }
    isLastItemTerminator.set(level, isTerm);

    moveToNextItemSlot(level);
  }

  public static void setBit(ArrayList<Long> bits, int pos) {
    if (pos < (bits.size() * SuRFConfig.kWordSize)) {
      throw new RuntimeException("Internal error in building SuRF");
    }
  }

  final void determineCutoffLevel() {
    int cutoffLevel = 0;
    long denseMem = computeDenseMem(cutoffLevel);
    long sparseMem = computeSparseMem(cutoffLevel);

    while ((cutoffLevel < getTreeHeight()) && (denseMem * sparseDenseRatio < sparseMem)) {
      cutoffLevel++;
      denseMem = computeDenseMem(cutoffLevel);
      sparseMem = computeSparseMem(cutoffLevel);
    }
    cutoffLevel--;
    sparseStartLevel = cutoffLevel;
  }

  final boolean isLevelEmpty(int level) {
    return (level >= getTreeHeight() || (labels.get(level).size()) == 0);
  }

  final void moveToNextItemSlot(int level) {
    if (level >= getTreeHeight())
      throw new RuntimeException("move to next item slot failed ");
    int numItems = getNumItems(level);
    if (numItems % SuRFConfig.kWordSize == 0) {
      childIndicatorBits.get(level).add(0L);
    }
  }

  long computeDenseMem(int downToLevel) {
    if (downToLevel <= getTreeHeight()) {
      throw new RuntimeException("Internal error in computeDenseMem");
    }
    long mem = 0;
    for (int level = 0; level < downToLevel; level++) {
      mem += (2 * SuRFConfig.kFanout * nodeCounts.get(level));
      if (level > 0) {
        mem += (nodeCounts.get(level - 1) / 8 + 1);
      }
      mem += (suffixCounts.get(level) * getSuffixLen() / 8);
    }
    return mem;
  }

  long computeSparseMem(int startLevel) {
    long mem = 0;
    for (int level = startLevel; level < getTreeHeight(); level++) {
      int numItems = labels.get(level).size();
      mem += (numItems + 2 * numItems / 8 + 1);
      mem += (suffixCounts.get(level) * getSuffixLen() / 8);
    }
    return mem;
  }

  int getSuffixLen() {
    return hashSuffixLen + realSuffixLen;
  }

  private boolean isSameKey(String a, String b) {
    return a.equals(b);
  }

  void setLabelAndChildIndicatorBitmap(int level, int nodeNum, int pos) {
    int label = labels.get(level).get(pos);
    setBit(bitmapLabels.get(level), nodeNum * SuRFConfig.kFanout + label);
    if (readBit(childIndicatorBits.get(level), pos)) {
      setBit(bitmapChildIndicatorBits.get(level), nodeNum * SuRFConfig.kFanout + label);
    }
  }

  void addLevel() {
    labels.add(new ArrayList<Byte>());
    childIndicatorBits.add(new ArrayList<Long>());
    loudsBits.add(new ArrayList<Long>());
    suffixes.add(new ArrayList<Long>());
    suffixCounts.add(0);

    nodeCounts.add(0);
    isLastItemTerminator.add(false);

    childIndicatorBits.get(getTreeHeight() - 1).add(0l);
    loudsBits.get(getTreeHeight() - 1).add(0l);
  }

  boolean isTerminator(int level, int pos) {
    int label = labels.get(level).get(pos);
    return ((label == SuRFConfig.kTerminator) && !readBit(childIndicatorBits.get(level), pos));
  }

  boolean isStartOfNode(int level, int pos) {
    return readBit(loudsBits.get(level), pos);
  }

  public int getSparseStartLevel() {
    return this.sparseStartLevel;
  }

  public ArrayList<ArrayList<Long>> getBitmapLabels() {
    return bitmapLabels;
  }

  public ArrayList<ArrayList<Long>> getBitmapChildIndicatorBits() {
    return bitmapChildIndicatorBits;
  }

  public ArrayList<ArrayList<Long>> getPrefixkeyIndicatorBits() {
    return prefixkeyIndicatorBits;
  }

  public SuffixType getSuffixType() {
    return suffixType;
  }

  public int getHashSuffixLen() {
    return hashSuffixLen;
  }

  public int getRealSuffixLen() {
    return realSuffixLen;
  }

  public ArrayList<Integer> getSuffixCounts() {
    return suffixCounts;
  }

  public ArrayList<ArrayList<Long>> getSuffixes() {
    return suffixes;
  }

  public ArrayList<ArrayList<Byte>> getLabels() {
    return labels;
  }

  public ArrayList<Integer> getNodeCounts() {
    return nodeCounts;
  }

  public ArrayList<ArrayList<Long>> getChildIndicatorBits() {
    return childIndicatorBits;
  }

  public ArrayList<ArrayList<Long>> getLoudsBits() {
    return loudsBits;
  }
}
