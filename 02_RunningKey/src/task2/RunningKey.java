/*
 * jCrypt - Programmierumgebung für das Kryptologie-Praktikum
 * Studienarbeit am Institut für Theoretische Informatik der
 * Technischen Universität Braunschweig
 * 
 * Datei:        RunningKey.java
 * Beschreibung: Dummy-Implementierung der Chiffre mit laufendem Schlüssel
 * Erstellt:     30. März 2010
 * Autor:        Martin Klußmann
 */

package task2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.StringTokenizer;

import de.tubs.cs.iti.jcrypt.chiffre.Cipher;

/**
 * Klasse für die Chiffre mit laufendem Schlüssel.
 * 
 * @author Henning Basold, Raimar Bühmann (Vorlage von Martin Klußmann)
 * @version 1.0 - Tue Mar 30 16:23:47 CEST 2010
 */
public class RunningKey extends Cipher {

  /**
   * Erzeugt einen neuen Schlüssel, der in der Klasse gespeichert wird,
   * damit durch einen anschließenden Methodenaufruf von
   * {@link #writeKey(BufferedWriter)}
   * der Schlüssel gespeichert werden kann.
   */
  public void makeKey() {
    System.out.println("Dummy für die Schlüsselerzeugung.");
  }

  /**
   * Schreibt den Schlüssel mit dem Writer <code>key</code>.
   * <p>
//   * Der Modulus und die Verschiebung werden durch ein Leerzeichen getrennt in
//   * die Schlüsseldatei geschrieben. Eine solche Schlüsseldatei hat also das
//   * folgende Format:
//   * 
//   * <pre style="background-color:#f0f0f0; border:1pt silver solid; * padding:3px">
//   * modulus numberOfShifts shift0 shift1 ...
//   * </pre>
   * </p>
   * 
   * @param key
   *          Der Writer, der in die Schlüsseldatei schreibt.
   * @see #makeKey makeKey
   * @see #readKey readKey
   */
  public void writeKey(BufferedWriter key) {
    try {
//      key.write(modulus + " " + shifts.length);
//      for (int i = 0; i < shifts.length; i++) {
//        key.write(" " + shifts[i]);
//      }
      key.newLine();
      key.close();
    } catch (IOException e) {
      System.out.println("Abbruch: Fehler beim Schreiben der Schlüsseldatei.");
      e.printStackTrace();
      System.exit(1);
    }
  }

  /**
   * Liest den Schlüssel mit dem Reader <code>key</code>
   * und speichert diesen in der Klasse. Anschließend kann
   * {@link #encipher(BufferedReader, BufferedWriter)}
   * mit diesem Schlüssel verschlüsseln. Das verwendete Format
   * wird in {@link #writeKey(BufferedWriter)} beschrieben.
   * 
   * @param key
   *          Der Reader, der aus der Schlüsseldatei liest.
   */
  public void readKey(BufferedReader key) {
    try {
      StringTokenizer st = new StringTokenizer(key.readLine(), " ");
//      modulus = Integer.parseInt(st.nextToken());
//      System.out.println("Modulus: " + modulus);
//      int numberOfShifts = Integer.parseInt(st.nextToken());
//      System.out.println(numberOfShifts + " Verschiebungen: ");
//      shifts = new int[numberOfShifts];
//      for (int i = 0; i < numberOfShifts; i++) {
//        shifts[i] = Integer.parseInt(st.nextToken());
//        System.out.println(" " + shifts[i]);
//      }
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
   * Verschlüsselt den durch den Reader <code>cleartext</code> gegebenen
   * Klartext und schreibt den Chiffretext mit dem Writer
   * <code>ciphertext</code>.
   * Der Schlüssel wird je nach Kommandozeile entweder durch {@link #readKey(BufferedReader)}
   * oder durch {@link #breakCipher(BufferedReader, BufferedWriter)}
   * bestimmt.
   * 
   * @param cleartext
   *          Der Reader, der den Klartext liefert.
   * @param ciphertext
   *          Der Writer, der den Chiffretext schreiben soll.
   */
  public void encipher(BufferedReader cleartext, BufferedWriter ciphertext) {

  }

  /**
   * Entschlüsselt den durch den Reader <code>ciphertext</code> gegebenen
   * Chiffretext und schreibt den Klartext mit dem Writer <code>cleartext</code>.
   * Der Schlüssel wird je nach Kommandozeile entweder durch {@link #readKey(BufferedReader)}
   * oder durch {@link #breakCipher(BufferedReader, BufferedWriter)}
   * bestimmt.
   * 
   * @param ciphertext
   *          Der Reader, der den Chiffretext liefert.
   * @param cleartext
   *          Der Writer, der den Klartext schreiben soll.
   */
  public void decipher(BufferedReader ciphertext, BufferedWriter cleartext) {

  }

  /**
   * Analysiert den durch den Reader <code>ciphertext</code> gegebenen
   * Chiffretext, bricht die Chiffre bzw. unterstützt das Brechen der Chiffre
   * (ggf. interaktiv). Der erkannte Schlüssel wird in der Klasse gespeichert,
   * damit {@link #decipher(BufferedReader, BufferedWriter)}
   * den Text entschlüsseln kann.
   * 
   * @param ciphertext
   *          Der Reader, der den Chiffretext liefert.
   * @param cleartext
   *          Bug: Dieser Writer hat keine Bedeutung. (Vorher:
   *          Der Writer, der den Klartext schreiben soll.)
   */
  public void breakCipher(BufferedReader ciphertext, BufferedWriter cleartext) {

  }

}
