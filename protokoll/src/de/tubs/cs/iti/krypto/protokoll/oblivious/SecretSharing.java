package de.tubs.cs.iti.krypto.protokoll.oblivious;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Comparator;
import java.util.TreeMap;
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
  private final int n = 4;

  private Random rnd = new Random(System.currentTimeMillis());

  private ObliviousTransfer1of2Protocol oblivious;
  private P2PCommunicator comm;

  private boolean duplicateMessage = false;
  private boolean replaceOneSecret = true;

  public SecretSharing() throws IOException {
    oblivious = new ObliviousTransfer1of2Protocol(duplicateMessage);
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
      if(duplicateMessage ){
        M[i][1] = M[i][0];
      }
      else{
        M[i][1] = BigIntegerUtil.randomSmallerThan(MAX_MESSAGE_NUMBER, rnd);
      }

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

    if(replaceOneSecret){
      for (BigInteger[] message : M) {
        // "geratener" Index der erhaltenen Nachricht = 0
        message[1] = BigIntegerUtil.randomSmallerThan(MAX_MESSAGE_NUMBER, rnd);
      }
    }

    Pair<Vector<Vector<Integer>>, Vector<Vector<MessagePrefixes>>> generateRes = generate(M, k);
    Vector<Vector<Integer>> secretIndices = generateRes.first;
    Vector<Vector<MessagePrefixes>> notSendPrefixes = generateRes.second;
    notSendPrefixes = mix(notSendPrefixes);

    Vector<Vector<TreeMap<Integer, BigInteger>>> validPrefixesB = generate(n, k);

    int m = k + 1;
    // in der BITS+1en Runde werden die übrigen nicht-gültigen Präfixe verschickt/empfangen.
    while (m <= BITS + 1) {
      System.out.println("A: prefixes of length " + m + "\n" + notSendPrefixes);
      System.out.println("A: valid prefixes B of length " + m + "\n" + validPrefixesB);
      int messageIndex = 0;
      for(Vector<MessagePrefixes> mPair : notSendPrefixes){
        int messagePairIndex = 0;
        for(MessagePrefixes mPref : mPair){
          ListIterator<Pair<Integer, BigInteger>> prefix = mPref.listIterator();
          int upperBound = (m == BITS+1) ? ((1<<k)-1) : (1<<k);
          for (int i = 0; i < upperBound; i++) {
            assert prefix.hasNext();
            Pair<Integer, BigInteger> p = prefix.next();
            if(p.first == secretIndices.get(messageIndex).get(messagePairIndex)){
              p = prefix.next();
            }
            comm.send(p.first);
            prefix.remove();

            Integer notPrefBIndex = comm.receiveInt();
            if(!validPrefixesB.get(messageIndex).get(messagePairIndex).containsKey(notPrefBIndex)){
              System.err.println("Betrug: Index mehrfach erhalten!");
            }
            validPrefixesB.get(messageIndex).get(messagePairIndex).remove(notPrefBIndex);
          }
          assert (m == BITS+1) ? true : prefixExists(subRange(M[messageIndex][messagePairIndex], BITS-1, (BITS-1) - m + 1), mPref);
          ++messagePairIndex;
        }
        ++messageIndex;
      }

      ++m;
      if(m <= BITS){
        generateRes = extend(M, m, notSendPrefixes);
        secretIndices = generateRes.first;
        notSendPrefixes = generateRes.second;
        notSendPrefixes = mix(notSendPrefixes);

        validPrefixesB = extendValid(validPrefixesB);
      }
    }

    for( Vector<TreeMap<Integer, BigInteger>> mPair  : validPrefixesB){
      for( TreeMap<Integer, BigInteger> message : mPair){
        assert message.size() == 1;
        System.out.println("A: Nachricht von B:" + message.firstEntry().getValue().toString(BASE));
      }
    }

    checkMessages(MB, validPrefixesB);
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

    Pair<Vector<Vector<Integer>>, Vector<Vector<MessagePrefixes>>> generateRes = generate(M, k);
    Vector<Vector<Integer>> secretIndices = generateRes.first;
    Vector<Vector<MessagePrefixes>> notSendPrefixes = generateRes.second;
    notSendPrefixes = mix(notSendPrefixes);

    Vector<Vector<TreeMap<Integer, BigInteger>>> validPrefixesA = generate(n, k);

    int m = k + 1;
    // in der BITS+1en Runde werden die übrigen nicht-gültigen Präfixe verschickt/empfangen.
    while (m <= BITS + 1) {
      System.out.println("B: notSendPrefixes of length " + m + "\n" + notSendPrefixes);
      System.out.println("B: valid prefixes A of length " + m + "\n" + validPrefixesA);

      int messageIndex = 0;
      for(Vector<MessagePrefixes> mPair : notSendPrefixes){
        int messagePairIndex = 0;
        for(MessagePrefixes mPref : mPair){
          ListIterator<Pair<Integer, BigInteger>> prefix = mPref.listIterator();
          int upperBound = (m == BITS+1) ? ((1<<k)-1) : (1<<k);
          for (int i = 0; i < upperBound; i++) {
            Integer notPrefBIndex = comm.receiveInt();
            if(!validPrefixesA.get(messageIndex).get(messagePairIndex).containsKey(notPrefBIndex)){
              System.err.println("Betrug: Index mehrfach erhalten!");
            }
            validPrefixesA.get(messageIndex).get(messagePairIndex).remove(notPrefBIndex);

            assert prefix.hasNext();
            Pair<Integer, BigInteger> p = prefix.next();
            if(p.first == secretIndices.get(messageIndex).get(messagePairIndex)){
              p = prefix.next();
            }
            comm.send(p.first);
            prefix.remove();
          }
          assert (m == BITS+1) ? true : prefixExists(subRange(M[messageIndex][messagePairIndex], BITS-1, (BITS-1) - m + 1), mPref);
          ++messagePairIndex;
        }
        ++messageIndex;
      }

      ++m;
      if(m <= BITS){
        generateRes = extend(M, m, notSendPrefixes);
        secretIndices = generateRes.first;
        notSendPrefixes = generateRes.second;
        notSendPrefixes = mix(notSendPrefixes);

        validPrefixesA = extendValid(validPrefixesA);
      }

    }

    for( Vector<TreeMap<Integer, BigInteger>> mPair  : validPrefixesA){
      for( TreeMap<Integer, BigInteger> message : mPair){
        assert message.size() == 1;
        System.out.println("B: Nachricht von A:" + message.firstEntry().getValue().toString(BASE));
      }
    }

    checkMessages(MA, validPrefixesA);

  }

  private void checkMessages(BigInteger[] MB,
      Vector<Vector<TreeMap<Integer, BigInteger>>> validPrefixesB) {
    int messageIndex = 0;
    for( Vector<TreeMap<Integer, BigInteger>> mPair  : validPrefixesB){
      if(!messageExists(MB[messageIndex], mPair)){
        System.err.println("Betrug: ein Geheimnis wurde gefälscht!");
      }
      for( TreeMap<Integer, BigInteger> message : mPair){
        assert message.size() == 1;
      }
      if(mPair.get(0).firstEntry().getValue().equals(mPair.get(1).firstEntry().getValue())){
        System.err.println("Betrug: zwei gleiche Nachrichten erhalten!");
      }
      ++messageIndex;
    }

  }

  private boolean messageExists(BigInteger mB,
      Vector<TreeMap<Integer, BigInteger>> mPair) {
    boolean exists = false;
    for( TreeMap<Integer, BigInteger> message : mPair){
      assert message.size() == 1;
      if(message.firstEntry().getValue().equals(mB)){
        exists = true;
      }
    }
    return exists;
  }

  private boolean prefixExists(BigInteger subRange, MessagePrefixes mPref) {
    boolean exists = false;
    for(Pair<Integer, BigInteger> p : mPref){
      if(p.second.equals(subRange)){
        exists = true;
      }
    }
    return exists;
  }

  private Vector<Vector<TreeMap<Integer, BigInteger>>> extendValid(
      Vector<Vector<TreeMap<Integer, BigInteger>>> validPrefixesB) {

    for(Vector<TreeMap<Integer, BigInteger>> mPair : validPrefixesB){
      for(TreeMap<Integer, BigInteger> mPref : mPair){
        TreeMap<Integer, BigInteger> mPrefNew = new TreeMap<Integer, BigInteger>();
        int index = 0;
        for(BigInteger prefix : mPref.values()){
          assert prefix.bitLength() <= BITS;
          mPrefNew.put(index, prefix.shiftLeft(1));
          index++;
          mPrefNew.put(index, prefix.shiftLeft(1).setBit(0));
          index++;
        }
        mPref.clear();
        mPref.putAll(mPrefNew);
      }
    }

    return validPrefixesB;
  }

  private class PairAscComp<FIRST, SECOND> implements Comparator<Pair<FIRST, SECOND>>{

    @Override
    public int compare(Pair<FIRST, SECOND> o1, Pair<FIRST, SECOND> o2) {
      return -(o1.compareTo(o2));
    }

  }

  private Pair<Vector<Vector<Integer>>, Vector<Vector<MessagePrefixes>>> extend(
      BigInteger[][] ms, int m, Vector<Vector<MessagePrefixes>> notSendPrefixes) {

    Vector<Vector<Integer>> secretIndices = new Vector<Vector<Integer>>();

    int messageIndex = 0;
    for(Vector<MessagePrefixes> mPair : notSendPrefixes){
      Vector<Integer> mPairSecrIndices = new Vector<Integer>();
      secretIndices.add(mPairSecrIndices);

      int messagePairIndex = 0;

      for(MessagePrefixes mPref : mPair){
        Collections.sort(mPref, new PairAscComp<Integer, BigInteger>()); // Pair hat lexikographische Ordung, Indizes sind aber immer unterschiedlich.

        ListIterator<Pair<Integer, BigInteger>> prefix = mPref.listIterator();
        MessagePrefixes mPrefNew = new MessagePrefixes();
        int index = 0;
        while(prefix.hasNext()){
          BigInteger b = subRange(ms[messageIndex][messagePairIndex], BITS-1, (BITS-1) - m + 1);
          Pair<Integer, BigInteger> currentNew = Pair.of(index, prefix.next().second.shiftLeft(1).setBit(0)); // Beware: tricky bit hacks!
          for(int j = 0; j < 2; ++j){
            currentNew = Pair.of(index, currentNew.second.flipBit(0));
            if(currentNew.second.equals(b)){
              mPairSecrIndices.add(index);
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

      assert mPairSecrIndices.size() == 2;

      ++messageIndex;
    }

    return Pair.of(secretIndices, notSendPrefixes);
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

  private Pair<Vector<Vector<Integer>>, Vector<Vector<MessagePrefixes>>> generate(BigInteger[][] ms, int k) {
    Vector<Vector<MessagePrefixes>> validPrefixes = new Vector<Vector<MessagePrefixes>>();
    Vector<Vector<Integer>> secretIndices = new Vector<Vector<Integer>>();

    for(BigInteger mPair[] : ms){
      Vector<MessagePrefixes> mPairPref = new Vector<MessagePrefixes>();
      validPrefixes.add(mPairPref);
      Vector<Integer> mPairSecrIndices = new Vector<Integer>();
      secretIndices.add(mPairSecrIndices);
      for(BigInteger m : mPair){
        MessagePrefixes mPref = new MessagePrefixes();
        mPairPref.add(mPref);
        BigInteger b = subRange(m, BITS-1, (BITS-1) - k);
        System.out.println("secret prefix: " + b);
        BigInteger current = BigInteger.ZERO;
        BigInteger end = BigInteger.ONE.shiftLeft(k + 1); // generiere aus [0, 2^(k+1)[
        int index = 0;
        while(current.compareTo(end) < 0){
          if(current.equals(b)){
            mPairSecrIndices.add(index);
          }
          mPref.add(Pair.of(Integer.valueOf(index), current));
          current = current.add(BigInteger.ONE);
          ++index;
        }

        assert mPref.size() == (1 << (k + 1));
      }

      assert mPairSecrIndices.size() == 2;
    }

    return Pair.of(secretIndices, validPrefixes);
  }

  private Vector<Vector<TreeMap<Integer, BigInteger>>> generate(int n, int k) {
    Vector<Vector<TreeMap<Integer, BigInteger>>> validPrefixes = new Vector<Vector<TreeMap<Integer,BigInteger>>>();

    TreeMap<Integer, BigInteger> prefixes = new TreeMap<Integer, BigInteger>();
    for(int i = 0; i < (1 << (k+1)); ++i){
      prefixes.put(i, BigInteger.valueOf(i));
    }

    for(int i = 0; i < n; ++i){
      Vector<TreeMap<Integer, BigInteger>> messagePair = new Vector<TreeMap<Integer,BigInteger>>();
      validPrefixes.add(messagePair);
      for(int j = 0; j < 2; ++j){
        messagePair.add(new TreeMap<Integer, BigInteger>(prefixes));
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
    return b.shiftRight(end).and(BigInteger.ONE.shiftLeft(begin - end + 1).subtract(BigInteger.ONE));
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
