package de.tubs.cs.iti.krypto.protokoll.oblivious;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Random;
import java.util.Vector;

import task4.ElGamalKeys;
import de.tubs.cs.iti.jcrypt.chiffre.BigIntegerUtil;
import de.tubs.cs.iti.krypto.protokoll.Communicator;
import de.tubs.cs.iti.krypto.protokoll.Protocol;

public class SecretSharing implements Protocol {

  private int n = 2; // max. 10 ; Anzahl Wörter
  private int k = 3; // max. 7  : Vorteil 2^k+1 zu 2^k
  
  private Random rnd = new Random(System.currentTimeMillis());

  private ObliviousTransfer1of2Protocol oblivious = new ObliviousTransfer1of2Protocol(true);

  public SecretSharing() throws IOException {
  }

  @Override
  public void setCommunicator(Communicator Com) {
    oblivious.setComm(Com);
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
    
    int m=51;
    while (m>=0) {
      Vector<Vector<HashSet<BigInteger>>> sendPrefixes = new Vector<Vector<HashSet<BigInteger>>>();
      for (int i = 0; i < 1<<k ; i++) {
        sendPrefixes
      }
        int begin, end;
        BigInteger b = subRange(b, begin, end);
      }
    }
    
    

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
