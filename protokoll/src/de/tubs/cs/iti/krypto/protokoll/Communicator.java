/* Generated by Together */

package de.tubs.cs.iti.krypto.protokoll;

import java.net.*;
import java.io.*;
import java.util.*;

/**
 * Die Klasse Communicator dient als Kommunikationsschnittstelle zwischen den
 * Protokollen und dem Server. Im Communicator werden Methoden realisiert, die
 * den Datenaustauch zwischen den Parteien des Netzwerks ueber den Server
 * ermoeglichen.
 * 
 * @author Wolfgang Schmiesing
 * @version 1.0
 */

public class Communicator {

  private Socket connection;
  private BufferedReader in;
  private int maxPlayer;
  private int minPlayer;
  private IClient myClient;
  private int myNumber;
  private String nameOfTheGame;
  private int playerNumber;
  private PrintWriter out;

  /**
   * Der Konstruktor zur Klasse Communicator erzeugt eine neue Kommunikations-
   * schnittstelle, die vom Client "client" benutzt wird. Im Konstruktor wird
   * eine Socketverbindung zum Server aufgebaut, zwei Streams zur Kommunikation
   * initialisiert und die fuer die Kommunikation notwendigen Variablen
   * Spielname Minimal/Maximalspielerzahl initialisiert. Danach werden diese
   * Daten an den Server uebermittelt, der dann die entsprechende Spielernummer
   * generiert und zurueckliefert.
   * 
   * @exception IOException
   * @exception UnknownHostException
   * @param host
   *          Hostname
   * @param port
   *          Portnummer
   * @param name
   *          Spielname
   * @param max
   *          maximale Spieleranzahl
   * @param min
   *          minimale Spieleranzahl
   * @param client
   *          aufrufender Client
   */
  public Communicator(String host, int port, String name, int max, int min,
      IClient client) throws UnknownHostException, IOException {
    connection = new Socket(InetAddress.getByName(host), port);
    connection.setSoTimeout(600000);
    in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
    out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
        connection.getOutputStream())));
    StringTokenizer ST = new StringTokenizer(name);
    String newName = "";
    String numberString = "";

    while (ST.hasMoreTokens()) {
      newName = newName + ST.nextToken();
    }

    nameOfTheGame = newName;
    maxPlayer = max;
    minPlayer = min;
    myClient = client;
    String serverdata = min + " " + max;
    sendTo(-1, serverdata);

    // System.out.println("Communicator: receiving my number!");
    numberString = receive();

    myNumber = Integer.parseInt(numberString);
    // if(myNumber==0){connection.setSoTimeout(30000);} //nur test
    System.out.println("communicator: my number: " + myNumber);
  } // constructor

  /**
   * Diese Methode gibt die Spielernummer des Spielers zurueck.
   * 
   * @return <code>int</code> Spielernummer
   */
  public int myNumber() {
    return myNumber;
  }

  /**
   * Diese Methode stellt die Anzahl der Parteien fest, die mit dem Server
   * verbunden sind.
   * 
   * @return <code>int</code> Anzahl der Spieler
   */
  public int playerNumber() {
    return playerNumber;
  }

  /**
   * Diese Methode realisiert den Empfang von Daten von anderen Spielern. Im
   * Fehlerfall (Nachricht undefiniert) wird die Verbindung getrennt und das
   * Programm mit Ausgabe einer Fehlermeldung abgebrochen.
   * 
   * @exception IOException
   * @exception InterruptedIOException
   * @return <code>String</code> empfangene Daten
   */
  public String receive() {
    String s = "";
    int len;
    char[] c;
    String re = null;
    StringWriter w = new StringWriter();
    PrintWriter result = new PrintWriter(w);
    int check;

    try {
      while (s.equals("")) { // Aufruf soll blockieren bis Daten empfangen sind
        if (in.ready()) { // zuerst L"ange der Daten lesen, dann Daten selbst
          len = Integer.parseInt(in.readLine());
          c = new char[len];
          if (in.read(c, 0, len) == -1)
            System.out.println("Communicator " + myNumber
                + " : error receiving message!");

          s = new String(c);
        }
      }

      /*
       * while (s.equals("")) { while (in.ready()) { // read one line
       * result.print(in.readLine());
       * 
       * // if there is more make a newline if (in.ready()) result.println(); }
       * // get the read string result.flush(); w.flush(); s = w.toString(); }
       */

      if (s == null) {
        System.out.println("communicator: connection closed by server");

        try {
          connection.close();
        } catch (IOException ex) {
        }

        myClient.end("Aborting game, connection was closed by server");
      }

      check = Character.getNumericValue(s.charAt(0));
      s = s.substring(1);

      if (check == 0) {
        System.out.println("communicator: error! connection closed; reason: "
            + s);

        try {
          connection.close();
        } catch (IOException ex) {
        }

        myClient.end(s);
      }
    } catch (InterruptedIOException e) {
      System.out.println("communicator: error! connection was timed out");

      try {
        connection.close();
      } catch (IOException ex) {
      }

      myClient.end("communicator: error! connection was timed out");
    } catch (IOException e) {
      System.out.println("communicator: error! connection closed by server");

      try {
        connection.close();
      } catch (IOException ex) {
      }

      myClient.end("communicator: error! connection closed by server");
    }

    // System.out.println("Communicator received message of length " +
    // s.length() + ": " + s);
    return s;
  } // receive

  /**
   * Die Methode sendTo sendet einen String zum Spieler mit der Spielernummer
   * player. Die Nachricht wird mit der Zieladresse und dem Spielnamen
   * verknuepft.
   * 
   * @param player
   *          Empfaenger der Nachricht
   * @param data
   *          zu sendende Nachricht
   */

  public synchronized void sendTo(int player, String data) {
    data = player + " " + nameOfTheGame + " " + data;
    out.println(data.length());
    out.write(data);
    out.flush();
  }

  /**
   * diese Methode wird nach Erzeugen des Communicator-Objektes gestartet, um
   * vor Beginn des Spiels auf hinzukommende/sich abmeldende Spieler zu warten
   * Falls der Client sich vor Beginn des Spiels abmeldet, liefert die Methode
   * den Wert "false". Bei erfolgreicher Anmeldung und Start des Spiels liefert
   * sie "true" zurueck.
   * 
   * @exception IOException
   * @exception NoSuchElementException
   * @return <code>boolean</code> Nachricht, ob Client das Spiel verlassen hat
   */
  public boolean waitForPlayers() {
    boolean running = false;

    while (!running) {
      String msg = receive();

      if (msg.equals("exit")) {
        try { // falls der Client beenden Button gedrueckt wurde
          connection.close(); // macht das Socket wieder zu und gibt
        } catch (IOException e) { // false zurueck
          System.out.println("communicator: error while closing connection");
          return false;
        }

        return false;
      }

      if (msg.equals("go")) {
        running = true;
      } else {
        try {
          StringTokenizer ST = new StringTokenizer(msg);
          playerNumber = Integer.parseInt(ST.nextToken());
          System.out.println("communicator: number of players " + playerNumber);
          myNumber = Integer.parseInt(ST.nextToken());
          myClient.playerNumberChanged(playerNumber);
          System.out.println("communicator: my number " + myNumber);
        } catch (NoSuchElementException e) {
          System.out
              .println("communicator: corrupt message in method waitForPlayers");
          sendTo(-2, String.valueOf(myNumber()));

          try {
            connection.close();
          } catch (IOException ex) {
          }

          return false;
        }
      }
    }

    return true;
  } // waitForPlayers
}

// $Log: Communicator.java,v $
// Revision 1.10 2001/07/03 21:52:17 y0013515
// minor updates
//
// Revision 1.8 2001/06/28 17:46:55 y0013515
// GUI.dispose() hinzugef?gt
//
// Revision 1.7 2001/06/27 22:04:45 y0013515
// update fuer das design und kleineaenderungen am netzwerk
// (kommentare)
//
// Revision 1.3 2001/06/20 23:13:57 y0013515
// alte files geloescht
//
// Revision 1.8 2001/06/18 08:06:08 y0013515
// NetzwerkUpdate:
// Socket-TimeOut eingestellt
// Fehlerbehandlung bei TimeOut Ueberschreitung hinzugefuegt
//