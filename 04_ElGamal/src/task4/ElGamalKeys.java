package task4;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Random;

import de.tubs.cs.iti.jcrypt.chiffre.BigIntegerUtil;

public class ElGamalKeys {
  public BigInteger p;
  public BigInteger g;
  public BigInteger x;
  public BigInteger y;

  public ElGamalKeys() {
  }

  public void createKeys(int bitLength) {
    Random rnd = new Random(System.currentTimeMillis());
    BigInteger q, p;
    do {
      q = BigInteger.probablePrime(bitLength, rnd);
      p = q.multiply(BigInteger.valueOf(2)).add(BigInteger.ONE);
    } while(!p.isProbablePrime(100));
    
    BigInteger g;
    BigInteger minus1ModP = p.subtract(BigInteger.ONE); // BigInteger.valueOf(-1).mod(p);
    do {
      g = BigIntegerUtil.randomBetween(BigInteger.valueOf(2), p.subtract(BigInteger.ONE), rnd);
    } while(!g.modPow(q, p).equals(minus1ModP));
    
    System.out.println("q=" + q + ", p= " + p + ", g=" + g);
    
    this.p = p;
    this.g = g;
    x = BigIntegerUtil.randomBetween(BigInteger.ONE, p.subtract(BigInteger.ONE), rnd);
    y = g.modPow(x, p);
  }

  public void readKeys(String privateKeyFile, String publicKeyFile) throws FileNotFoundException, IOException {
      BufferedReader in = new BufferedReader(new FileReader(privateKeyFile));
      p = new BigInteger(in.readLine(), 16);
      g = new BigInteger(in.readLine(), 16);
      x = new BigInteger(in.readLine(), 16);
      
      in = new BufferedReader(new FileReader(publicKeyFile));
      BigInteger p_ = new BigInteger(in.readLine(), 16);
      BigInteger g_ = new BigInteger(in.readLine(), 16);
      y = new BigInteger(in.readLine(), 16);
      
      assert p.equals(p_);
      assert g.equals(g_);
    }

  public void writeKeys(String privateKeyFile, String publicKeyFile) throws IOException {
    BufferedWriter out = new BufferedWriter(new FileWriter(privateKeyFile));
    out.write(p.toString(16)); out.newLine();
    out.write(g.toString(16)); out.newLine();
    out.write(x.toString(16)); out.newLine();
    out.close();      
    
    out = new BufferedWriter(new FileWriter(publicKeyFile));
    out.write(p.toString(16)); out.newLine();
    out.write(g.toString(16)); out.newLine();
    out.write(y.toString(16)); out.newLine();
    out.close();
  }
}