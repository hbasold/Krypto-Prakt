package de.tubs.cs.iti.krypto.protokoll.oblivious;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.Random;

import task4.ElGamalKeys;
import de.tubs.cs.iti.krypto.protokoll.Communicator;
import de.tubs.cs.iti.krypto.protokoll.Protocol;

public class ObliviousTransfer1of2 implements Protocol {

  private ObliviousTransfer1of2Data data = new ObliviousTransfer1of2Data(true);

  public ObliviousTransfer1of2() throws IOException {
    data.rnd = new Random(System.currentTimeMillis());

  }

  @Override
  public void setCommunicator(Communicator Com) {
    data.setComm(Com);
  }

  @Override
  public String nameOfTheGame() {
    return "1-of-2-Oblivious-Transfer";
  }

  @Override
  public void sendFirst() {

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

    BufferedReader standardInput = new BufferedReader(new InputStreamReader(System.in));
    String message;
    try {
      BigInteger M[] = new BigInteger[2];
      do{
        System.out.print("Nachricht 1:");
        message = standardInput.readLine();
        M[0] = new BigInteger(1, message.getBytes());
      } while(M[0].compareTo(elGamal.p) >= 0);

      if(data.isOskar){
        M[1] = new BigInteger(1, message.getBytes());
      }
      else{
        do{
          System.out.print("Nachricht 2:");
          message = standardInput.readLine();
          M[1] = new BigInteger(1, message.getBytes());
        } while(M[1].compareTo(elGamal.p) >= 0);
      }

      data.obliviousTransferSend(elGamal, M);

    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Override
  public void receiveFirst() {
    BigInteger MA = data.obliviousTransferReceive();
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
