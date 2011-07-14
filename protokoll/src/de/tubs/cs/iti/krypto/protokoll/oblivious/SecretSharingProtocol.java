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
import de.tubs.cs.iti.krypto.protokoll.util.P2PCommunicator;

public class SecretSharingProtocol {

  private final static Random rnd = new Random(System.currentTimeMillis());

  private final SecretSharingProtocolData data;
  private final ObliviousTransfer1of2Protocol oblivious;
  private final BigInteger[][] M;
  private final boolean replaceOneSecret;
  private final int n;

  private P2PCommunicator comm;

  private class PairList<FIRST, SECOND> extends LinkedList<Pair<FIRST, SECOND>> {
    private static final long serialVersionUID = -7295097574442135571L;
  }

  private class MessagePrefixes extends PairList<Integer, BigInteger> {
    private static final long serialVersionUID = -5372582791038032155L;
  }

  /**
   * @param data The data used for the protocol.
   * @param oblivious The oblivious transfer protocol
   * @param M An array of paired messages to transmit. Use
   *          {@link SecretSharingProtocol#getRandomMessages()} to generate random messages.
   * @param replaceOneSecret True, if Oskar replaces one message of each pair with different contend.
   * @throws IOException Thrown by oblivious transfer.
   */
  public SecretSharingProtocol(
        SecretSharingProtocolData data,
        ObliviousTransfer1of2Protocol oblivious,
        BigInteger[][] M,
        boolean replaceOneSecret) throws IOException {
    this.data = data;
    this.oblivious = oblivious; // new ObliviousTransfer1of2Protocol(manipulateObliviousSignature)
    this.M = M;
    this.replaceOneSecret = replaceOneSecret;
    n = M.length;
  }

  public void setCommunicator(Communicator com) {
    this.comm = new P2PCommunicator(com);
    oblivious.setComm(comm);
  }

