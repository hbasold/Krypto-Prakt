/*
 * jCrypt - Programmierumgebung für das Kryptologie-Praktikum
 * Studienarbeit am Institut für Theoretische Informatik der
 * Technischen Universität Braunschweig
 * 
 * Datei:        ElGamalSignature.java
 * Beschreibung: Dummy-Implementierung des ElGamal-Public-Key-Signaturverfahrens
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
import java.util.Random;

import de.tubs.cs.iti.jcrypt.chiffre.BigIntegerUtil;
import de.tubs.cs.iti.jcrypt.chiffre.Signature;

/**
 * Dummy-Klasse für das ElGamal-Public-Key-Signaturverfahren.
 *
 * @author Martin Klußmann
 * @version 1.1 - Sat Apr 03 22:14:47 CEST 2010
 */
public final class ElGamalSignature extends Signature {
  
  private ElGamalKeys keys = new ElGamalKeys();
  private String privateKeyFile;
  private String publicKeyFile;

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
   * Signiert den durch den FileInputStream <code>cleartext</code> gegebenen
   * Klartext und schreibt die Signatur in den FileOutputStream
   * <code>ciphertext</code>.
   * <p>Das blockweise Lesen des Klartextes soll mit der Methode {@link
   * #readClear readClear} durchgeführt werden, das blockweise Schreiben der
   * Signatur mit der Methode {@link #writeCipher writeCipher}.</p>
   * 
   * @param cleartext
   * Der FileInputStream, der den Klartext liefert.
   * @param ciphertext
   * Der FileOutputStream, in den die Signatur geschrieben werden soll.
   */
  public void sign(FileInputStream cleartext, FileOutputStream ciphertext) {
    Random rnd = new Random(System.currentTimeMillis());
    final BigInteger pMinus1 = keys.p.subtract(BigInteger.ONE);
    BigInteger upperBoundK = pMinus1;
    // Anzahl der Bytes pro Block
    int blocksize = (keys.p.bitLength()-1) / 8; // Integer-Division macht Math.floor()
    try {
      BigInteger m = readClear(cleartext, blocksize); // Block einlesen
      while (m != null) { // solange Block vorhanden
        BigInteger k;
        do {
          k = BigIntegerUtil.randomBetween(BigInteger.ONE, upperBoundK, rnd);
        } while(!k.gcd(pMinus1).equals(BigInteger.ONE));
        
        BigInteger r = keys.g.modPow(k, keys.p);     // r =   g^k mod p
        BigInteger kInv = k.modInverse(pMinus1);
        BigInteger s = m.subtract(keys.x.multiply(r)).multiply(kInv).mod(pMinus1);
        BigInteger c = r.add(s.multiply(keys.p));              // C'= a+b*p
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
   * Überprüft die durch den FileInputStream <code>ciphertext</code> gegebene
   * Signatur auf den vom FileInputStream <code>cleartext</code> gelieferten
   * Klartext.
   * <p>Das blockweise Lesen der Signatur soll mit der Methode {@link
   * #readCipher readCipher} durchgeführt werden, das blockweise Lesen des
   * Klartextes mit der Methode {@link #readClear readClear}.</p>
   *
   * @param ciphertext
   * Der FileInputStream, der die zu prüfende Signatur liefert.
   * @param cleartext
   * Der FileInputStream, der den Klartext liefert, auf den die Signatur
   * überprüft werden soll.
   */
  public void verify(FileInputStream ciphertext, FileInputStream cleartext) {
    try {
      final BigInteger pMinus1 = keys.p.subtract(BigInteger.ONE);
      // Die Blocklänge wird durch die ersten 8 Zeichen erkannt (interpretiert als Hex-Zahl).
      BigInteger c = readCipher(ciphertext); // Block einlesen
      int blocksize = (keys.p.bitLength()-1) / 8; // Integer-Division macht Math.floor()
      while (c != null) { // solange Block vorhanden
        BigInteger m = readClear(cleartext, blocksize); // Block einlesen
        BigInteger r = c.mod(keys.p);                   // a = C' mod p
        BigInteger s = c.divide(keys.p);                // b = C' div p
        if(r.compareTo(BigInteger.ONE) < 0 || r.compareTo(pMinus1) > 0){
          System.err.println("Signatur für Block " + m + " nicht in Ordnung.");
        }
        else{
          BigInteger v1 = keys.y.modPow(r, keys.p).multiply(r.modPow(s, keys.p)).mod(keys.p);
          BigInteger v2 = keys.g.modPow(m, keys.p);
          if(!v1.equals(v2)){
            System.err.println("Signatur für Block " + m + " nicht in Ordnung.");
          }
        }
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
