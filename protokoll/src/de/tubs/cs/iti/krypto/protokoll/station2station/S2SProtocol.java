package de.tubs.cs.iti.krypto.protokoll.station2station;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;

import task3.CBC;
import task3.CoDecIDEA;
import task3.UInt16;
import task5.ChaumHash;
import task5.HashExpansion;
import de.tubs.cs.iti.jcrypt.chiffre.BigIntegerUtil;
import de.tubs.cs.iti.krypto.protokoll.Communicator;
import de.tubs.cs.iti.krypto.protokoll.Protocol;

public class S2SProtocol implements Protocol {

  private Random rnd;
  private Communicator c;
  private int other;
  private ChaumHash chaum = new ChaumHash();
  private HashExpansion hash = new HashExpansion(chaum);
  private BigInteger rsaModule;
  private BigInteger rsaPrivate;
  private BigInteger rsaPublic;

  public S2SProtocol() throws IOException {
    rnd = new Random(System.currentTimeMillis());

    BufferedReader params = new BufferedReader(new FileReader("../protokolle/Station-to-Station/hashparameters"));
    chaum.readKeys(params, 10);

    BigInteger p = BigInteger.probablePrime(512, rnd);
    BigInteger q = BigInteger.probablePrime(512, rnd);
    BigInteger N = p.multiply(q);
    BigInteger phiN = p.subtract(BigInteger.ONE).multiply(q.multiply(BigInteger.ONE));
    BigInteger e;
    do {
      e = BigIntegerUtil.randomBetween(BigInteger.valueOf(2), phiN, rnd);
    } while(!e.gcd(phiN).equals(BigInteger.ONE));

    rsaModule = N;
    rsaPrivate = e.modInverse(phiN);
    rsaPublic = e;
  }

  @Override
  public void setCommunicator(Communicator Com) {
    c = Com;
    other = 1 - c.myNumber();
  }

  @Override
  public String nameOfTheGame() {
    return "Station-To-Station";
  }

  @Override
  public void sendFirst() {
    int bitLength = 512;
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

    c.sendTo(other, p.toString(16));
    c.sendTo(other, g.toString(16));

    c.sendTo(other, rsaPublic.toString(16));
    c.sendTo(other, rsaModule.toString(16));

    BigInteger eB = new BigInteger(c.receive(), 16);
    BigInteger nB = new BigInteger(c.receive(), 16);

    BigInteger xA = BigIntegerUtil.randomBetween(BigInteger.ONE, p.subtract(BigInteger.ONE), rnd);
    BigInteger yA = g.modPow(xA, p);
    c.sendTo(other, yA.toString(16));
  }

  @Override
  public void receiveFirst() {
    BigInteger p = new BigInteger(c.receive(), 16);
    BigInteger g = new BigInteger(c.receive(), 16);

    BigInteger eA = new BigInteger(c.receive(), 16);
    BigInteger nA = new BigInteger(c.receive(), 16);

    c.sendTo(other, rsaPublic.toString(16));
    c.sendTo(other, rsaModule.toString(16));

    BigInteger yA = new BigInteger(c.receive(), 16);
    BigInteger xB = BigIntegerUtil.randomBetween(BigInteger.ONE, p.subtract(BigInteger.ONE), rnd);
    BigInteger yB = g.modPow(xB, p);
    BigInteger k = yA.modPow(xB, p);

    BigInteger sB = signature(p, yA, yB);
    BigInteger sBEnc = encrypt(sB, k);

    c.sendTo(other, sB.toString(16));
  }

  private BigInteger signature(BigInteger p, BigInteger yA, BigInteger yB) {
    BigInteger m = yB.multiply(p).add(yA);
    byte[] m_ = m.toByteArray();
    hash.concat(m_, m_.length);
    BigInteger sB = hash.read().modPow(rsaPrivate, rsaModule);
    return sB;
  }

  private BigInteger encrypt(BigInteger s, BigInteger key){
    BigInteger mask = BigInteger.ONE.shiftLeft(128).subtract(BigInteger.ONE);
    BigInteger key_ = key.and(mask);

    byte[] s_ = s.toByteArray();

    CoDecIDEA cipher = new CoDecIDEA(toUInt16s(key_.toByteArray()));
    byte[] initial = new byte[cipher.blockSize()];
    rnd.nextBytes(initial);
    CBC blockCipher = new CBC(cipher, initial);

    byte[] block = new byte[cipher.blockSize()];
    byte[] out = new byte[cipher.blockSize()];
    byte[] enc = new byte[s_.length];
    int rest = s_.length % cipher.blockSize();
    for(int i = 0; i < s_.length - rest; i += cipher.blockSize()){
      System.arraycopy(s_, i, block, 0, cipher.blockSize());
      blockCipher.encryptNextBlock(block, out);
      System.arraycopy(out, 0, enc, i, cipher.blockSize());
    }

    // Rest verarbeiten
    Arrays.fill(s_, (byte)0);
    System.arraycopy(s_, s_.length - rest, block, 0, rest);
    blockCipher.encryptNextBlock(block, out);
    System.arraycopy(out, 0, enc, s_.length - rest, rest);

    return new BigInteger(enc);
  }

  private UInt16[] toUInt16s(byte[] k_) {
    UInt16[] k = new UInt16[8];
    for(int i = 0; i < 16; i += 2){
      k[i/2] = new UInt16((k_[i]) << 8 | k_[i + 1]);
      char[] a = new char[2];
      k[i/2].copyTo(a, 0);
    }
    return k;
  }

  @Override
  public int minPlayer() {
    return 2;
  }

  @Override
  public int maxPlayer() {
    return 2;
  }

}
