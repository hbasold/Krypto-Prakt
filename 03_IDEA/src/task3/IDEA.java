/*
 * jCrypt - Programmierumgebung für das Kryptologie-Praktikum
 * Studienarbeit am Institut für Theoretische Informatik der
 * Technischen Universität Braunschweig
 *
 * Datei:        IDEA.java
 * Beschreibung: Dummy-Implementierung des International Data Encryption
 *               Algorithm (IDEA)
 * Erstellt:     30. März 2010
 * Autor:        Martin Klußmann
 */

package task3;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;
import de.tubs.cs.iti.jcrypt.chiffre.BlockCipher;

/**
 * Dummy-Klasse für den International Data Encryption Algorithm (IDEA).
 *
 * @author Martin Klußmann
 * @version 1.1 - Sat Apr 03 21:57:35 CEST 2010
 */
public final class IDEA extends BlockCipher {

  UInt16[] key;

  /**
   * Entschlüsselt den durch den FileInputStream <code>ciphertext</code>
   * gegebenen Chiffretext und schreibt den Klartext in den FileOutputStream
   * <code>cleartext</code>.
   *
   * @param ciphertext
   * Der FileInputStream, der den Chiffretext liefert.
   * @param cleartext
   * Der FileOutputStream, in den der Klartext geschrieben werden soll.
   */
  public void decipher(FileInputStream ciphertext, FileOutputStream cleartext) {
    try {
      byte[] inBlock  = new byte[8]; // 64 bit block
      byte[] outBlock = new byte[8]; // 64 bit block
      CoDecIDEA idea = new CoDecIDEA(key);

      if (ciphertext.read(inBlock)!=8) {
        throw new IOException("Kein vollständiger initialer Vektor (8 Bytes) vorhanden!");
      }
      CBC cbc = new CBC(idea, inBlock);

      int len = ciphertext.read(inBlock);
      while (len!=-1) {
        // if not read a full block, fill with spaces
        if (len!=8) {
          throw new IOException("Kein vollständiger Block (8 Bytes) am Ende der Datei!");
        }
        cbc.decryptNextBlock(inBlock, outBlock);
        cleartext.write(outBlock); // write decrypted block
        len = ciphertext.read(inBlock);
      }
      cleartext.close();
      ciphertext.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Verschlüsselt den durch den FileInputStream <code>cleartext</code>
   * gegebenen Klartext und schreibt den Chiffretext in den FileOutputStream
   * <code>ciphertext</code>.
   *
   * @param cleartext
   * Der FileInputStream, der den Klartext liefert.
   * @param ciphertext
   * Der FileOutputStream, in den der Chiffretext geschrieben werden soll.
   */
  public void encipher(FileInputStream cleartext, FileOutputStream ciphertext) {
    try {
      byte[] inBlock  = new byte[8]; // 64 bit block
      byte[] outBlock = new byte[8]; // 64 bit block

      Random rnd = new Random(System.currentTimeMillis());
      rnd.nextBytes(outBlock);
      ciphertext.write(outBlock);

      CoDecIDEA idea = new CoDecIDEA(key);
      CBC cbc = new CBC(idea, outBlock);

      int len = cleartext.read(inBlock);
      while (len!=-1) {
        // if not read a full block, fill with spaces
        for (int i=len; i<inBlock.length; i++) {
          inBlock[i] = (byte) ' ';
        }
        cbc.encryptNextBlock(inBlock, outBlock);
        ciphertext.write(outBlock); // write encrypted block
        len = cleartext.read(inBlock);
      }
      cleartext.close();
      ciphertext.close();
    } catch (IOException e) {
      e.printStackTrace();
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
    // Frage jeweils solange die Eingabe ab, bis diese akzeptiert werden kann.
    do {
      System.out.print("Geben Sie den Schlüssel ein (entweder 16 Zeichen oder nichts, dann wird ein zufälliger generiert): ");
      try {
        String key_ = standardInput.readLine();
        if (!(key_.isEmpty() || key_.length() == 16)) {
          System.out.println("Schlüssel muss leer sein oder genau 16 Zeichen haben. Bitte "
              + "korrigieren Sie Ihre Eingabe.");
        } else {
          accepted = true;
          if(key_.length() == 0){
            // Zufällig erzeugen
            Random rnd = new Random(System.currentTimeMillis());
            key = new UInt16[8];
            for(int i = 0; i < 8; ++i){
              key[i] = new UInt16(rnd.nextInt(0xFFFF + 1));
            }
          }
          else{
            key = toUInt16s(key_.toCharArray());
          }
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
  }

  private UInt16[] toUInt16s(char[] k_) {
    UInt16[] k = new UInt16[8];
    for(int i = 0; i < 16; i += 2){
      k[i/2] = new UInt16((k_[i]) << 8 | k_[i + 1]);
      char[] a = new char[2];
      k[i/2].copyTo(a, 0);
    }
    return k;
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
      String key_ = key.readLine();
      this.key = toUInt16s(key_.toCharArray());
      //System.out.println("Schlüssel: " + this.key);
      key.close();
    } catch (IOException e) {
      System.err.println("Abbruch: Fehler beim Lesen oder Schließen der "
          + "Schlüsseldatei.");
      e.printStackTrace();
      System.exit(1);
    } catch (NumberFormatException e) {
      System.err.println("Abbruch: Fehler beim Parsen eines Wertes aus der "
          + "Schlüsseldatei.");
      e.printStackTrace();
      System.exit(1);
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
      char[] str = new char[16];
      for(int i = 0; i < this.key.length; ++i){
        this.key[i].copyTo(str, i * 2);
      }
      System.out.println(str);
      key.write(String.copyValueOf(str));
      key.close();
    } catch (IOException e) {
      System.out.println("Abbruch: Fehler beim Schreiben oder Schließen der "
          + "Schlüsseldatei.");
      e.printStackTrace();
      System.exit(1);
    }
  }
}
