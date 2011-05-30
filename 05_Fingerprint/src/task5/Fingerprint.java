/*
 * jCrypt - Programmierumgebung für das Kryptologie-Praktikum
 * Studienarbeit am Institut für Theoretische Informatik der
 * Technischen Universität Braunschweig
 *
 * Datei:        Fingerprint.java
 * Beschreibung: Dummy-Implementierung der Hash-Funktion von Chaum, van Heijst
 *               und Pfitzmann
 * Erstellt:     30. März 2010
 * Autor:        Martin Klußmann
 */

package task5;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;

import de.tubs.cs.iti.jcrypt.chiffre.HashFunction;

/**
 * Dummy-Klasse für die Hash-Funktion von Chaum, van Heijst und Pfitzmann.
 *
 * @author Martin Klußmann
 * @version 1.1 - Sat Apr 03 22:20:18 CEST 2010
 */
public final class Fingerprint extends HashFunction {

  private ChaumHash keys = new ChaumHash();

  /**
   * Berechnet den Hash-Wert des durch den FileInputStream
   * <code>cleartext</code> gegebenen Klartextes und schreibt das Ergebnis in
   * den FileOutputStream <code>ciphertext</code>.
   *
   * @param cleartext
   * Der FileInputStream, der den Klartext liefert.
   * @param ciphertext
   * Der FileOutputStream, in den der Hash-Wert geschrieben werden soll.
   */
  public void hash(FileInputStream cleartext, FileOutputStream ciphertext) {
    HashExpansion hash = new HashExpansion(keys);
    // Anzahl der Bytes pro Block
    int blocksize = hash.inputBitLength() / 8; // Integer-Division macht Math.floor()
    byte[] input = new byte[blocksize];
    try {
      int read = cleartext.read(input);
      while (read != -1) { // solange Block vorhanden
        hash.concat(input, read);
        read = cleartext.read(input);
      }
      ciphertext.write(hash.read().toByteArray());
      cleartext.close();
      ciphertext.close();
    } catch (IOException e) {
      System.err.println("Abbruch: Fehler beim Zugriff auf Klar- oder Chiffretextdatei.");
      e.printStackTrace();
      System.exit(1);
    }
  }

  /**
   * Erzeugt neue Parameter.
   *
   * @see #readParam readParam
   * @see #writeParam writeParam
   */
  public void makeParam() {
    BufferedReader standardInput = launcher.openStandardInput();
    boolean accepted = false;

    int bitLength = 0;

    do {
      System.out.print("Geben Sie die gewünschte Bitlänge ein (min. 512): ");
      try {
        bitLength = Integer.parseInt(standardInput.readLine());
        if (bitLength < 512) {
          System.out.println("Bitlänge zu klein.");
        } else {
          accepted = true;
        }
      } catch (NumberFormatException e) {
        System.out.println("Bitlänge ungültig.");
      } catch (IOException e) {
        System.err
            .println("Abbruch: Fehler beim Lesen von der Standardeingabe.");
        e.printStackTrace();
        System.exit(1);
      }
    } while (!accepted);

    keys.createKeys(bitLength);
  }

  /**
   * Liest die Parameter mit dem Reader <code>param</code>.
   *
   * @param param
   * Der Reader, der aus der Parameterdatei liest.
   * @see #makeParam makeParam
   * @see #writeParam writeParam
   */
  public void readParam(BufferedReader param) {
    try {
      keys.readKeys(param);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Berechnet den Hash-Wert des durch den FileInputStream
   * <code>cleartext</code> gegebenen Klartextes und vergleicht das
   * Ergebnis mit dem durch den FileInputStream <code>ciphertext</code>
   * gelieferten Wert.
   *
   * @param ciphertext
   * Der FileInputStream, der den zu prüfenden Hash-Wert liefert.
   * @param cleartext
   * Der FileInputStream, der den Klartext liefert, dessen Hash-Wert berechnet
   * werden soll.
   */
  public void verify(FileInputStream ciphertext, FileInputStream cleartext) {
    HashExpansion hash = new HashExpansion(keys);
    // Anzahl der Bytes pro Block
    int blocksize = hash.inputBitLength() / 8; // Integer-Division macht Math.floor()
    byte[] input = new byte[blocksize];
    try {
      int read = cleartext.read(input);
      while (read != -1) { // solange Block vorhanden
        hash.concat(input, read);
        read = cleartext.read(input);
      }
      BigInteger h = hash.read();
      
      byte[] hashInput = new byte[hash.outputBitLength() / 8];
      read = ciphertext.read(hashInput);
      if(read != hash.outputBitLength() / 8){
        System.err.println("Gelesener Hash ungültig: zu kurz.");
      }
      else{
        BigInteger expected = new BigInteger(1, hashInput);
        if(!expected.equals(h)){
          System.err.println("Gelesener Hash ungültig: falscher Wert.");
        }
        else{
          System.out.println("Hash in Ordnung.");
        }
      }
      
      cleartext.close();
      ciphertext.close();
    } catch (IOException e) {
      System.err.println("Abbruch: Fehler beim Zugriff auf Klar- oder Chiffretextdatei.");
      e.printStackTrace();
      System.exit(1);
    }
  }

  /**
   * Schreibt die Parameter mit dem Writer <code>param</code>.
   *
   * @param param
   * Der Writer, der in die Parameterdatei schreibt.
   * @see #makeParam makeParam
   * @see #readParam readParam
   */
  public void writeParam(BufferedWriter param) {
    try {
      keys.writeKeys(param);
      param.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
