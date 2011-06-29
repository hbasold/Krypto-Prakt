package de.tubs.cs.iti.krypto.protokoll.station2station;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;

import task4.ElGamalKeys;
import de.tubs.cs.iti.jcrypt.chiffre.BigIntegerUtil;
import de.tubs.cs.iti.krypto.protokoll.Certificate;
import de.tubs.cs.iti.krypto.protokoll.Communicator;
import de.tubs.cs.iti.krypto.protokoll.Protocol;
import de.tubs.cs.iti.krypto.protokoll.TrustedAuthority;

public class ObliviousTransfer1of2 implements Protocol {

  private Random rnd;
  private Communicator c;
  private int other;

  private boolean isOskar = true;

  public ObliviousTransfer1of2() throws IOException {
    rnd = new Random(System.currentTimeMillis());

  }

  @Override
  public void setCommunicator(Communicator Com) {
    c = Com;
    other = 1 - c.myNumber();
  }

  @Override
  public String nameOfTheGame() {
    return "1-of-2-Oblivious-Transfer";
  }

  @Override
  public void sendFirst() {

    // (0) -- ElGamal initialisieren und verteilen
    ElGamalKeys elGamal = new ElGamalKeys();
    elGamal.createKeys(512);
    send(elGamal.p);
    send(elGamal.g);
    send(elGamal.y);

    final BigInteger pSquare = elGamal.p.multiply(elGamal.p);

    BufferedReader standardInput = new BufferedReader(new InputStreamReader(System.in));
    String message;
    try {
      BigInteger M[] = new BigInteger[2];
      do{
        System.out.print("Nachricht 1:");
        message = standardInput.readLine();
        M[0] = new BigInteger(1, message.getBytes());
      } while(M[0].compareTo(elGamal.p) >= 0);

      if(isOskar){
        M[1] = new BigInteger(1, message.getBytes());
      }
      else{
        do{
          System.out.print("Nachricht 2:");
          message = standardInput.readLine();
          M[1] = new BigInteger(1, message.getBytes());
        } while(M[1].compareTo(elGamal.p) >= 0);
      }

      // (1) -- Hilfsnachrichten
      BigInteger[] m = {
          BigIntegerUtil.randomBetween(BigInteger.ZERO, elGamal.p),
          BigIntegerUtil.randomBetween(BigInteger.ZERO, elGamal.p)
      };
      send(m[0]);
      send(m[1]);

      // (2)
      BigInteger q = receive();
      BigInteger k_[] = new BigInteger[2];
      BigInteger k_Sig[] = new BigInteger[2];

      // (3)
      for(int i = 0; i < 2; ++i){
        k_[i] = elGamal.decrypt(q.subtract(m[i]).mod(pSquare));
        k_Sig[i] = elGamal.sign(k_[i]);
        send(k_Sig[i]);
      }

      int s = rnd.nextInt(2);
      BigInteger M_[] = new BigInteger[2];
      for (int i = 0; i < 2; i++) {
        M_[i] = M[i].add(k_[s ^ i]).mod(elGamal.p);
        send(M_[i]);
      }
      send(BigInteger.valueOf(s));

    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Override
  public void receiveFirst() {
    BigInteger p = receive();
    BigInteger g = receive();
    BigInteger yA = receive();
    ElGamalKeys elGamalA = new ElGamalKeys(p, g, yA);

    final BigInteger pSquare = p.multiply(p);

    // (1)
    BigInteger[] m = {
      receive(),
      receive()
    };

    // (2)
    int r = rnd.nextInt(2);
    BigInteger k = BigIntegerUtil.randomBetween(BigInteger.ZERO, p);

    BigInteger q = elGamalA.encrypt(k).add(m[r]).mod(pSquare);
    send(q);

    // (3)
    BigInteger k_Sig[] = {
        receive(),
        receive()
    };
    
    if(k_Sig[0].equals(k_Sig[1])){
      System.err.println("Betrug! Gleiche Signaturen.");
      return;
    }

    BigInteger M_[] = {
        receive(),
        receive()
    };
    int s = receive().intValue();

    // (4)
    BigInteger Mrs = M_[r ^ s].subtract(k).mod(p);
    //assert elGamalA.verify(M_[r ^ s].subtract(Mrs).mod(p), k_Sig[r^s]);

    if(elGamalA.verify(M_[r ^ 1].subtract(Mrs).mod(p), k_Sig[r^1])){
      System.err.println("Betrug! Nachricht dupliziert.");
    }
    else{
      System.out.println("Nachricht: " + new String(toByteArray(Mrs)));
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

  private BigInteger hashForTA(String id, byte[] certData) throws NoSuchAlgorithmException {
    MessageDigest md = MessageDigest.getInstance("SHA");
    md.update(id.getBytes());
    md.update(certData);
    BigInteger certBExpectedSig = new BigInteger(md.digest()).mod(TrustedAuthority.getModulus());
    return certBExpectedSig;
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
