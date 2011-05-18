package task3;

interface BlockCipherPure {
  public void encode(byte[] in, byte[] out);
  public void decode(byte[] in, byte[] out);
  public int blockSize(); // in bytes
}

public class CoDecIDEA implements BlockCipherPure {

  // generated key with 52=8*6+4 UInt16 (104 bytes, 832 bits)
  private UInt16[] subKey;
  private UInt16[] invKey;

  public int blockSize(){
    return 8;
  }

  /**
   * Encodes and decodes IDEA.
   * @param key The user key with 16 bytes (128 bits).
   */
  public CoDecIDEA(UInt16[] key) {
    subKey = generateSubKey(key);
    invKey = generateInverseSubKey(subKey);
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
    // subKeys[block + 1](0,6) | subKeys[block + 2](6, 15) (so we start at bit 25 (= 16 + 9 jumped over))
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
    UInt16 m1 = new UInt16(in, 0);
    UInt16 m2 = new UInt16(in, 2);
    UInt16 m3 = new UInt16(in, 4);
    UInt16 m4 = new UInt16(in, 6);

    int key = 0;
    for (int round = 0; round < 8; round++) {
      m1.mul(keys[key++]); // M1 * K_1^(round)
      m2.add(keys[key++]); // M2 + K_2^(round)
      m3.add(keys[key++]); // M3 + K_3^(round)
      m4.mul(keys[key++]); // M4 * K_4^(round)

      UInt16 tx1 = new UInt16(m1); // intermediate values
      UInt16 tx2 = new UInt16(m2);
      tx1.xor(m3);
      tx2.xor(m4);
      tx1.mul(keys[key++]);
      tx2.add(tx1);
      tx2.mul(keys[key++]);
      tx1.add(tx2);

      m1.xor(tx2);
      m2.xor(tx1);
      m3.xor(tx2);
      m4.xor(tx1);

      m2.swap(m3);
    }

    m2.swap(m3);
    m1.mul(keys[key++]);
    m2.add(keys[key++]);
    m3.add(keys[key++]);
    m4.mul(keys[key]);

    m1.copyTo(out, 0);
    m2.copyTo(out, 2);
    m3.copyTo(out, 4);
    m4.copyTo(out, 6);
  }

  private UInt16[] generateInverseSubKey(UInt16[] subKeys) {
    UInt16[] invKeys = new UInt16[52];

    int invKeyPos = 0;
    int subKeyPos = 51; // go backwards

    UInt16 k4 = subKeys[subKeyPos--].invert();
    UInt16 k3 = subKeys[subKeyPos--].negate();
    UInt16 k2 = subKeys[subKeyPos--].negate();
    UInt16 k1 = subKeys[subKeyPos--].invert();
    UInt16 k6 = new UInt16(subKeys[subKeyPos--]);
    UInt16 k5 = new UInt16(subKeys[subKeyPos--]);
    invKeys[invKeyPos++] = k1;
    invKeys[invKeyPos++] = k2;
    invKeys[invKeyPos++] = k3;
    invKeys[invKeyPos++] = k4;
    invKeys[invKeyPos++] = k5;
    invKeys[invKeyPos++] = k6;

    for(int round = 1; round < 8; ++round){
      k4 = subKeys[subKeyPos--].invert();
      k3 = subKeys[subKeyPos--].negate();
      k2 = subKeys[subKeyPos--].negate();
      k1 = subKeys[subKeyPos--].invert();
      k6 = new UInt16(subKeys[subKeyPos--]);
      k5 = new UInt16(subKeys[subKeyPos--]);
      invKeys[invKeyPos++] = k1;
      invKeys[invKeyPos++] = k3; // beware of ordering here!
      invKeys[invKeyPos++] = k2; // These two are exchanged
      invKeys[invKeyPos++] = k4;
      invKeys[invKeyPos++] = k5;
      invKeys[invKeyPos++] = k6;
    }

    k4 = subKeys[subKeyPos--].invert();
    k3 = subKeys[subKeyPos--].negate();
    k2 = subKeys[subKeyPos--].negate();
    k1 = subKeys[subKeyPos--].invert();
    invKeys[invKeyPos++] = k1;
    invKeys[invKeyPos++] = k2;
    invKeys[invKeyPos++] = k3;
    invKeys[invKeyPos++] = k4;

    return invKeys;
  }

  public void decode(byte[] in, byte[] out) {
    idea(in, out, invKey);
  }
}
