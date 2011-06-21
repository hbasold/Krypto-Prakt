package de.tubs.cs.iti.krypto.protokoll.station2station;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;

import task3.CBC;
import task3.CoDecIDEA;
import task3.UInt16;
import task5.ChaumHash;
import task5.HashExpansion;
import de.tubs.cs.iti.jcrypt.chiffre.BigIntegerUtil;
import de.tubs.cs.iti.krypto.protokoll.Certificate;
import de.tubs.cs.iti.krypto.protokoll.Communicator;
import de.tubs.cs.iti.krypto.protokoll.Protocol;
import de.tubs.cs.iti.krypto.protokoll.TrustedAuthority;

public class S2SProtocol implements Protocol {

  private Random rnd;
  private Communicator c;
  private int other;
  private ChaumHash chaum = new ChaumHash();
  private HashExpansion hash = new HashExpansion(chaum);
  private BigInteger rsaModule;
  private BigInteger rsaPrivate;
  private BigInteger rsaPublic;
  private Certificate cert;
  private MessageDigest md;

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

    BigInteger certData = e.multiply(N).add(N);
    cert = TrustedAuthority.newCertificate(certData.toByteArray());

    try {
      md = MessageDigest.getInstance("SHA");
    } catch (NoSuchAlgorithmException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }
  }

  @Override
  public void setCommunicator(Communicator Com) {
    c = Com;
    System.out.println("myNumber = " + c.myNumber());
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

    send(p);
    send(g);

    send(rsaPublic);
    send(rsaModule);

    BigInteger eB = receive();
    BigInteger nB = receive();

    BigInteger xA = BigIntegerUtil.randomBetween(BigInteger.ONE, p.subtract(BigInteger.ONE), rnd);
    BigInteger yA = g.modPow(xA, p);
    send(yA);

    // (4)
    Certificate certB = receiveCertificate();
    BigInteger certSig = certB.getSignature().modPow(TrustedAuthority.getPublicExponent(), TrustedAuthority.getModulus());
    md.reset();
    md.update(certB.getID().getBytes());
    md.update(eB.multiply(nB).add(nB).toByteArray());
    if(!Arrays.equals(certSig.toByteArray(), md.digest())){
      System.err.println("Zertifikat von B ungültig");
      return;
    }
    BigInteger yB = receive();
    BigInteger initialB = receive();
    BigInteger sBEnc = receive();

    BigInteger k = yB.modPow(xA, p);
    BigInteger sB = decrypt(sBEnc, k, nB.bitLength() / 8, initialB.toByteArray());
    System.out.println("sB = " + sB.toString(16));
    if(!verifySignature(sB, yB, yA, p, eB, nB)){
      System.err.println("Signatur von B ungültig");
      return;
    }

    // (5)
    BigInteger sA = signature(p, yA, yB);

    // (6)
    BigInteger mask = BigInteger.ONE.shiftLeft(128).subtract(BigInteger.ONE);
    BigInteger key_ = k.and(mask);
    CoDecIDEA cipher = new CoDecIDEA(toUInt16s(key_.toByteArray()));
    byte[] initial = new byte[cipher.blockSize()];
    rnd.nextBytes(initial);
    BigInteger sAEnc = encrypt(sA, k, cipher, initial);

    sendCertificate(cert);
    // send(yA); unnötig?
    send(new BigInteger(1, initial));
    send(sAEnc);
  }

  private boolean verifySignature(BigInteger sig, BigInteger yOther, BigInteger ySelf, BigInteger p, BigInteger eOther, BigInteger nOther) {
    final BigInteger hashedKey = hashKey(p, yOther, ySelf);
    System.out.println("sig verif hash = " + hashedKey.toString(16));
    return sig.modPow(eOther, nOther).equals(hashedKey);
  }

  @Override
  public void receiveFirst() {
    BigInteger p = receive();
    BigInteger g = receive();

    BigInteger eA = receive();
    BigInteger nA = receive();

    send(rsaPublic);
    send(rsaModule);

    BigInteger yA = receive();
    BigInteger xB = BigIntegerUtil.randomBetween(BigInteger.ONE, p.subtract(BigInteger.ONE), rnd);
    BigInteger yB = g.modPow(xB, p);
    BigInteger k = yA.modPow(xB, p);

    BigInteger sB = signature(p, yB, yA);
    System.out.println("sB = " + sB.toString(16));

    BigInteger mask = BigInteger.ONE.shiftLeft(128).subtract(BigInteger.ONE);
    BigInteger key_ = k.and(mask);
    CoDecIDEA cipher = new CoDecIDEA(toUInt16s(key_.toByteArray()));
    byte[] initial = new byte[cipher.blockSize()];
    rnd.nextBytes(initial);
    BigInteger sBEnc = encrypt(sB, k, cipher, initial);

    sendCertificate(cert);
    send(yB);
    send(new BigInteger(1, initial));
    send(sBEnc);

    // (7)
    Certificate certA = receiveCertificate();
    BigInteger certSig = certA.getSignature().modPow(TrustedAuthority.getPublicExponent(), TrustedAuthority.getModulus());
    md.reset();
    md.update(certA.getID().getBytes());
    md.update(eA.multiply(nA).add(nA).toByteArray());
    if(!Arrays.equals(certSig.toByteArray(), md.digest())){
      System.err.println("Zertifikat von A ungültig");
      return;
    }
    //BigInteger yA = receive();
    BigInteger initialA = receive();
    BigInteger sAEnc = receive();

    BigInteger sA = decrypt(sAEnc, k, nA.bitLength() / 8, initialA.toByteArray());
    if(!verifySignature(sA, yA, yB, p, eA, nA)){
      System.err.println("Signatur von A ungültig");
      return;
    }
  }

  private void send(BigInteger p) {
    System.out.println("Sending " + p.toString(16));
    c.sendTo(other, p.toString(16));
  }

  private BigInteger receive() {
    BigInteger p = new BigInteger(c.receive(), 16);
    System.out.println("Received " + p.toString(16));
    return p;
  }

  private void sendCertificate(Certificate cert) {
    c.sendTo(other, cert.getID());
    send(new BigInteger(1, cert.getData()));
    send(cert.getSignature());
  }

  private Certificate receiveCertificate(){
    String id = c.receive();
    BigInteger data = receive();
    BigInteger signature = receive();

    return new Certificate(id, data.toByteArray(), signature);
  }

  private BigInteger signature(BigInteger p, BigInteger ySelf, BigInteger yOther) {
    final BigInteger hashedKey = hashKey(p, ySelf, yOther);
    System.out.println("sig create hash = " + hashedKey.toString(16));
    BigInteger sB = hashedKey.modPow(rsaPrivate, rsaModule);
    return sB;
  }

  private BigInteger hashKey(BigInteger p, BigInteger ySelf, BigInteger yOther) {
    hash.reset();
    BigInteger m = yOther.multiply(p).add(ySelf);
    byte[] m_ = m.toByteArray();
    int hashBlockSize = hash.inputBitLength() / 8;
    byte[] block = new byte[hashBlockSize];
    for(int i = 0; i < m_.length; i += hashBlockSize){
      int currentBlockSize = Math.min(hashBlockSize, m_.length - i);
      System.arraycopy(m_, i, block, 0, currentBlockSize);
      hash.concat(block, currentBlockSize);
    }
    final BigInteger hashedKey = hash.read();
    return hashedKey;
  }

  private BigInteger encrypt(BigInteger s, BigInteger key, CoDecIDEA cipher, byte[] initial){
    byte[] s_ = s.toByteArray();

    CBC blockCipher = new CBC(cipher, initial);

    byte[] block = new byte[cipher.blockSize()];
    byte[] out = new byte[cipher.blockSize()];
    byte[] enc = new byte[(((s_.length + (cipher.blockSize() - 1)) / cipher.blockSize())) * cipher.blockSize()];
    int rest = s_.length % cipher.blockSize();
    for(int i = 0; i < s_.length - rest; i += cipher.blockSize()){
      System.arraycopy(s_, i, block, 0, cipher.blockSize());
      blockCipher.encryptNextBlock(block, out);
      System.arraycopy(out, 0, enc, i, cipher.blockSize());
    }

    // Rest verarbeiten
    if(rest > 0){
      Arrays.fill(block, (byte)0);
      System.arraycopy(s_, s_.length - rest, block, 0, rest);
      blockCipher.encryptNextBlock(block, out);
      System.arraycopy(out, 0, enc, s_.length - rest, cipher.blockSize());
    }

    return new BigInteger(1, enc);
  }

  private BigInteger decrypt(BigInteger sBEnc, BigInteger key, int neededBytes, byte[] initial) {
    BigInteger mask = BigInteger.ONE.shiftLeft(128).subtract(BigInteger.ONE);
    BigInteger key_ = key.and(mask);

    byte[] s_ = sBEnc.toByteArray();

    CoDecIDEA cipher = new CoDecIDEA(toUInt16s(key_.toByteArray()));
    CBC blockCipher = new CBC(cipher, initial);

    byte[] block = new byte[cipher.blockSize()];
    byte[] out = new byte[cipher.blockSize()];
    byte[] dec = new byte[s_.length];
    for(int i = 0; i < s_.length; i += cipher.blockSize()){
      System.arraycopy(s_, i, block, 0, cipher.blockSize());
      blockCipher.decryptNextBlock(block, out);
      System.arraycopy(out, 0, dec, i, cipher.blockSize());
    }

    // Oen entfernen
    byte[] dec_ = new byte[neededBytes];
    System.arraycopy(dec, 0, dec_, 0, neededBytes);

    return new BigInteger(1, dec_);
  }

  private UInt16[] toUInt16s(byte[] k_) {
    UInt16[] k = new UInt16[8];
    for(int i = 0; i < 16; i += 2){
      int upper = (char) k_[i]      & 0xFF;
      int lower = (char) k_[i + 1]  & 0xFF;
      k[i/2] = new UInt16((upper << 8) | lower);
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
