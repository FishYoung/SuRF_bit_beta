import java.util.Iterator;

/**
 * This is used to record the offset of the every data. Use Integer to represent
 * each offset
 */
public class FastByteArray implements Iterable<Byte> {
    private int MaxCompacity = Integer.MAX_VALUE;
    private int capacity = 500;
    private int size = 0;

    byte[] elementData;

    private int lastPos = 0;

    public FastByteArray() {
        elementData = new byte[capacity];
    }

    public FastByteArray(byte[] value) {
        this(value, value.length);
    }

    public FastByteArray(byte[] value, int length) {
        elementData = value;
        size = length;
        capacity = length;
    }

    public FastByteArray(int length) {
        elementData = new byte[length];
        size = length;
    }

    public int[] getArray() {
        int[] result = new int[size];
        System.arraycopy(elementData, 0, result, 0, size);
        return result;
    }

    public void add(byte value) {
        elementData = growIfNeed(elementData);
        elementData[size] = value;
        size++;
    }

//    public void set(int index, short element) {
//        elementData[index] = element;
//    }

    public byte get(int index) {
        return elementData[index];
    }

    public void add(int index, byte value) {
        elementData = growIfNeed(elementData);
        System.arraycopy(elementData, index, elementData, index + 1, size - index);
        elementData[index] = value;
        size++;
    }

    public int getSize() {
        return size;
    }

    public boolean isEmpty() {
        if (size == 0)
            return true;
        else
            return false;
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
        elementData = new byte[capacity];
        lastPos = 0;
    }

    public byte[] growIfNeed(byte[] oldArray) {
        if (size >= capacity) {
            capacity = capacity << 1;
            if (capacity >= MaxCompacity) {
                capacity = MaxCompacity;
            }
            byte[] newArray = new byte[capacity];
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
    public Iterator<Byte> iterator() {
        return new Iterator<Byte>() {
            private int count = 0;

            public boolean hasNext() {
                return (count < size);
            }

            public Byte next() {
                return elementData[count++];
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
