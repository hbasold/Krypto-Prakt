package de.tubs.cs.iti.krypto.protokoll.oblivious;

import java.io.IOException;
import java.math.BigInteger;

import de.tubs.cs.iti.krypto.protokoll.Communicator;
import de.tubs.cs.iti.krypto.protokoll.Protocol;

public class SecretSharing implements Protocol {

  private boolean manipulateObliviousSignature = false;
  private boolean duplicateFirstMessage = false;
  private boolean replaceOneSecret = false;
  
  private SecretSharingProtocol secretSharingProtocal;

  public SecretSharing() throws IOException {
    SecretSharingProtocolData data = SecretSharingProtocolData.getTestData();
    secretSharingProtocal = new SecretSharingProtocol(
        data,
        new ObliviousTransfer1of2Protocol(manipulateObliviousSignature),
        SecretSharingProtocol.getRandomMessages(10, data, duplicateFirstMessage),
        replaceOneSecret);
  }

  @Override
  public void setCommunicator(Communicator com) {
    secretSharingProtocal.setCommunicator(com);
  }

  @Override
  public String nameOfTheGame() {
    return "SecretSharing";
  }

  @Override
  public void sendFirst() {
    BigInteger[][] messages = secretSharingProtocal.sendFirst();
    System.out.println("A: received messages:\n" + messages);
  }

  @Override
  public void receiveFirst() {
    BigInteger[][] messages = secretSharingProtocal.receiveFirst();
    System.out.println("B: received messages:\n" + messages);
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
