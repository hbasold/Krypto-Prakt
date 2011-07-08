package de.tubs.cs.iti.krypto.protokoll.oblivious;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Random;
import java.util.Vector;

import task4.ElGamalKeys;
import de.tubs.cs.iti.jcrypt.chiffre.BigIntegerUtil;
import de.tubs.cs.iti.krypto.protokoll.Communicator;
import de.tubs.cs.iti.krypto.protokoll.Protocol;
import de.tubs.cs.iti.krypto.protokoll.util.P2PCommunicator;

public class SecretSharing implements Protocol {

  private int n = 2; // max. 10 ; Anzahl Wörter
  private int k = 3; // max. 7  : Vorteil 2^k+1 zu 2^k

  private Random rnd = new Random(System.currentTimeMillis());

  private ObliviousTransfer1of2Protocol oblivious = new ObliviousTransfer1of2Protocol(true);
  private P2PCommunicator comm;

  public SecretSharing() throws IOException {
  }

  @Override
  public void setCommunicator(Communicator com) {
    this.comm = new P2PCommunicator(com);
    oblivious.setComm(comm);
  }

  @Override
  public String nameOfTheGame() {
    return "SecretSharing";
  }

  @Override
  public void sendFirst() {

//    BufferedReader standardInput = new BufferedReader(new InputStreamReader(System.in));

    BigInteger M[][] = new BigInteger[n][2];
    BigInteger MAX_MESSAGE_NUMBER = new BigInteger("zzzzzzzzzz", 36);
    for (int i=0; i<n; i++) {
      M[i][0] = BigIntegerUtil.randomSmallerThan(MAX_MESSAGE_NUMBER, rnd);
      M[i][1] = BigIntegerUtil.randomSmallerThan(MAX_MESSAGE_NUMBER, rnd);
    }

    // (0) -- ElGamal initialisieren
    ElGamalKeys elGamal = new ElGamalKeys();
    try {
      elGamal.readKeys("../protokolle/ElGamal/schluessel/key.secr", "../protokolle/ElGamal/schluessel/key.secr.public");
    } catch (FileNotFoundException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    } catch (IOException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }

    for (BigInteger[] message : M) {
      oblivious.obliviousTransferSend(elGamal, message);
    }

    BigInteger MB[] = new BigInteger[n];
    for (int i = 0; i < MB.length; i++) {
      MB[i] = oblivious.obliviousTransferReceive();
    }

    Vector<Vector<LinkedList<BigInteger>>> notSendPrefixes = generate(M, k);
    notSendPrefixes = mix(notSendPrefixes);

    int m = 51 - k;
    while (m >= 0) {

      int messageIndex = 0;
      for(Vector<LinkedList<BigInteger>> mPair : notSendPrefixes){
        for(LinkedList<BigInteger> mPref : mPair){
          ListIterator<BigInteger> prefix = mPref.listIterator();
          for (int i = 0; i < 1<<k ; i++) {
            assert prefix.hasNext();
            comm.send(prefix.next());
            prefix.remove();

            BigInteger notPrefB = comm.receive();
            if(isPrefix(notPrefB, MB[messageIndex])){
              System.err.println("Betrug!");
            }
            else{
              // TODO: insert
            }
          }
        }
        ++messageIndex;
      }

      notSendPrefixes = extend(notSendPrefixes);
      notSendPrefixes = mix(notSendPrefixes);
      --m;
    }
  }



  private boolean isPrefix(BigInteger notPrefB, BigInteger bigInteger) {
    // TODO Auto-generated method stub
    return false;
  }

  private Vector<Vector<LinkedList<BigInteger>>> extend(
      Vector<Vector<LinkedList<BigInteger>>> prefixes) {

    for(Vector<LinkedList<BigInteger>> mPair : prefixes){
      for(LinkedList<BigInteger> mPref : mPair){
        ListIterator<BigInteger> prefix = mPref.listIterator(mPref.size() - 1);
        while(prefix.hasPrevious()){
          BigInteger current = prefix.previous();
          mPref.add(current.shiftLeft(1));
          mPref.add(current.shiftLeft(1).setBit(0));
          prefix.remove();
        }
      }
    }

    return prefixes;
  }

  private Vector<Vector<LinkedList<BigInteger>>> mix(
      Vector<Vector<LinkedList<BigInteger>>> prefixes) {

    for(Vector<LinkedList<BigInteger>> mPair : prefixes){
      for(LinkedList<BigInteger> mPref : mPair){
        ListIterator<BigInteger> prefix = mPref.listIterator();
        while(prefix.hasNext()){
          BigInteger current = prefix.next();
          int destIndex = rnd.nextInt(mPref.size());
          ListIterator<BigInteger> dest = mPref.listIterator(destIndex);
          assert dest.hasNext();
          BigInteger target = dest.next();
          dest.set(current);
          prefix.set(target);
        }
      }
    }

    return prefixes;

  }

  private Vector<Vector<LinkedList<BigInteger>>> generate(BigInteger[][] ms, int k) {
    Vector<Vector<LinkedList<BigInteger>>> validPrefixes = new Vector<Vector<LinkedList<BigInteger>>>();

    for(BigInteger mPair[] : ms){
      Vector<LinkedList<BigInteger>> mPairPref = new Vector<LinkedList<BigInteger>>();
      validPrefixes.add(mPairPref);
      for(BigInteger m : mPair){
        LinkedList<BigInteger> mPref = new LinkedList<BigInteger>();
        mPairPref.add(mPref);
        BigInteger b = subRange(m, 51, 51 - k - 1);
        BigInteger current = BigInteger.ZERO;
        BigInteger end = BigInteger.ONE.shiftLeft(k + 1); // generiere aus [0, 2^(k+1)[
        while(current.compareTo(end) < 0){
          if(!current.equals(b)){
            mPref.add(current);
          }
          current = current.add(BigInteger.ONE);
        }
      }
    }

      return validPrefixes;
  }

  /**
   *
   * @param b
   * @param begin left / high bit position
   * @param end   right / low bit position
   * @return
   */
  private BigInteger subRange(BigInteger b, int begin, int end) {
    assert begin>=end;
    return b.shiftRight(end).and(BigInteger.ONE.shiftLeft(begin-end).subtract(BigInteger.ONE));
  }

  @Override
  public void receiveFirst() {

    BigInteger M[][] = new BigInteger[n][2];
    BigInteger MAX_MESSAGE_NUMBER = new BigInteger("zzzzzzzzzz", 36);
    for (int i=0; i<n; i++) {
      M[i][0] = BigIntegerUtil.randomSmallerThan(MAX_MESSAGE_NUMBER, rnd);
      M[i][1] = BigIntegerUtil.randomSmallerThan(MAX_MESSAGE_NUMBER, rnd);
    }

    // (0) -- ElGamal initialisieren
    ElGamalKeys elGamal = new ElGamalKeys();
    try {
      elGamal.readKeys("../protokolle/ElGamal/schluessel/key.secr", "../protokolle/ElGamal/schluessel/key.secr.public");
    } catch (FileNotFoundException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    } catch (IOException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }

    BigInteger MA[] = new BigInteger[n];
    for (int i = 0; i < MA.length; i++) {
      MA[i] = oblivious.obliviousTransferReceive();
    }

    for (BigInteger[] message : M) {
      oblivious.obliviousTransferSend(elGamal, message);
    }

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
