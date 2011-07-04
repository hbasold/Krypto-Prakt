package de.tubs.cs.iti.krypto.protokoll.util;

import java.math.BigInteger;
import java.util.Arrays;

import de.tubs.cs.iti.krypto.protokoll.Communicator;

public class P2PCommunicator {
  private Communicator c;
  private int other;

  public P2PCommunicator(Communicator c) {
    super();
    this.c = c;
    this.other = 1 - c.myNumber();
  }

  @SuppressWarnings("unused")
  private void sendBytes(byte[] data) {
    send(new BigInteger(1, data));
  }

  @SuppressWarnings("unused")
  private byte[] receiveBytes() {
    BigInteger initialB_ = receive();
    return toByteArray(initialB_);
  }

  /**
   * Returns /a/ as byte array eliminating the leading byte if its is 0.
   *
   * This is needed, as it contains the sign bit which is unused here.
   *
   * @param a
   * @return
   */
  public byte[] toByteArray(BigInteger a) {
    byte[] initialB = a.toByteArray();
    // Führendes 0-Byte entfernen
    if(initialB[0] == 0){
      return Arrays.copyOfRange(initialB, 1, initialB.length);
    }
    else{
      return initialB;
    }
  }

  public void send(BigInteger p) {
    //System.out.println("Sending " + p.toString(16));
    c.sendTo(other, p.toString(16));
  }

  public BigInteger receive() {
    BigInteger p = new BigInteger(c.receive(), 16);
    //System.out.println("Received " + p.toString(16));
    return p;
  }
}