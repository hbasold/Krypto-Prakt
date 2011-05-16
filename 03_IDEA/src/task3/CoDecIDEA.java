package task3;

import java.util.Arrays;

public class CoDecIDEA {

  // generated key with 52=8*6+4 UInt16 (104 bytes, 832 bits)
  private UInt16[] subKey;
  private UInt16[] invKey;

  /**
   * Encodes and decodes IDEA.
   * @param key The user key with 16 bytes (128 bits).
   */
  public CoDecIDEA(UInt16[] key) {
    subKey = generateSubKey(key);
    invKey = generateInverseSubKey(subKey);
    System.out.println(Arrays.toString(invKey));
  }

  private UInt16[] generateSubKey(UInt16[] key) {
    UInt16[] subKeys = new UInt16[52];

    // copy first 8 blocks
    for (int i = 0; i < 8; i++){
      subKeys[i] = key[i];
    }

    int block = 0; // index to the first of the blocks we are shifting by 25 bits
    int j = 8; // current calculated sub key
    // We are copying from
    // block + 1 | block + 2 (so we start at bit 25 (= 16 + 9 jumped over))
    // block + 2 | block + 3
    // ....
    // block + 7 | block + 0
    // block + 0 | block + 1 (and finish there again (25 = 9 + 7 + 9 bits used))
    // Each time 7 bits of the upper and 9 bits of the lower block.
    // This performs the circular left shift of 25.
    for(int i = 1; j < 52; j++ ) {
      /*
      System.out.println("block=" + block);
      System.out.println("i=" + i);
      System.out.println("i % 8=" + (i%8));
      System.out.println("index1=" + (block + (i%8)));
      */

      int upper = (subKeys[block + (i % 8)].getValue() & 0x7f) << 9; // -> 7 bits
      int lower = subKeys[block + ((i + 1) % 8)].getValue() >>> 7; // -> 9 Bits
      subKeys[j] = new UInt16(upper | lower);

      if(i == 8){
        block += 8;
        i = 0; // will be 1 after increment
      }

      i++;
    }

    // check with reference implementation from http://packetstormsecurity.org/files/view/20397/idea-algorithm.txt
    int[] ukey = new int[8];
    for(int i = 0; i < 8; ++i) {
      ukey[i] = key[i].getValue();
    }

    int[] k = new int[52];
    int i;
    for (i=0; i<8; i++) k[i]=ukey[i];
    for (i=8; i<52; i++) {
      if ((i & 7) < 6)
        k[i]=((k[i-7] & 127) << 9 | k[i-6] >>> 7);
      else if ((i & 7) == 6)
        k[i]=((k[i-7] & 127) << 9 | k[i-14] >>> 7);
      else
        k[i]=((k[i-15] & 127) << 9 | k[i-14] >>> 7);
    }

    //assert Arrays.equals(subKeys, k);

    System.out.println(Arrays.toString(subKeys));
    System.out.println(Arrays.toString(k));

    return subKeys;
  }

  /**
   * Encodes 8 bytes input to 8 bytes output with the 104 byte generated key.
   * @param in The input block with 8 bytes (64 bits)
   * @param out The output block with 8 bytes (64 bits)
   */
  public void encode(byte[] in, byte[] out) {
    idea(in, out, subKey);
  }

  private void idea(byte[] in, byte[] out, UInt16[] keys) {
    UInt16 x0 = new UInt16(in, 0); // M1
    UInt16 x1 = new UInt16(in, 2); // M2
    UInt16 x2 = new UInt16(in, 4); // M3
    UInt16 x3 = new UInt16(in, 6); // M4

    for (int round = 0; round < 8; round++) {
      int keyOffset = round * 4;
      x0.mul(keys[keyOffset]);   // M1 * K1
      x1.add(keys[keyOffset+1]); // M2 + K2
      x2.add(keys[keyOffset+2]); // M3 + K3
      x3.mul(keys[keyOffset+3]); // M4 * K4

      UInt16 tx1 = new UInt16(x1); // store x1 temporarily for later use
      UInt16 tx2 = new UInt16(x2); // store x2 temporarily for later use
      x2.xor(x0); // M3'' = M3' ^ M1'
      x1.xor(x3); // M2'' = M2' ^ M4'

      x2.mul(keys[keyOffset+4]); // M3''' = M3'' * K5
      x1.add(x2); // M2''' = M2'' + M3'''
      x1.mul(keys[keyOffset+5]); // M2''' = M2'' * K6
      x2.add(x1); // M3''' = M3'' + M2'''

      x0.xor(x1);
      x3.xor(x2);
      x1.xor(tx2);
      x2.xor(tx1);
    }
    int keyOffset = 8 * 4;
    x0.mul(keys[keyOffset]);
    x2.add(keys[keyOffset+1]);
    x1.add(keys[keyOffset+2]);
    x3.mul(keys[keyOffset+3]);
    x0.copyTo(out, 0);
    x2.copyTo(out, 2);
    x1.copyTo(out, 4);
    x3.copyTo(out, 6);
  }

  private UInt16[] generateInverseSubKey(UInt16[] subKeys) {
    UInt16[] invKeys = new UInt16[52];

    // Round 1:
    invKeys[0] = subKeys[51].invert();
    invKeys[1] = subKeys[50].negate();
    invKeys[2] = subKeys[49].negate();
    invKeys[3] = subKeys[48].invert();
    for(int round = 0; round < 8; ++round){
      int pos = round * 6;
      invKeys[pos] = subKeys[51 - pos].invert();
      ++pos;
      invKeys[pos] = subKeys[51 - pos].negate();
      ++pos;
      invKeys[pos] = subKeys[51 - pos].negate();
      ++pos;
      invKeys[pos] = subKeys[51 - pos].invert();
      ++pos;
      invKeys[pos] = new UInt16(subKeys[51 - pos]);
      ++pos;
      invKeys[pos] = new UInt16(subKeys[51 - pos]);
    }

    int pos = 8 * 6;
    invKeys[pos] = subKeys[51 - pos].invert();
    ++pos;
    invKeys[pos] = subKeys[51 - pos].negate();
    ++pos;
    invKeys[pos] = subKeys[51 - pos].negate();
    ++pos;
    invKeys[pos] = subKeys[51 - pos].invert();

    return invKeys;
  }

  public void decode(byte[] in, byte[] out) {
    idea(in, out, invKey);
  }

  /*
  public static void main(String[] args) {
    byte[] a = {(byte)0x12, (byte)0x34};
    UInt16 u = new UInt16(a, 0);
    System.out.println(u.getValue());
    System.out.println("i="+Integer.toString(u.getValue(), 16));
    BigInteger i = new BigInteger(a);
    System.out.println("BI="+i.toString(16));
    i = i.shiftLeft(8);
    System.out.println("BI << 8="+i.toString(16));
    i = i.and(new BigInteger(
        new byte[] {0, 0, (byte)0xFF, (byte)0xFF }));
    System.out.println("BI << 8 &0xFFFF="+i.toString(16));
  }
  */

}
