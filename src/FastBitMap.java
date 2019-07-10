public class FastBitMap {
    public byte[] elementData;
    private int capacity = 500;
    private int size = 0;

    public FastBitMap() {elementData = new byte[capacity * 32];}

    public FastBitMap(int len) {
        size = len;
        elementData = new byte[len * 32];
    }

    public void add(byte[] value) {
        if (size >= 500) {
            throw new RuntimeException("Not support dynamic growing. BitMap is too big(over 500)!!");
        }
        System.arraycopy(value, 0, this.elementData,  size*32, 32);
        size ++;
    }

    public byte get(int index) {return elementData[index];}

    public byte[] getNodeByte(int nodeNum) {
        byte[] ret = new byte[32];
        System.arraycopy(this.elementData, nodeNum*32, ret, 0, 32);
        return ret;
    }

    public boolean getBit(int index) {
        int idx = index/8;
        int offset = index % 8;
        return (elementData[idx] & (((byte)0x1) << (7 - offset)) ) != 0;
    }

    public int getSize() {
        return this.size;
    }
}
