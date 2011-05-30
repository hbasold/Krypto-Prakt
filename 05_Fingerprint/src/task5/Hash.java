package task5;

import java.math.BigInteger;

interface Hash {
  int inputBitLength();
  int outputBitLength();
  BigInteger hash(BigInteger in);
}