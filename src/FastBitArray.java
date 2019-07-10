public class FastBitArray {
  private int capacity = 5000;
  int size = 0;

  byte[] elementData;
  int[] fastRank;
  int[] fastSelect;

  int currentPos = 0;
  private int offset = 0;

  public FastBitArray() {
    elementData = new byte[100000];
  }

  public FastBitArray(int fixedSize){
    elementData = new byte[(fixedSize + 7)/8];
    size = 128;
  }

  public void generateRankAndSelect() {
    fastRank = new int[size];
    fastSelect = new int[size];
    int cnt = 0;
    for (int i = 0; i < size; i++) {
      if (getBit(i)) {
        cnt++;
        fastSelect[cnt] = i;
      }
      fastRank[i] = cnt;
    }
  }

  public void addBit(boolean value) {
    if (value) {
      elementData[currentPos] = (byte) (elementData[currentPos] | (byte) (1 << (7 - offset)));
    }
    offset++;

    if (offset >= 8) {
      offset = 0;
      currentPos++;
    }
    size++;
  }

  public void addBit(int value) {
    if (value == 1)
      addBit(true);
    else if (value == 0)
      addBit(false);
    else {
      //non-zero means '1'?
      addBit(true);
    }
  }

  public boolean getBit(int index) {
    int idx = index / 8;
    int offset = index % 8;
    return (elementData[idx] & (((byte) 0x1) << (7 - offset))) != 0;
  }

  public void setBitToTrue(int index) {
    int idx = index / 8;
    int offset = index % 8;
    elementData[idx] = (byte) (elementData[idx] | (byte) (1 << (7 - offset)));
  }

  public int getSize() {
    return size;
  }

  public int getCapacity() {
    return capacity;
  }

  public boolean isEmpty() {
    if (size == 0)
      return true;
    else
      return false;
  }


  public int rank1(int pos) {
    return fastRank[pos];
  }

  public int select1(int pos) {
    return fastSelect[pos];
  }

}
