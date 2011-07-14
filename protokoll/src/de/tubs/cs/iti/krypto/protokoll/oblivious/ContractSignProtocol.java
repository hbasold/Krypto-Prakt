package de.tubs.cs.iti.krypto.protokoll.oblivious;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;

import task4.ElGamalKeys;

import de.tubs.cs.iti.jcrypt.chiffre.BigIntegerUtil;
import de.tubs.cs.iti.krypto.protokoll.Communicator;
import de.tubs.cs.iti.krypto.protokoll.Protocol;
import de.tubs.cs.iti.krypto.protokoll.util.P2PCommunicator;
import de.tubs.cs.iti.krypto.protokoll.util.PohligHellman;

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

  private String contractFile = "../text/rennen.txt";
  private String contract;

  public ContractSignProtocol() throws IOException {
    rnd = new Random(System.currentTimeMillis());
    oblivious = new ObliviousTransfer1of2Protocol(duplicateMessage );

    BufferedReader in = new BufferedReader(new FileReader(contractFile));
    String line = in.readLine();
    while(line != null){
      contract = contract + line;
      line = in.readLine();
    }
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
    ElGamalKeys elGamal = getElGamalKeys();
    ElGamalKeys elGamalB = getElGamalKeys();

    // (0)
    BigInteger pA = BigInteger.probablePrime(BITS, rnd);
    comm.send(pA);
    BigInteger puzzleS = BigIntegerUtil.randomSmallerThan(pA.divide(BigInteger.valueOf(10)), rnd);
    comm.send(puzzleS);

    // (1)
    BigInteger[][] A = generateKeys(pA);
    BigInteger[][] C = makeEncryptions(pA, A, puzzleS);

    System.out.println("A: generierte Schlüssel: \n" + Arrays.deepToString(A));
    System.out.println("A: generierte C: \n" + Arrays.deepToString(C));

    BigInteger[][] CB = new BigInteger[n][2];
    for (int i = 0; i < A.length; i++) {
      for (int j = 0; j < 2; j++) {
        comm.send(C[i][j]);
        CB[i][j] = comm.receive();
      }
    }

    // Erzeuge Vertragstext + Unterzeichnung
    String erklaerung = "Bla bla ... " + n + Arrays.deepToString(C);
    MessageDigest md;
    try {
      md = MessageDigest.getInstance("SHA");
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
      return;
    }

    md.update(erklaerung.getBytes());
    md.update(contract.getBytes());
    BigInteger hashErklaerung = new BigInteger(md.digest()).mod(elGamal.p);
    BigInteger signedErklaerung = elGamal.sign(hashErklaerung);

    comm.send(erklaerung);
    comm.send(contract);
    comm.send(signedErklaerung);

    String erklaerungB = comm.receiveString();
    String contractB = comm.receiveString();
    BigInteger signedErklaerungB = comm.receive();

    // Erklaerung prüfen
    md.reset();
    md.update(erklaerungB.getBytes());
    md.update(contractB.getBytes());
    BigInteger hashErklaerungB = new BigInteger(md.digest()).mod(elGamal.p);
    if(!elGamalB.verify(hashErklaerungB, signedErklaerungB)){
      System.err.println("Vertrag passt nicht zu Signatur.");
      comm.send(0);
      return;
    }

    comm.send(1);
    int continueProtocol = comm.receiveInt();
    if(continueProtocol == 0){
      return;
    }

    try {
      SecretSharingProtocol secretSharing = new SecretSharingProtocol(
          new SecretSharingProtocolData(36, 10, 4),
          new ObliviousTransfer1of2Protocol(false),
          A,
          false);
      BigInteger[][] AB = secretSharing.sendFirst();
      if(!checkPuzzles(pA, puzzleS, AB, CB)){
        System.err.println("Betrug: Schlüssel passen nicht zu Puzzeln.");
        return;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  @Override
  public void receiveFirst() {
    ElGamalKeys elGamal = getElGamalKeys();
    ElGamalKeys elGamalA = getElGamalKeys();

    // (0)
    BigInteger pA = comm.receive();
    BigInteger puzzleS = comm.receive();
    BigInteger pB = BigInteger.probablePrime(BITS, rnd);

    // (1)
    BigInteger[][] A = generateKeys(pB);
    BigInteger[][] C = makeEncryptions(pB, A, puzzleS);

    System.out.println("B: generierte Schlüssel: \n" + Arrays.deepToString(A));
    System.out.println("B: generierte C: \n" + Arrays.deepToString(C));

    BigInteger[][] CA = new BigInteger[n][2];
    for (int i = 0; i < A.length; i++) {
      for (int j = 0; j < 2; j++) {
        CA[i][j] = comm.receive();
        comm.send(C[i][j]);
      }
    }

    String erklaerungA = comm.receiveString();
    String contractA = comm.receiveString();
    BigInteger signedErklaerungA = comm.receive();

    // Erzeuge Vertragstext + Unterzeichnung
    String erklaerung = "Bla bla ... " + n + Arrays.deepToString(C);
    MessageDigest md;
    try {
      md = MessageDigest.getInstance("SHA");
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
      return;
    }

    md.update(erklaerung.getBytes());
    md.update(contract.getBytes());
    BigInteger hashErklaerung = new BigInteger(md.digest()).mod(elGamal.p);
    BigInteger signedErklaerung = elGamal.sign(hashErklaerung);

    comm.send(erklaerung);
    comm.send(contract);
    comm.send(signedErklaerung);

    int continueProtocol = comm.receiveInt();
    if(continueProtocol == 0){
      return;
    }

    // Erklaerung prüfen
    md.reset();
    md.update(erklaerungA.getBytes());
    md.update(contractA.getBytes());
    BigInteger hashErklaerungA = new BigInteger(md.digest()).mod(elGamal.p);
    if(!elGamalA.verify(hashErklaerungA, signedErklaerungA)){
      System.err.println("Vertrag passt nicht zu Signatur.");
      comm.send(0);
      return;
    }

    comm.send(1);

    // (2)
    try {
      SecretSharingProtocol secretSharing = new SecretSharingProtocol(
          new SecretSharingProtocolData(36, 10, 4),
          new ObliviousTransfer1of2Protocol(false),
          A,
          false);
      BigInteger[][] AA = secretSharing.receiveFirst();

      if(!checkPuzzles(pA, puzzleS, AA, CA)){
        System.err.println("Betrug: Schlüssel passen nicht zu Puzzeln.");
        return;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private boolean checkPuzzles(BigInteger pA, BigInteger puzzleS,
      BigInteger[][] aA, BigInteger[][] cA) {
    for (int i = 0; i < cA.length; i++) {
      if(!(PohligHellman.decrypt(pA, aA[i][0], cA[i][0]).equals(puzzleS) && PohligHellman.decrypt(pA, aA[i][1], cA[i][1]).equals(puzzleS))
      || (PohligHellman.decrypt(pA, aA[i][0], cA[i][1]).equals(puzzleS) && PohligHellman.decrypt(pA, aA[i][1], cA[i][0]).equals(puzzleS))){
        return false;
      }
    }
    return true;
  }

  private BigInteger[][] makeEncryptions(BigInteger p, BigInteger[][] A,
      BigInteger puzzleS) {
    BigInteger C[][] = new BigInteger[n][2];

    for (int i = 0; i < A.length; i++) {
      for (int j = 0; j < 2; j++) {
        C[i][j] = PohligHellman.encrypt(p, A[i][j], puzzleS);
      }
    }

    return C;
  }

  private BigInteger[][] generateKeys(BigInteger p) {
    BigInteger[][] A = new BigInteger[n][2];

    for(BigInteger[] APair : A){
      APair[0] = PohligHellman.generateKey(rnd, p);
      APair[1] = PohligHellman.generateKey(rnd, p);
    }

    return A;
  }

  private static ElGamalKeys getElGamalKeys() {
    ElGamalKeys elGamal = new ElGamalKeys();
    try {
      elGamal.readKeys("../protokolle/ElGamal/schluessel/key.secr", "../protokolle/ElGamal/schluessel/key.secr.public");
    } catch (FileNotFoundException e1) {
      e1.printStackTrace();
    } catch (IOException e1) {
      e1.printStackTrace();
    }
    return elGamal;
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
