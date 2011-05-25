/**
 *
 */
package task3;

import java.util.Arrays;

/**
 * Implementierung von CBC auf Basis einer Blockchiffre.
 *
 */
public class CBC {

  private BlockCipherPure cipher;
  private byte[] state;
  private byte[] t;

  CBC(BlockCipherPure cipher_, byte[] initial){
    assert initial.length >= cipher_.blockSize();

    cipher = cipher_;
    state = Arrays.copyOf(initial, cipher.blockSize());
    t = new byte[cipher.blockSize()];
  }

  public void encryptNextBlock(byte[] in, byte[] out){
    xor(in, state, t);
    cipher.encode(t, out);
    System.arraycopy(out, 0, state, 0, cipher.blockSize());
  }

  public void decryptNextBlock(byte[] in, byte[] out){
    cipher.decode(in, t);
    xor(t, state, out);
    System.arraycopy(in, 0, state, 0, cipher.blockSize());
  }

  private void xor(byte[] lhs, byte[] rhs, byte[] out) {
    for(int i = 0; i < cipher.blockSize(); ++i){
      out[i] = (byte) (lhs[i] ^ rhs[i]);
    }
  }

}
