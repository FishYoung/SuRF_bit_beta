import java.util.Iterator;

/**
 * This is used to record the offset of the every data. Use Integer to represent
 * each offset
 */
public class FastIntArray implements Iterable<Integer> {
  private int MaxCompacity = Integer.MAX_VALUE;
  private int capacity = 100;
  private int size = 0;

  int[] elementData;

  private int lastPos = 0;

  public FastIntArray() {
    elementData = new int[capacity];
  }

  public FastIntArray(int[] value) {
    this(value, value.length);
  }

  public FastIntArray(int[] value, int length) {
    elementData = value;
    size = length;
    capacity = length;
  }

  public FastIntArray(int length) {
    elementData = new int[length];
    size = length;
  }

  public int[] getArray() {
    int[] result = new int[size];
    System.arraycopy(elementData, 0, result, 0, size);
    return result;
  }

  public void add(int value) {
    elementData = growIfNeed(elementData);
    elementData[size] = value;
    size++;
  }

  public void set(int index, short element) {
    elementData[index] = element;
  }

  public void set(int index, int element) {
    elementData[index] = element;
  }

  public int get(int index) {
    return elementData[index];
  }

  public void add(int index, int value) {
    elementData = growIfNeed(elementData);
    System.arraycopy(elementData, index, elementData, index + 1, size - index);
    elementData[index] = value;
    size++;
  }

  // must be forward
  public int findEqualOrGreater(int value, boolean isBinarySearch) {
    if (isBinarySearch) {
      lastPos = bs(lastPos, size, value);
    } else {
      lastPos = seq(value);
    }
    return lastPos;
  }

  private int seq(int value) {
    int i;
    for (i = lastPos; i < size; i++) {
      if (elementData[i] >= value) {
        break;
      }
    }
    lastPos = i;
    return lastPos;
  }

  private int bs(int l, int r, int value) {
    int mid = (l + r) / 2;
    if (mid == l) {
      if (elementData[mid] == value || (l == 0 && value < elementData[mid])) {
        return mid;
      } else {
        return mid + 1;
      }
    }
    if (elementData[mid] == value) {
      return mid;
    } else if (value < elementData[mid]) {
      return bs(l, mid, value);
    } else if (value > elementData[mid]) {
      return bs(mid, r, value);
    }
    return -1;
  }

  public void clear() {
    size = 0;
    elementData = new int[capacity];
    lastPos = 0;
  }

  public int[] growIfNeed(int[] oldArray) {
    if (size >= capacity) {
      capacity = capacity << 1;
      if (capacity >= MaxCompacity) {
        capacity = MaxCompacity;
      }
      int[] newArray = new int[capacity];
      System.arraycopy(oldArray, 0, newArray, 0, size);
      return newArray;
    }
    return oldArray;
  }

  public int size() {
    return size;
  }

  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < size; i++) {
      if (sb.length() != 0) {
        sb.append(",");
      }
      sb.append(elementData[i]);
    }
    return sb.toString();
  }

  @Override
  public Iterator<Integer> iterator() {
    return new Iterator<Integer>() {
      private int count = 0;

      public boolean hasNext() {
        return (count < size);
      }

      public Integer next() {
        return elementData[count++];
      }

      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }
}