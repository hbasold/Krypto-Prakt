/*
 * jCrypt - Programmierumgebung für das Kryptologie-Praktikum
 * Studienarbeit am Institut für Theoretische Informatik der
 * Technischen Universität Braunschweig
 *
 * Datei:        ElGamalCipher.java
 * Beschreibung: Dummy-Implementierung der ElGamal-Public-Key-Verschlüsselung
 * Erstellt:     30. März 2010
 * Autor:        Martin Klußmann
 */

package task4;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import de.tubs.cs.iti.jcrypt.chiffre.BlockCipher;

/**
 * Dummy-Klasse für das ElGamal-Public-Key-Verschlüsselungsverfahren.
 *
 * @author Martin Klußmann
 * @version 1.1 - Sat Apr 03 22:06:35 CEST 2010
 */
public final class ElGamalCipher extends BlockCipher {

  private ElGamalKeys keys = new ElGamalKeys();
  private String privateKeyFile;
  private String publicKeyFile;

  /**
   * Entschlüsselt den durch den FileInputStream <code>ciphertext</code>
   * gegebenen Chiffretext und schreibt den Klartext in den FileOutputStream
   * <code>cleartext</code>.
   * <p>Das blockweise Lesen des Chiffretextes soll mit der Methode {@link
   * #readCipher readCipher} durchgeführt werden, das blockweise Schreiben des
   * Klartextes mit der Methode {@link #writeClear writeClear}.</p>
   *
   * @param ciphertext
   * Der FileInputStream, der den Chiffretext liefert.
   * @param cleartext
   * Der FileOutputStream, in den der Klartext geschrieben werden soll.
   */
  public void decipher(FileInputStream ciphertext, FileOutputStream cleartext) {
    try {
      // Die Blocklänge wird durch die ersten 8 Zeichen erkannt (interpretiert als Hex-Zahl).
      BigInteger c = readCipher(ciphertext); // Block einlesen
      while (c != null) { // solange Block vorhanden
        BigInteger m = keys.decrypt(c);
        writeClear(cleartext, m); // entschlüsselten Block schreiben
        c = readCipher(ciphertext); // nächsten verschlüsselten Block einlesen
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
   * Verschlüsselt den durch den FileInputStream <code>cleartext</code>
   * gegebenen Klartext und schreibt den Chiffretext in den FileOutputStream
   * <code>ciphertext</code>.
   * <p>Das blockweise Lesen des Klartextes soll mit der Methode {@link
   * #readClear readClear} durchgeführt werden, das blockweise Schreiben des
   * Chiffretextes mit der Methode {@link #writeCipher writeCipher}.</p>
   *
   * @param cleartext
   * Der FileInputStream, der den Klartext liefert.
   * @param ciphertext
   * Der FileOutputStream, in den der Chiffretext geschrieben werden soll.
   */
  public void encipher(FileInputStream cleartext, FileOutputStream ciphertext) {
    // Anzahl der Bytes pro Block
    int blocksize = (keys.p.bitLength()-1) / 8; // Integer-Division macht Math.floor()
    try {
      BigInteger m = readClear(cleartext, blocksize); // Block einlesen
      while (m != null) { // solange Block vorhanden
        BigInteger c = keys.encrypt(m);
        writeCipher(ciphertext, c);          // verschlüsselten Block schreiben
        m = readClear(cleartext, blocksize); // nächsten Klartext-Block einlesen
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
   * Erzeugt einen neuen Schlüssel.
   *
   * @see #readKey readKey
   * @see #writeKey writeKey
   */
  public void makeKey() {
    BufferedReader standardInput = launcher.openStandardInput();
    boolean accepted = false;

    int bitLength = 0;

    // Frage jeweils solange die Eingabe ab, bis diese akzeptiert werden kann.
    do {
      System.out.print("Geben Sie den Pfad zu der Schlüsseldatei für den privaten Schlüssel ein: ");
      try {
        privateKeyFile = standardInput.readLine();
        if (privateKeyFile.isEmpty()) {
          System.out.println("Dateinamen eingeben!");
        } else {
          publicKeyFile = privateKeyFile + ".public";
          accepted = true;
        }
      } catch (NumberFormatException e) {
        // kann nicht auftreten
      } catch (IOException e) {
        System.err
            .println("Abbruch: Fehler beim Lesen von der Standardeingabe.");
        e.printStackTrace();
        System.exit(1);
      }
    } while (!accepted);

    accepted = false;

    do {
      System.out.print("Geben Sie die gewünschte Bitlänge ein (min. 512, max. 2048 Bit): ");
      try {
        bitLength = Integer.parseInt(standardInput.readLine());
        if (bitLength < 512 || bitLength > 2048) {
          System.out.println("Bitlänge zu groß/klein.");
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
   * Liest den Schlüssel mit dem Reader <code>key</code>.
   *
   * @param key
   * Der Reader, der aus der Schlüsseldatei liest.
   * @see #makeKey makeKey
   * @see #writeKey writeKey
   */
  public void readKey(BufferedReader key) {
    try {
      privateKeyFile = key.readLine();
      publicKeyFile = key.readLine();

      keys.readKeys(privateKeyFile, publicKeyFile);

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Schreibt den Schlüssel mit dem Writer <code>key</code>.
   *
   * @param key
   * Der Writer, der in die Schlüsseldatei schreibt.
   * @see #makeKey makeKey
   * @see #readKey readKey
   */
  public void writeKey(BufferedWriter key) {
    try {
      key.write(privateKeyFile); key.newLine();
      key.write(publicKeyFile); key.newLine();
      key.close();

      keys.writeKeys(privateKeyFile, publicKeyFile);

    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
