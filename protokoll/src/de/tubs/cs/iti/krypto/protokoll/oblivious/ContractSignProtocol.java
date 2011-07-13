package de.tubs.cs.iti.krypto.protokoll.oblivious;

import java.math.BigInteger;
import java.util.Random;

import de.tubs.cs.iti.jcrypt.chiffre.BigIntegerUtil;
import de.tubs.cs.iti.krypto.protokoll.Communicator;
import de.tubs.cs.iti.krypto.protokoll.Protocol;
import de.tubs.cs.iti.krypto.protokoll.util.P2PCommunicator;

public class ContractSignProtocol implements Protocol {
  
  // Basis = Anzahl der Buchstaben im Alphabet; 36 default
  private final static int BASE = 16; // 36

  // Anzahl der Buchstaben pro Wort (Wortlänge) über Alphabet mit BASE Zeichen; max. 10
  private final static int LETTERS = 4; // 10

  // maximale Anzahl verschiedener Nachrichten; max. 36^10
  private final static BigInteger MAX_MESSAGE_NUMBER = BigInteger.valueOf(BASE).pow(LETTERS);

  // Anzahl der Bits pro Wort in Abhängigkeit von letters; LETTERS=10 => BITS=52
  //private final static int BITS = (int) Math.floor(LETTERS * Math.log(BASE) / Math.log(2));
  private final static int BITS = 52;

  // Vorteil 2^k+1 zu 2^k; max. 7
  private final static int k = 3; // 7

  // max. 10 ; Anzahl Geheimnispaare (Paare von Wörtern)
  private final int n = 4;

  private P2PCommunicator comm;
  private ObliviousTransfer1of2Protocol oblivious;
   
  private boolean duplicateMessage = false;

  private Random rnd;
  
  public ContractSignProtocol() {
    rnd = new Random(System.currentTimeMillis());
    oblivious = new ObliviousTransfer1of2Protocol(duplicateMessage );
  }

  @Override
  public void setCommunicator(Communicator com) {
    this.comm = new P2PCommunicator(com);
    oblivious.setComm(comm);
  }

  @Override
  public String nameOfTheGame() {
    return "Vertragsunterzeichnung";
  }

  @Override
  public void sendFirst() {
    // (0)
    BigInteger pA = BigInteger.probablePrime(BITS, rnd);
    comm.send(pA);
    BigInteger puzzleS = BigIntegerUtil.randomSmallerThan(pA.divide(BigInteger.valueOf(10)), rnd);
    comm.send(puzzleS);

  }

  @Override
  public void receiveFirst() {
    // (0)
    BigInteger pA = comm.receive();
    BigInteger puzzleS = comm.receive();
    BigInteger pB = BigInteger.probablePrime(BITS, rnd);
    

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
