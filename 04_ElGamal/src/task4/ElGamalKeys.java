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

  private Random rnd;

  public ElGamalKeys() {
    rnd = new Random(System.currentTimeMillis());
  }

  public ElGamalKeys(BigInteger p, BigInteger g, BigInteger y) {
    rnd = new Random(System.currentTimeMillis());
    this.p = p;
    this.g = g;
    this.y = y;
  }

  public void createKeys(int bitLength) {

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

  public BigInteger encrypt(BigInteger m){
    final BigInteger upperBoundK = p.subtract(BigInteger.ONE);

    BigInteger k = BigIntegerUtil.randomBetween(BigInteger.ONE, upperBoundK, rnd);
    BigInteger a = g.modPow(k, p);                    // a =   g^k mod p
    BigInteger b = m.multiply(y.modPow(k, p)).mod(p); // b = M*y^k mod p
    return a.add(b.multiply(p));              // C'= a+b*p
  }

  public BigInteger decrypt(BigInteger c) {
    final BigInteger pMinus1MinusX = p.subtract(BigInteger.ONE).subtract(x); // p-1-x
    BigInteger a = c.mod(p);                   // a = C' mod p
    BigInteger b = c.divide(p);                // b = C' div p
    BigInteger z = a.modPow(pMinus1MinusX, p); // z = a^(p-1-x) mod p
    BigInteger m = z.multiply(b).mod(p);       // M = z*b mod p
    return m;
  }
  
  public BigInteger sign(BigInteger m){
    final BigInteger pMinus1 = p.subtract(BigInteger.ONE);
    BigInteger upperBoundK = pMinus1;
    
    BigInteger k;
    do {
      k = BigIntegerUtil.randomBetween(BigInteger.ONE, upperBoundK, rnd);
    } while(!k.gcd(pMinus1).equals(BigInteger.ONE));
    
    BigInteger r = g.modPow(k, p);     // r =   g^k mod p
    BigInteger kInv = k.modInverse(pMinus1);
    BigInteger s = m.subtract(x.multiply(r)).multiply(kInv).mod(pMinus1);
    BigInteger c = r.add(s.multiply(p));              // C'= a+b*p
    
    return c;
  }
  
  public boolean verify(BigInteger m, BigInteger c){
    final BigInteger pMinus1 = p.subtract(BigInteger.ONE);
    
    BigInteger r = c.mod(p);                   // a = C' mod p
    BigInteger s = c.divide(p);                // b = C' div p
    if(r.compareTo(BigInteger.ONE) < 0 || r.compareTo(pMinus1) > 0){
      return false;
    }
    else{
      BigInteger v1 = y.modPow(r, p).multiply(r.modPow(s, p)).mod(p);
      BigInteger v2 = g.modPow(m, p);
      if(!v1.equals(v2)){
        return false;
      }
    }
    return true;
  }
}