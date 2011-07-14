package de.tubs.cs.iti.krypto.protokoll.oblivious;

import java.math.BigInteger;
import java.util.Random;

import task4.ElGamalKeys;

import de.tubs.cs.iti.jcrypt.chiffre.BigIntegerUtil;
import de.tubs.cs.iti.krypto.protokoll.util.P2PCommunicator;

public class ObliviousTransfer1of2Protocol {
  private Random rnd;
  private P2PCommunicator comm;
  public boolean manipulateSignature;

  public void setComm(P2PCommunicator comm) {
    this.comm = comm;
  }


  public ObliviousTransfer1of2Protocol(boolean manipulateSignature) {
    this.manipulateSignature = manipulateSignature;
    rnd = new Random(System.currentTimeMillis());
  }

  public void obliviousTransferSend(ElGamalKeys elGamal, BigInteger[] M) {

    final BigInteger pSquare = elGamal.p.multiply(elGamal.p);

    // (0) -- ElGamal verteilen
    comm.send(elGamal.p);
    comm.send(elGamal.g);
    comm.send(elGamal.y);

    // (1) -- Hilfsnachrichten
    BigInteger[] m = {
        BigIntegerUtil.randomBetween(BigInteger.ZERO, elGamal.p),
        BigIntegerUtil.randomBetween(BigInteger.ZERO, elGamal.p)
    };
    comm.send(m[0]);
    comm.send(m[1]);

    // (2)
    BigInteger q = comm.receive();
    BigInteger k_[] = new BigInteger[2];
    BigInteger k_Sig[] = new BigInteger[2];

    // (3)
    for(int i = 0; i < 2; ++i){
      k_[i] = elGamal.decrypt(q.subtract(m[i]).mod(pSquare));
      k_Sig[i] = elGamal.sign(k_[i]);
      comm.send(k_Sig[i]);
    }

    if(manipulateSignature){
      k_Sig[1] = k_Sig[1].add(BigInteger.ONE);
    }

    int s = rnd.nextInt(2);
    BigInteger M_[] = new BigInteger[2];
    for (int i = 0; i < 2; i++) {
      M_[i] = M[i].add(k_[s ^ i]).mod(elGamal.p);
      comm.send(M_[i]);
    }
    comm.send(BigInteger.valueOf(s));
  }

  public BigInteger obliviousTransferReceive() {
    BigInteger p = comm.receive();
    BigInteger g = comm.receive();
    BigInteger yA = comm.receive();
    ElGamalKeys elGamalA = new ElGamalKeys(p, g, yA);

    final BigInteger pSquare = p.multiply(p);

    // (1)
    BigInteger[] m = {
      comm.receive(),
      comm.receive()
    };

    // (2)
    int r = rnd.nextInt(2);
    BigInteger k = BigIntegerUtil.randomBetween(BigInteger.ZERO, p);

    BigInteger q = elGamalA.encrypt(k).add(m[r]).mod(pSquare);
    comm.send(q);

    // (3)
    BigInteger k_Sig[] = {
        comm.receive(),
        comm.receive()
    };

    if(k_Sig[0].equals(k_Sig[1])){
      System.err.println("Betrug! Gleiche Signaturen.");
      return null;
    }

    BigInteger M_[] = {
        comm.receive(),
        comm.receive()
    };
    int s = comm.receive().intValue();

    // (4)
    BigInteger Mrs = M_[r ^ s].subtract(k).mod(p);
    if(!( elGamalA.verify(M_[r ^ s].subtract(Mrs).mod(p), k_Sig[0])
      || elGamalA.verify(M_[r ^ s].subtract(Mrs).mod(p), k_Sig[1]))){
      System.err.println("Betrug! Gefälschte Signaturen.");
    }

    if(elGamalA.verify(M_[r ^ 1].subtract(Mrs).mod(p), k_Sig[r^1])){
      System.err.println("Betrug! Nachricht dupliziert.");
    }
    else{
      System.out.println("Nachricht: " + new String(comm.toByteArray(Mrs)));
    }

    return Mrs;
  }
}