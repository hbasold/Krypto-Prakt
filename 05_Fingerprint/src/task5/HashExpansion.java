package task5;

import java.math.BigInteger;

public class HashExpansion {

  private Hash hash;
  private int paddedBytes;
  private BigInteger state;

  public HashExpansion(Hash hash) {
    this.hash = hash;
    paddedBytes = 0;
    state = null;
  }

  public void concat(byte data[], int length){
    assert length <= inputBitLength() / 8;

    byte in_[] = new byte[inputBitLength() / 8];
    System.arraycopy(data, 0, in_, 0, length);

    int toPad = (inputBitLength() / 8) - length;
    assert toPad == 0 || paddedBytes == 0 : "Padding only allowed at last block!";
    paddedBytes = toPad;
    for(int i = length; i < length + toPad; ++i){
      in_[i] = 0;
    }

    assert state == null || state.bitLength() <= hash.outputBitLength();

    if(state == null){
      state = BigInteger.ZERO;
    }
    else{
      state = state.shiftLeft(1).setBit(0);
    }

    assert state.equals(BigInteger.ZERO) || state.bitLength() <= hash.outputBitLength() + 1;

    BigInteger in = new BigInteger(1, in_);
    state = state.shiftLeft(inputBitLength()).or(in);
    state = hash.hash(state);
  }

  public int inputBitLength(){
    return hash.inputBitLength() - hash.outputBitLength() - 1;
  }
  
  public int outputBitLength(){
    return hash.outputBitLength();
  }

  public BigInteger read(){
    BigInteger in = BigInteger.valueOf(paddedBytes * 8);
    return hash.hash(state.shiftLeft(inputBitLength()).or(in));
  }

}
