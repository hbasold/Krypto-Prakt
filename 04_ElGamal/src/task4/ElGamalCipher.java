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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Random;

import de.tubs.cs.iti.jcrypt.chiffre.BigIntegerUtil;
import de.tubs.cs.iti.jcrypt.chiffre.BlockCipher;

/**
 * Dummy-Klasse für das ElGamal-Public-Key-Verschlüsselungsverfahren.
 *
 * @author Martin Klußmann
 * @version 1.1 - Sat Apr 03 22:06:35 CEST 2010
 */
public final class ElGamalCipher extends BlockCipher {
  
  private BigInteger p;
  private BigInteger g;
  private BigInteger x;
  private BigInteger y;

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
      BigInteger pMinus1MinusX = p.subtract(BigInteger.ONE); // p-1-x
      BigInteger c = readCipher(ciphertext); // Block einlesen
      // TODO: Woher weiß BlockCypherUtil, wie viele Bytes ein Block hat?
      while (c != null) { // solange Block vorhanden
        BigInteger a = c.mod(p);                   // a = C' mod p
        BigInteger b = c.divide(p);                // b = C' div p
        BigInteger z = a.modPow(pMinus1MinusX, p); // z = a^(p-1-x) mod p
        BigInteger m = z.multiply(b).mod(p);       // M = z*b mod p
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
    Random rnd = new Random(System.currentTimeMillis());
    BigInteger upperBoundK = p.subtract(BigInteger.ONE);
    try {
      // Anzahl der Bytes pro Block
      int blocksize = (int) Math.floor( (p.bitLength()-1)/8 );
      BigInteger m = readClear(cleartext, blocksize); // Block einlesen
      while (m != null) { // solange Block vorhanden
        BigInteger k = BigIntegerUtil.randomBetween(BigInteger.ONE, upperBoundK, rnd);
        BigInteger a = g.modPow(k, p);                    // a =   g^k mod p
        BigInteger b = m.multiply(y.modPow(k, p)).mod(p); // b = M*y^k mod p
        BigInteger c = a.add(b.multiply(p));              // C'= a+b*p
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
    

    Random rnd = new Random(System.currentTimeMillis());
    BigInteger q, p;
    do {
      q = BigInteger.probablePrime(bitLength, rnd);
      p = q.multiply(BigInteger.valueOf(2)).add(BigInteger.ONE);
    } while(!p.isProbablePrime(100));
    
    BigInteger g;
    BigInteger minus1ModP = p.subtract(BigInteger.ONE); // BigInteger.valueOf(-1).mod(p);
    do {
      g = BigIntegerUtil.randomBetween(BigInteger.valueOf(2), p.subtract(BigInteger.ONE), rnd);
    } while(!g.modPow(q, p).equals(minus1ModP));
    
    System.out.println("q=" + q + ", p= " + p + ", g=" + g);
    
    this.p = p;
    this.g = g;
    x = BigIntegerUtil.randomBetween(BigInteger.ONE, p.subtract(BigInteger.ONE), rnd);
    y = g.modPow(x, p);
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
      
      BufferedReader in = new BufferedReader(new FileReader(privateKeyFile));
      p = new BigInteger(in.readLine(), 16);
      g = new BigInteger(in.readLine(), 16);
      x = new BigInteger(in.readLine(), 16);
      
      in = new BufferedReader(new FileReader(publicKeyFile));
      BigInteger p_ = new BigInteger(in.readLine(), 16);
      BigInteger g_ = new BigInteger(in.readLine(), 16);
      y = new BigInteger(in.readLine(), 16);
      
      assert p.equals(p_);
      assert g.equals(g_);
      
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
      
      BufferedWriter out = new BufferedWriter(new FileWriter(privateKeyFile));
      out.write(p.toString(16)); out.newLine();
      out.write(g.toString(16)); out.newLine();
      out.write(x.toString(16)); out.newLine();
      out.close();      
      
      out = new BufferedWriter(new FileWriter(publicKeyFile));
      out.write(p.toString(16)); out.newLine();
      out.write(g.toString(16)); out.newLine();
      out.write(y.toString(16)); out.newLine();
      out.close();
      
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
