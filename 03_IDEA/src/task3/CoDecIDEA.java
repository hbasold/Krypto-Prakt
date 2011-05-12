package task3;

import java.math.BigInteger;

public class CoDecIDEA {

  // generated key with 52=8*6+4 UInt16 (104 bytes, 832 bits)
  private UInt16[] subKey = new UInt16[52];
  
  /**
   * Encodes and decodes IDEA.
   * @param key The user key with 16 bytes (128 bits).
   */
  public CoDecIDEA(BigInteger key) {
    generateSubKey(key);
  }
  
  private BigInteger rotateLeft(BigInteger a, int shift){
    return a.shiftLeft(shift).or(a.shiftRight(a.bitLength() - shift));
  }
  
  private void generateSubKey(BigInteger key) {
    // 6*8 + 4 = 52 Schlüssel generieren
    int generatedKeys = 0;
    int keyBlock = 0;
    
    while(generatedKeys < 51){
      // copy 8 blocks and in the last step 4 blocks 
      int toCopy = (keyBlock < 6) ? 8 : 4;
      
      // rotate key
      BigInteger key128Bit = rotateLeft(key, keyBlock * 25);
            
      // copy into 16 bit blocks      
      for(int i = 0; i < toCopy; ++i){
        subKey[generatedKeys + i] = new UInt16(key128Bit);
        key128Bit = key128Bit.shiftRight(16);
      }
      
      ++keyBlock;
      generatedKeys += toCopy;
    }
  }

  /**
   * Encodes 8 bytes input to 8 bytes output with the 104 byte generated key.
   * @param in The input block with 8 bytes (64 bits)
   * @param out The output block with 8 bytes (64 bits)
   */
  public void encode(byte[] in, byte[] out) {

    UInt16 x0 = new UInt16(in, 0); // M1
    UInt16 x1 = new UInt16(in, 2); // M2
    UInt16 x2 = new UInt16(in, 4); // M3
    UInt16 x3 = new UInt16(in, 6); // M4

    for (int round = 0; round < 8; round++) {
      int keyOffset = round * 4;
      x0.mul(subKey[keyOffset]);   // M1 * K1
      x1.add(subKey[keyOffset+1]); // M2 + K2
      x2.add(subKey[keyOffset+2]); // M3 + K3
      x3.mul(subKey[keyOffset+3]); // M4 * K4

      UInt16 tx1 = new UInt16(x1); // store x1 temporarily for later use
      UInt16 tx2 = new UInt16(x2); // store x2 temporarily for later use
      x2.xor(x0); // M3'' = M3' ^ M1'
      x1.xor(x3); // M2'' = M2' ^ M4'

      x2.mul(subKey[keyOffset+4]); // M3''' = M3'' * K5
      x1.add(x2); // M2''' = M2'' + M3'''
      x1.mul(subKey[keyOffset+5]); // M2''' = M2'' * K6
      x2.add(x1); // M3''' = M3'' + M2'''

      x0.xor(x1);
      x3.xor(x2);
      x1.xor(tx2);
      x2.xor(tx1);
    }
    int keyOffset = 8 * 4;
    x0.mul(subKey[keyOffset]);
    x2.add(subKey[keyOffset+1]);
    x1.add(subKey[keyOffset+2]);
    x3.mul(subKey[keyOffset+3]);
    x0.copyTo(out, 0);
    x2.copyTo(out, 2);
    x1.copyTo(out, 4);
    x3.copyTo(out, 6);
  }

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

}
