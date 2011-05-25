package task5;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Random;

import de.tubs.cs.iti.jcrypt.chiffre.BigIntegerUtil;

public class ChaumKeys {

  public BigInteger p;
  public BigInteger g1;
  public BigInteger g2;

  public void createKeys(int bitLength) {
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
    g1 = new BigInteger(param.readLine(), 16);
    g2 = new BigInteger(param.readLine(), 16);
  }

}
