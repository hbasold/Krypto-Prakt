package de.tubs.cs.iti.krypto.protokoll.oblivious;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Collections;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Random;
import java.util.Vector;

import task2.Pair;
import task4.ElGamalKeys;
import de.tubs.cs.iti.jcrypt.chiffre.BigIntegerUtil;
import de.tubs.cs.iti.krypto.protokoll.Communicator;
import de.tubs.cs.iti.krypto.protokoll.Protocol;
import de.tubs.cs.iti.krypto.protokoll.util.P2PCommunicator;

@SuppressWarnings("serial")
class PairList<FIRST, SECOND> extends LinkedList<Pair<FIRST, SECOND>> {}

@SuppressWarnings("serial")
class MessagePrefixes extends PairList<Integer, BigInteger> {}

public class SecretSharing implements Protocol {

  // Basis = Anzahl der Buchstaben im Alphabet; 36 default
  private final static int BASE = 16; // 36
  
  // Anzahl der Buchstaben pro Wort (Wortlänge) über Alphabet mit BASE Zeichen; max. 10
  private final static int LETTERS = 4; // 10

  // maximale Anzahl verschiedener Nachrichten; max. 36^10
  private final static BigInteger MAX_MESSAGE_NUMBER = BigInteger.valueOf(BASE).pow(LETTERS);

  // Anzahl der Bits pro Wort in Abhängigkeit von letters; LETTERS=10 => BITS=52
  private final static int BITS = (int) Math.floor(LETTERS * Math.log(BASE) / Math.log(2));

  // Vorteil 2^k+1 zu 2^k; max. 7
  private final static int k = 3; // 7

  // max. 10 ; Anzahl Geheimnispaare (Paare von Wörtern)
  private final int n = 2;

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
    for (int i=0; i<n; i++) {
      M[i][0] = BigIntegerUtil.randomSmallerThan(MAX_MESSAGE_NUMBER, rnd);
      M[i][1] = BigIntegerUtil.randomSmallerThan(MAX_MESSAGE_NUMBER, rnd);
      System.out.println("A: Message " + i + " [0] = " + M[i][0].toString(BASE));
      System.out.println("A: Message " + i + " [1] = " + M[i][1].toString(BASE));
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
      System.out.println("A: Recv Message " + i + " = " + MB[i].toString(BASE));
    }

    Pair<Integer, Vector<Vector<MessagePrefixes>>> generateRes = generate(M, k);
    int secretIndex = generateRes.first;
    Vector<Vector<MessagePrefixes>> notSendPrefixes = generateRes.second;
    System.out.println("A: notSendPrefixes (generated)" + "\n" + notSendPrefixes);
    notSendPrefixes = mix(notSendPrefixes);
    System.out.println("A: notSendPrefixes (mixed)" + "\n" + notSendPrefixes);
    
    Vector<Vector<LinkedList<BigInteger>>> validPrefixesB = generate(n, k);