  /**
   * Generate random pairs of messages.
   * @param n The number of paired messages.
   * @param data The data used for the protocol (number of paired messages and MAX_MESSAGE_NUMBER).
   * @param doublicateFirstMessage True, if Oskar duplicates the first message.
   * @return Array of paired random messages as a two dimensional array.
   */
  public static BigInteger[][] getRandomMessages(int n, SecretSharingProtocolData data, boolean duplicateFirstMessage) {
    BigInteger[][] M = new BigInteger[n][2];
    for (int i=0; i<n; i++) {
      M[i][0] = BigIntegerUtil.randomSmallerThan(data.MAX_MESSAGE_NUMBER, rnd);
      if(duplicateFirstMessage){
        M[i][1] = M[i][0];
      }
      else{
        M[i][1] = BigIntegerUtil.randomSmallerThan(data.MAX_MESSAGE_NUMBER, rnd);
      }

      System.out.println("Message " + i + " [0] = " + M[i][0].toString(data.BASE));
      System.out.println("Message " + i + " [1] = " + M[i][1].toString(data.BASE));
    }
    return M;
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

  public BigInteger[][] sendFirst() {

    // (0) -- ElGamal initialisieren
    ElGamalKeys elGamal = getElGamalKeys();

    for (BigInteger[] message : M) {
      oblivious.obliviousTransferSend(elGamal, message);
    }

    BigInteger MB[] = new BigInteger[n];
    for (int i = 0; i < MB.length; i++) {
      MB[i] = oblivious.obliviousTransferReceive();
      System.out.println("A: Recv Message " + i + " = " + MB[i].toString(data.BASE));
    }

    if(replaceOneSecret){
      for (BigInteger[] message : M) {
        // "geratener" Index der erhaltenen Nachricht = 0
        message[1] = BigIntegerUtil.randomSmallerThan(data.MAX_MESSAGE_NUMBER, rnd);
      }
    }

    Pair<Vector<Vector<Integer>>, Vector<Vector<MessagePrefixes>>> generateRes = generate(M, data.k);
    Vector<Vector<Integer>> secretIndices = generateRes.first;
    Vector<Vector<MessagePrefixes>> notSendPrefixes = generateRes.second;
    notSendPrefixes = mix(notSendPrefixes);

    Vector<Vector<TreeMap<Integer, BigInteger>>> validPrefixesB = generate(n, data.k);

    int m = data.k + 1;
    // in der data.BITS+1en Runde werden die übrigen nicht-gültigen Präfixe verschickt/empfangen.
    while (m <= data.BITS + 1) {
      System.out.println("A: prefixes of length " + m + "\n" + notSendPrefixes);
      System.out.println("A: valid prefixes B of length " + m + "\n" + validPrefixesB);
      int messageIndex = 0;
      for(Vector<MessagePrefixes> mPair : notSendPrefixes){
        int messagePairIndex = 0;
        for(MessagePrefixes mPref : mPair){
          ListIterator<Pair<Integer, BigInteger>> prefix = mPref.listIterator();
          int upperBound = (m == data.BITS+1) ? ((1<<data.k)-1) : (1<<data.k);
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
          assert (m == data.BITS+1) ? true : prefixExists(subRange(M[messageIndex][messagePairIndex], data.BITS-1, (data.BITS-1) - m + 1), mPref);
          ++messagePairIndex;
        }
        ++messageIndex;
      }

      ++m;
      if(m <= data.BITS){
        generateRes = extend(M, m, notSendPrefixes);
        secretIndices = generateRes.first;
        notSendPrefixes = generateRes.second;
        notSendPrefixes = mix(notSendPrefixes);

        validPrefixesB = extendValid(validPrefixesB);
      }
    }

    BigInteger[][] otherMessages = new BigInteger[n][2];
    int i = 0;
    for( Vector<TreeMap<Integer, BigInteger>> mPair  : validPrefixesB){
      int j = 0;
      for( TreeMap<Integer, BigInteger> message : mPair){
        assert message.size() == 1;
        otherMessages[i][j] = message.firstEntry().getValue();
        System.out.println("A: Nachricht von B:" + otherMessages[i][j].toString(data.BASE));
        j++;
      }
      i++;
    }

    checkMessages(MB, validPrefixesB);
    return otherMessages;
  }

  public BigInteger[][] receiveFirst() {

    // (0) -- ElGamal initialisieren
    ElGamalKeys elGamal = getElGamalKeys();

    BigInteger MA[] = new BigInteger[n];
    for (int i = 0; i < MA.length; i++) {
      MA[i] = oblivious.obliviousTransferReceive();
      System.out.println("B: Recv Message " + i + " = " + MA[i].toString(data.BASE));
    }

    for (BigInteger[] message : M) {
      oblivious.obliviousTransferSend(elGamal, message);
    }

    Pair<Vector<Vector<Integer>>, Vector<Vector<MessagePrefixes>>> generateRes = generate(M, data.k);
    Vector<Vector<Integer>> secretIndices = generateRes.first;
    Vector<Vector<MessagePrefixes>> notSendPrefixes = generateRes.second;
    notSendPrefixes = mix(notSendPrefixes);

    Vector<Vector<TreeMap<Integer, BigInteger>>> validPrefixesA = generate(n, data.k);

    int m = data.k + 1;
    // in der data.BITS+1en Runde werden die übrigen nicht-gültigen Präfixe verschickt/empfangen.
    while (m <= data.BITS + 1) {
      System.out.println("B: notSendPrefixes of length " + m + "\n" + notSendPrefixes);
      System.out.println("B: valid prefixes A of length " + m + "\n" + validPrefixesA);

      int messageIndex = 0;
      for(Vector<MessagePrefixes> mPair : notSendPrefixes){
        int messagePairIndex = 0;
        for(MessagePrefixes mPref : mPair){
          ListIterator<Pair<Integer, BigInteger>> prefix = mPref.listIterator();
          int upperBound = (m == data.BITS+1) ? ((1<<data.k)-1) : (1<<data.k);
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
          assert (m == data.BITS+1) ? true : prefixExists(subRange(M[messageIndex][messagePairIndex], data.BITS-1, (data.BITS-1) - m + 1), mPref);
          ++messagePairIndex;
        }
        ++messageIndex;
      }

      ++m;
      if(m <= data.BITS){
        generateRes = extend(M, m, notSendPrefixes);
        secretIndices = generateRes.first;
        notSendPrefixes = generateRes.second;
        notSendPrefixes = mix(notSendPrefixes);

        validPrefixesA = extendValid(validPrefixesA);
      }

    }

    BigInteger[][] otherMessages = new BigInteger[n][2];
    int i = 0;
    for( Vector<TreeMap<Integer, BigInteger>> mPair  : validPrefixesA){
      int j = 0;
      for( TreeMap<Integer, BigInteger> message : mPair){
        assert message.size() == 1;
        otherMessages[i][j] = message.firstEntry().getValue();
        System.out.println("B: Nachricht von A:" + otherMessages[i][j].toString(data.BASE));
        j++;
      }
      i++;
    }

    checkMessages(MA, validPrefixesA);

    return otherMessages;
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
          assert prefix.bitLength() <= data.BITS;
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
          BigInteger b = subRange(ms[messageIndex][messagePairIndex], data.BITS-1, (data.BITS-1) - m + 1);
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

        assert mPref.size() == (1 << (data.k + 1));

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
        BigInteger b = subRange(m, data.BITS-1, (data.BITS-1) - k);
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

}
