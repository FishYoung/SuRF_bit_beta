import java.util.ArrayList;

public class SuRFSimpleBuilder {
  // trie level < sparse_start_level_: LOUDS-Dense
  // trie level >= sparse_start_level_: LOUDS-Sparse
  private boolean includeDense;
  private int sparseDenseRatio;
  private int sparseStartLevel;

  SuffixType suffixType;
  int hashSuffixLen;
  int realSuffixLen;
  int trieHeight;

  // LOUDS-Sparse bit/byte vectors
  FastByteArray[] labels;
  FastBitArray[] childIndicatorBits;
  FastBitArray[] loudsBits;

  // LOUDS-Dense bit vectors
  FastBitMap[] labelBitmaps;
  FastBitMap[] childIndicatorBitMaps;
  FastBitArray[] prefixkeyIndicatorBits;

  FastIntArray nodeCounts;


  SuRFSimpleBuilder(boolean includeDense, int sparseDenseRatio, SuffixType suffixType,
                    int hashSuffixLen, int realSuffixLen) {
    this.includeDense = includeDense;
    this.sparseDenseRatio = sparseDenseRatio;
    this.suffixType = suffixType;
    this.hashSuffixLen = hashSuffixLen;
    this.realSuffixLen = realSuffixLen;
    this.labels = new FastByteArray[50];
    this.childIndicatorBits = new FastBitArray[50];
    this.loudsBits = new FastBitArray[50];
    this.trieHeight = 0;
    this.nodeCounts = new FastIntArray();
  }

  // insert key into LOUDS-Sparse
  void insertSingleKey(byte[] key) {
    boolean addedNewElement = false;
    int keyLen = key.length;
    for (int i = 0; i < keyLen; i++) {
      // Actually there should be another level to judge whether louds[i] add '0' or '1'
      if (i == 0 && labels[i] == null) {
        addedNewElement = true;
      }

      // Find suffix is exist in this level or not; (-1 means no)
      int suffixIdx = findSuffix(i, key[i], addedNewElement);
      if (suffixIdx == -1) {
        // Add this key to current level
        addKeyToSparse(key[i], i, addedNewElement, (i != (keyLen - 1)));
        addedNewElement = true;
        continue;
      } else {
        // if it changes the S-HasChild[i][suffixIdx] from '0' to '1', we should put '$' to next level
        if ((i != (keyLen - 1)) && !childIndicatorBits[i].getBit(suffixIdx)) {
          // use 0 instead of '$'
          addKeyToSparse((byte) 0, i + 1, true, false);
          childIndicatorBits[i].setBitToTrue(suffixIdx);
        }
        addedNewElement = false;
        continue;
      }

    }
  }


  int findSuffix(int level, byte key, boolean addedNewelement) {
    int ret = -1;
    if (loudsBits[level] == null || addedNewelement) {
      return ret;
    }

    for (int i = loudsBits[level].getSize() - 1; i >= 0; i--) {
      if (labels[level].get(i) == key) {
        ret = i;
        break;
      }
      if (loudsBits[level].getBit(i)) {
        break;
      }
    }
    return ret;
  }

  void addKeyToSparse(byte value, int level, boolean isElder, boolean hasChild) {
    if (labels[level] == null) {
      trieHeight++;
      labels[level] = new FastByteArray();
      childIndicatorBits[level] = new FastBitArray();
      loudsBits[level] = new FastBitArray();
      this.nodeCounts.add(0);
    }

    if (isElder) {
      nodeCounts.set(level, nodeCounts.get(level) + 1);
    }
    labels[level].add(value);
    childIndicatorBits[level].addBit(hasChild);
    loudsBits[level].addBit(isElder);
  }

  public void finishBuilding() {
    if (includeDense) {
      determineCutoffLevel();
      // buildDense();
    }
  }

  public void determineCutoffLevel() {
    int cutOffLevel = 0;
    long denseMem = computeDenseMem(cutOffLevel);
    long sparseMem = computeSparseMem(cutOffLevel);

    while ((cutOffLevel < getTrieHeight()) && (denseMem * this.sparseDenseRatio < sparseMem)) {
      cutOffLevel++;
      denseMem = computeDenseMem(cutOffLevel);
      sparseMem = computeSparseMem(cutOffLevel);
    }
    sparseStartLevel = cutOffLevel--;
  }

  public long computeDenseMem(int level) {
    if (level > trieHeight) {
      throw new RuntimeException("Invalid level when compute dense memory!");
    }

    long mem = 0;
    for (int i = 0; i < level; i++) {
      int numItems = labels[i].getSize();
      // TODO: Shouldn't use magic number
      mem += numItems * (256 * 2 + 1);
    }
    return mem;
  }

  public long computeSparseMem(int level) {
    if (level > trieHeight) {
      throw new RuntimeException("Invalid level when compute sparse memory!");
    }

    long mem = 0;
    for (int i = level; i < trieHeight; i++) {
      int numItems = labels[i].getSize();
      mem += numItems * (8 + 2);
    }
    return mem;
  }


  public int getTrieHeight() {
    return trieHeight;
  }

  public int getSparseStartLevel() {
    return sparseStartLevel;
  }

  public int getNodeCountsInLevel(int level) {
    return nodeCounts.get(level);
  }
}