    int m = k + 1;
    while (m <= BITS) {
      System.out.println("A: prefixes of length " + m + "\n" + notSendPrefixes);
      System.out.println("A: valid prefixes B of length " + m + "\n" + validPrefixesB);
      int messageIndex = 0;
      for(Vector<MessagePrefixes> mPair : notSendPrefixes){
        int messagePairIndex = 0;
        for(MessagePrefixes mPref : mPair){
          ListIterator<Pair<Integer, BigInteger>> prefix = mPref.listIterator();
          for (int i = 0; i < 1<<k ; i++) {
            assert prefix.hasNext();
            Pair<Integer, BigInteger> p = prefix.next();
            if(p.first == secretIndex){
              p = prefix.next();
            }
            System.out.println("A: send prefix " + i + ": " + p.first);
            comm.send(p.first);
            prefix.remove();

            Integer notPrefBIndex = comm.receiveInt();
            System.out.println("A: recv prefix " + i + ": " + notPrefBIndex);
            validPrefixesB.get(messageIndex).get(messagePairIndex).remove(notPrefBIndex);
            /*
            if(isPrefix(notPrefB, MB[messageIndex])){
              System.err.println("Betrug!");
            }
            else{
              // TODO: insert
            }
            */
          }
          ++messagePairIndex;
        }
        ++messageIndex;
      }

      ++m;
      if(m <= BITS){
        generateRes = extend(M, m, notSendPrefixes);
        secretIndex = generateRes.first;
        notSendPrefixes = generateRes.second;
        notSendPrefixes = mix(notSendPrefixes);
        
        validPrefixesB = extendValid(validPrefixesB);
      }
    }
  }
  
  @Override
  public void receiveFirst() {

    BigInteger M[][] = new BigInteger[n][2];
    for (int i=0; i<n; i++) {
      M[i][0] = BigIntegerUtil.randomSmallerThan(MAX_MESSAGE_NUMBER, rnd);
      M[i][1] = BigIntegerUtil.randomSmallerThan(MAX_MESSAGE_NUMBER, rnd);
      System.out.println("B: Message " + i + " [0] = " + M[i][0].toString(BASE));
      System.out.println("B: Message " + i + " [1] = " + M[i][1].toString(BASE));
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
      System.out.println("B: Recv Message " + i + " = " + MA[i].toString(BASE));
    }

    for (BigInteger[] message : M) {
      oblivious.obliviousTransferSend(elGamal, message);
    }
    
    Pair<Integer, Vector<Vector<MessagePrefixes>>> generateRes = generate(M, k);
    int secretIndex = generateRes.first;
    Vector<Vector<MessagePrefixes>> notSendPrefixes = generateRes.second;
    System.out.println("B: notSendPrefixes (generated)" + "\n" + notSendPrefixes);
    notSendPrefixes = mix(notSendPrefixes);
    System.out.println("B: notSendPrefixes (mixed)" + "\n" + notSendPrefixes);
    
    Vector<Vector<LinkedList<BigInteger>>> validPrefixesA = generate(n, k);

    int m = k + 1;
    while (m <= BITS) {
      System.out.println("B: notSendPrefixes of length " + m + "\n" + notSendPrefixes);
      System.out.println("B: valid prefixes A of length " + m + "\n" + validPrefixesA);
      
      int messageIndex = 0;
      for(Vector<MessagePrefixes> mPair : notSendPrefixes){
        int messagePairIndex = 0;
        for(MessagePrefixes mPref : mPair){
          ListIterator<Pair<Integer, BigInteger>> prefix = mPref.listIterator();
          for (int i = 0; i < 1<<k ; i++) {
            Integer notPrefBIndex = comm.receiveInt();
            System.out.println("B: recv prefix " + i + ": " + notPrefBIndex);
            validPrefixesA.get(messageIndex).get(messagePairIndex).remove(notPrefBIndex);
            
            assert prefix.hasNext();
            Pair<Integer, BigInteger> p = prefix.next();
            if(p.first == secretIndex){
              p = prefix.next();
            }
            System.out.println("B: sending prefix " + i + ": " + p.first);
            comm.send(p.first);
            prefix.remove();
            
            /*
            if(isPrefix(notPrefB, MB[messageIndex])){
              System.err.println("Betrug!");
            }
            else{
              // TODO: insert
            }
            */
          }
          ++messagePairIndex;
        }
        ++messageIndex;
      }

      ++m;
      if(m <= BITS){
        generateRes = extend(M, m, notSendPrefixes);
        secretIndex = generateRes.first;
        notSendPrefixes = generateRes.second;
        notSendPrefixes = mix(notSendPrefixes);
        
        validPrefixesA = extendValid(validPrefixesA);
      }
      
    }

  }


  private Vector<Vector<LinkedList<BigInteger>>> extendValid(
      Vector<Vector<LinkedList<BigInteger>>> validPrefixesOther) {
    
    for(Vector<LinkedList<BigInteger>> mPair : validPrefixesOther){
      for(LinkedList<BigInteger> mPref : mPair){
        LinkedList<BigInteger> mPrefNew = new LinkedList<BigInteger>();
        for(BigInteger prefix : mPref){
          assert prefix.bitLength() <= BITS;
          mPrefNew.add(prefix.shiftLeft(1));
          mPrefNew.add(prefix.shiftLeft(1).setBit(0));
        }
        mPref.clear();
        mPref.addAll(mPrefNew);
      }
    }

    return validPrefixesOther;
  }

  private boolean isPrefix(BigInteger notPrefB, BigInteger bigInteger) {
    // TODO Auto-generated method stub
    return false;
  }

  private Pair<Integer, Vector<Vector<MessagePrefixes>>> extend(
      BigInteger[][] ms, int m, Vector<Vector<MessagePrefixes>> notSendPrefixes) {
    
    int secretIndex = 0;

    int messageIndex = 0;
    for(Vector<MessagePrefixes> mPair : notSendPrefixes){
      int messagePairIndex = 0;
      for(MessagePrefixes mPref : mPair){
        Collections.sort(mPref); // Pair hat lexikographische Ordung, Indizes sind aber immer unterschiedlich.
        
        ListIterator<Pair<Integer, BigInteger>> prefix = mPref.listIterator();
        MessagePrefixes mPrefNew = new MessagePrefixes();
        int index = 0;
        while(prefix.hasNext()){
          BigInteger b = subRange(ms[messageIndex][messagePairIndex], BITS-1, (BITS-1) - m - 1);
          Pair<Integer, BigInteger> currentNew = Pair.of(index, prefix.next().second.shiftLeft(1).setBit(0)); // Beware: tricky bit hacks!
          for(int j = 0; j < 2; ++j){
            currentNew = Pair.of(index, currentNew.second.flipBit(0));
            if(currentNew.equals(b)){
              secretIndex = index;
            }
            mPrefNew.add(currentNew);
            ++index;
          }          
        }
        mPref.clear();
        mPref.addAll(mPrefNew);
        
        assert mPref.size() == (1 << (k + 1));
        
        ++messagePairIndex;
      }
      ++messageIndex;
    }

    return Pair.of(secretIndex, notSendPrefixes);
  }

  private Vector<Vector<MessagePrefixes>> mix(
      Vector<Vector<MessagePrefixes>> notSendPrefixes) {

    for(Vector<MessagePrefixes> mPair : notSendPrefixes){
      for(MessagePrefixes mPref : mPair){
        Collections.shuffle(mPref, rnd);
      }
    }

    return notSendPrefixes;
  }

  private Pair<Integer, Vector<Vector<MessagePrefixes>>> generate(BigInteger[][] ms, int k) {
    Vector<Vector<MessagePrefixes>> validPrefixes = new Vector<Vector<MessagePrefixes>>();
    int secretIndex = 0;
    
    for(BigInteger mPair[] : ms){
      Vector<MessagePrefixes> mPairPref = new Vector<MessagePrefixes>();
      validPrefixes.add(mPairPref);
      for(BigInteger m : mPair){
        MessagePrefixes mPref = new MessagePrefixes();
        mPairPref.add(mPref);
        BigInteger b = subRange(m, BITS-1, (BITS-1) - k - 1);
        BigInteger current = BigInteger.ZERO;
        BigInteger end = BigInteger.ONE.shiftLeft(k + 1); // generiere aus [0, 2^(k+1)[
        int index = 0;
        while(current.compareTo(end) < 0){
          if(current.equals(b)){
            secretIndex = index;
          }
          mPref.add(Pair.of(Integer.valueOf(index), current));
          current = current.add(BigInteger.ONE);
          ++index;
        }
        
        assert mPref.size() == (1 << (k + 1));
      }
    }
    
    return Pair.of(secretIndex, validPrefixes);
  }
  
  private Vector<Vector<LinkedList<BigInteger>>> generate(int n, int k) {
    Vector<Vector<LinkedList<BigInteger>>> validPrefixes = new Vector<Vector<LinkedList<BigInteger>>>();
    
    LinkedList<BigInteger> prefixes = new LinkedList<BigInteger>();
    for(int i = 0; i < (1 << (k+1)); ++i){
      prefixes.add(BigInteger.valueOf(i));
    }
    
    for(int i = 0; i < n; ++i){
      Vector<LinkedList<BigInteger>> messagePair = new Vector<LinkedList<BigInteger>>();
      validPrefixes.add(messagePair);
      for(int j = 0; j < 2; ++j){
        messagePair.add(new LinkedList<BigInteger>(prefixes));
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
  public int minPlayer() {
    return 2;
  }

  @Override
  public int maxPlayer() {
    return 2;
  }

}
