package de.tubs.cs.iti.krypto.protokoll.station2station;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

  public S2SProtocol() throws IOException {
    rnd = new Random(System.currentTimeMillis());

    BufferedReader params = new BufferedReader(new FileReader("../protokolle/Station-to-Station/hashparameters"));
    chaum.readKeys(params, 10);

    BigInteger p = BigInteger.probablePrime(512, rnd);
    BigInteger q = BigInteger.probablePrime(512, rnd);
    BigInteger N = p.multiply(q);
    BigInteger phiN = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));
    BigInteger e;
    do {
      e = BigIntegerUtil.randomBetween(BigInteger.valueOf(2), phiN, rnd);
    } while(!e.gcd(phiN).equals(BigInteger.ONE));

    rsaModule = N;
    rsaPrivate = e.modInverse(phiN);
    rsaPublic = e;

    assert rsaPrivate.multiply(rsaPublic).mod(phiN).equals(BigInteger.ONE);
    assert BigInteger.valueOf(5).modPow(rsaPublic, rsaModule).modPow(rsaPrivate, rsaModule).equals(BigInteger.valueOf(5));

    BigInteger certData = e.multiply(N).add(N);
    cert = TrustedAuthority.newCertificate(toByteArray(certData));
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

    byte[] certBData = toByteArray(eB.multiply(nB).add(nB));
    if(!Arrays.equals(certBData, certB.getData())){
      System.err.println("Zertifikat von B passt nicht zu übertragenem Schlüssel");
    }
    BigInteger certBExpectedSig;
    try {
      certBExpectedSig = hashForTA(certB.getID(), certBData);
    } catch (NoSuchAlgorithmException e) {
      System.out.println("Could not create message digest! Exception "
          + e.toString());
      return;
    }
    if(!certSig.equals(certBExpectedSig)){
      System.err.println("Zertifikat von B ungültig");
      return;
    }

    BigInteger yB = receive();
    byte[] initialB = receiveBytes();
    BigInteger sBEnc = receive();

    BigInteger k = yB.modPow(xA, p);
    BigInteger sB = decrypt(sBEnc, k, (nB.bitLength() + 7) / 8, initialB);
    //System.out.println("sB = " + sB.toString(16));
    if(!verifySignature(sB, yB, yA, p, eB, nB)){
      System.err.println("Signatur von B ungültig");
      return;
    }

    // (5)
    BigInteger sA = signature(p, yA, yB);
    System.out.println("sA = " + sA.toString(16));

    // (6)
    BigInteger mask = BigInteger.ONE.shiftLeft(128).subtract(BigInteger.ONE);
    BigInteger key_ = k.and(mask);
    CoDecIDEA cipher = new CoDecIDEA(toUInt16s(toByteArray(key_)));
    byte[] initial = new byte[cipher.blockSize()];
    rnd.nextBytes(initial);
    BigInteger sAEnc = encrypt(sA, k, cipher, initial);

    sendCertificate(cert);
    // send(yA); unnötig?
    sendBytes(initial);
    send(sAEnc);

    BufferedReader standardInput = new BufferedReader(new InputStreamReader(System.in));
    String message;
    try {
      System.out.print(">");
      message = standardInput.readLine();
      while(!message.equals("quit")){
        rnd.nextBytes(initial);
        byte[] encMessage = encrypt(cipher, initial, message.getBytes());
        c.sendTo(other, Integer.toHexString(message.length()));
        sendBytes(initial);
        sendBytes(encMessage);

        int length = Integer.parseInt(c.receive(), 16);
        if(length <= 0){
          System.err.println("Ungültige Textlänge");
          break;
        }
        initialB = receiveBytes();
        encMessage = receiveBytes();
        byte[] m = decrypt(k, length, initialB, encMessage);
        System.out.println("B: " + new String(m));

        message = standardInput.readLine();
      }
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private BigInteger hashForTA(String id, byte[] certData) throws NoSuchAlgorithmException {
    MessageDigest md = MessageDigest.getInstance("SHA");
    md.update(id.getBytes());
    md.update(certData);
    BigInteger certBExpectedSig = new BigInteger(md.digest()).mod(TrustedAuthority.getModulus());
    return certBExpectedSig;
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
    //System.out.println("sB = " + sB.toString(16));

    BigInteger mask = BigInteger.ONE.shiftLeft(128).subtract(BigInteger.ONE);
    BigInteger key_ = k.and(mask);
    CoDecIDEA cipher = new CoDecIDEA(toUInt16s(toByteArray(key_)));
    byte[] initial = new byte[cipher.blockSize()];
    rnd.nextBytes(initial);
    BigInteger sBEnc = encrypt(sB, k, cipher, initial);

    sendCertificate(cert);
    send(yB);
    sendBytes(initial);
    send(sBEnc);

    // (7)
    Certificate certA = receiveCertificate();
    BigInteger certSig = certA.getSignature().modPow(TrustedAuthority.getPublicExponent(), TrustedAuthority.getModulus());
    byte[] certAData = toByteArray(eA.multiply(nA).add(nA));
    if(!Arrays.equals(certAData, certA.getData())){
      System.err.println("Zertifikat von A passt nicht zu übertragenem Schlüssel");
    }
    BigInteger certBExpectedSig;
    try {
      certBExpectedSig = hashForTA(certA.getID(), certAData);
    } catch (NoSuchAlgorithmException e) {
      System.out.println("Could not create message digest! Exception "
          + e.toString());
      return;
    }
    if(!certSig.equals(certBExpectedSig)){
      System.err.println("Zertifikat von A ungültig");
      return;
    }
    //BigInteger yA = receive();
    byte[] initialA = receiveBytes();
    BigInteger sAEnc = receive();

    BigInteger sA = decrypt(sAEnc, k, (nA.bitLength() + 7) / 8, initialA);
    //System.out.println("sA = " + sA.toString(16));
    if(!verifySignature(sA, yA, yB, p, eA, nA)){
      System.err.println("Signatur von A ungültig"); //*
      return;
    }

    BufferedReader standardInput = new BufferedReader(new InputStreamReader(System.in));
    String message;
    try {

      while(true){
        int length = Integer.parseInt(c.receive(), 16);
        if(length <= 0){
          System.err.println("Ungültige Textlänge");
          break;
        }
        initialA = receiveBytes();
        byte[] encMessage = receiveBytes();
        byte[] m = decrypt(k, length, initialA, encMessage);
        System.out.println("A: " + new String(m));

        message = standardInput.readLine();
        if(message.equals("quit")){
          break;
        }
        rnd.nextBytes(initial);
        encMessage = encrypt(cipher, initial, message.getBytes());
        c.sendTo(other, Integer.toHexString(message.length()));
        sendBytes(initial);
        sendBytes(encMessage);
      }
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private void sendBytes(byte[] data) {
    send(new BigInteger(1, data));
  }

  private byte[] receiveBytes() {
    BigInteger initialB_ = receive();
    return toByteArray(initialB_);
  }

  /**
   * Returns /a/ as byte array eliminating the leading byte if its is 0.
   *
   * This is needed, as it contains the sign bit which is unused here.
   *
   * @param a
   * @return
   */
  private byte[] toByteArray(BigInteger a) {
    byte[] initialB = a.toByteArray();
    // Führendes 0-Byte entfernen
    if(initialB[0] == 0){
      return Arrays.copyOfRange(initialB, 1, initialB.length);
    }
    else{
      return initialB;
    }
  }

  private void send(BigInteger p) {
    //System.out.println("Sending " + p.toString(16));
    c.sendTo(other, p.toString(16));
  }

  private BigInteger receive() {
    BigInteger p = new BigInteger(c.receive(), 16);
    //System.out.println("Received " + p.toString(16));
    return p;
  }

  private void sendCertificate(Certificate cert) {
    c.sendTo(other, cert.getID());
    sendBytes(cert.getData());
    send(cert.getSignature());
  }

  private Certificate receiveCertificate(){
    String id = c.receive();
    byte[] data = receiveBytes();
    BigInteger signature = receive();

    return new Certificate(id, data, signature);
  }

  private BigInteger signature(BigInteger p, BigInteger ySelf, BigInteger yOther) {
    BigInteger sB = signature(p, ySelf, yOther, rsaModule, rsaPrivate);
    //assert sB.modPow(rsaPublic, rsaModule).equals(hashedKey);
    return sB;
  }

  private BigInteger signature(BigInteger p, BigInteger ySelf, BigInteger yOther,
      BigInteger N, BigInteger d) {
    final BigInteger hashedKey = hashKey(p, ySelf, yOther);
    //System.out.println("sig create hash = " + hashedKey.toString(16));
    BigInteger sB = hashedKey.modPow(d, N);
    return sB;
  }

  private boolean verifySignature(BigInteger sig, BigInteger yOther, BigInteger ySelf, BigInteger p, BigInteger eOther, BigInteger nOther) {
    final BigInteger hashedKey = hashKey(p, yOther, ySelf);
    //System.out.println("sig verif hash = " + hashedKey.toString(16));
    BigInteger expectedHash = sig.modPow(eOther, nOther);
    return expectedHash.equals(hashedKey);
  }


  private BigInteger hashKey(BigInteger p, BigInteger ySelf, BigInteger yOther) {
    hash.reset();
    BigInteger m = yOther.multiply(p).add(ySelf);
    byte[] m_ = toByteArray(m);
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
    byte[] s_ = toByteArray(s);

    byte[] enc = encrypt(cipher, initial, s_);

    return new BigInteger(1, enc);
  }

  private byte[] encrypt(CoDecIDEA cipher, byte[] initial, byte[] s_) {
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
    return enc;
  }

  private BigInteger decrypt(BigInteger sBEnc, BigInteger key, int neededBytes, byte[] initial) {
    byte[] s_ = toByteArray(sBEnc);

    byte[] dec_ = decrypt(key, neededBytes, initial, s_);

    return new BigInteger(1, dec_);
  }

  private byte[] decrypt(BigInteger key, int neededBytes, byte[] initial,
      byte[] s_) {
    BigInteger mask = BigInteger.ONE.shiftLeft(128).subtract(BigInteger.ONE);
    BigInteger key_ = key.and(mask);

    CoDecIDEA cipher = new CoDecIDEA(toUInt16s(toByteArray(key_)));
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
    return dec_;
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
