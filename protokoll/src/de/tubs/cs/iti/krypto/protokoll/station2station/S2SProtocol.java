package de.tubs.cs.iti.krypto.protokoll.station2station;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import task5.ChaumHash;
import task5.HashExpansion;
import de.tubs.cs.iti.krypto.protokoll.Communicator;
import de.tubs.cs.iti.krypto.protokoll.Protocol;

public class S2SProtocol implements Protocol {
  
  private Communicator c;
  private ChaumHash chaum = new ChaumHash();
  private HashExpansion hash = new HashExpansion(chaum);

  public S2SProtocol() throws IOException {
    BufferedReader params = new BufferedReader(new FileReader("../protokolle/Station-to-Station/hashparameters"));
    chaum.readKeys(params, 10);
  }
  
  @Override
  public void setCommunicator(Communicator Com) {
    c = Com;
  }

  @Override
  public String nameOfTheGame() {
    return "Station-To-Station";
  }

  @Override
  public void sendFirst() {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void receiveFirst() {
    // TODO Auto-generated method stub
    
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
