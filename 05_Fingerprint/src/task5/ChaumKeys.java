package task5;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Random;

import de.tubs.cs.iti.jcrypt.chiffre.BigIntegerUtil;

interface Hash {
  int inputBitLength();
  int outputBitLength();
  BigInteger hash(byte data[]);
  BigInteger hash(BigInteger x1, BigInteger x2);
  BigInteger hash(BigInteger state);
}

public class ChaumKeys implements Hash {

  private int bitLength; // Bitlänge von q = Bitlänge von p - 1
  private BigInteger p;
  private BigInteger g1;
  private BigInteger g2;

  public void createKeys(int bitLength) {
    this.bitLength = bitLength;
    
    Random rnd = new Random(System.currentTimeMillis());
    BigInteger q, p;
    do {
      q = BigInteger.probablePrime(bitLength, rnd);
      p = q.multiply(BigInteger.valueOf(2)).add(BigInteger.ONE);
    } while(!p.isProbablePrime(100));
    
    this.p = p;
    
    BigInteger g;
    final BigInteger minus1ModP = p.subtract(BigInteger.ONE); // BigInteger.valueOf(-1).mod(p);
    do {
      g = BigIntegerUtil.randomBetween(BigInteger.valueOf(2), p.subtract(BigInteger.ONE), rnd);
    } while(!g.modPow(q, p).equals(minus1ModP));
    
    this.g1 = g;
    
    do {
      g = BigIntegerUtil.randomBetween(BigInteger.valueOf(2), p.subtract(BigInteger.ONE), rnd);
    } while(!g.modPow(q, p).equals(minus1ModP));
    
    this.g2 = g;
    
    System.out.println("q=" + q + ", p= " + p + ", g1=" + g1 + ", g2=" + g2);
  }

  public void writeKeys(BufferedWriter param) throws IOException {
    param.write(p.toString(16)); param.newLine();
    param.write(g1.toString(16)); param.newLine();
    param.write(g2.toString(16)); param.newLine();    
  }

  public void readKeys(BufferedReader param) throws IOException {
    p = new BigInteger(param.readLine(), 16);
    bitLength = p.bitLength() - 1;
    System.out.println("bits=" + bitLength + ", p=" + p);
    g1 = new BigInteger(param.readLine(), 16);
    g2 = new BigInteger(param.readLine(), 16);
  }
  
  public BigInteger hash(byte data[]){
    assert data.length == inputBitLength() / 8;
    
    byte x1_[] = new byte[data.length / 2];
    System.arraycopy(data, 0, x1_, 0, data.length / 2);
    BigInteger x1 = new BigInteger(1, x1_);
    
    byte x2_[] = new byte[data.length / 2];
    System.arraycopy(data, data.length / 2, x2_, 0, data.length / 2);
    BigInteger x2 = new BigInteger(1, x2_);
    
    return hash(x1, x2);
  }
  
  public BigInteger hash(BigInteger x1, BigInteger x2){
    assert x1.bitLength() <= bitLength && x2.bitLength() <= bitLength;
    return g1.modPow(x1, p).multiply(g2.modPow(x2, p)).mod(p);
  }

  public int inputBitLength() {
    return 2 * bitLength;
  }

  public int outputBitLength() {
    return bitLength + 1;
  }

  public BigInteger hash(BigInteger in) {
    assert in.bitLength() <= inputBitLength();
    
    BigInteger lowerMask = BigInteger.ONE.shiftLeft(bitLength - 1).subtract(BigInteger.ONE);
    
    BigInteger x1 = in.and(lowerMask);
    BigInteger x2 = in.shiftRight(bitLength);
    return hash(x1, x2);
  }

}
