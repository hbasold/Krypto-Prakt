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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.Vector;

import de.tubs.cs.iti.jcrypt.chiffre.CharacterMapping;
import de.tubs.cs.iti.jcrypt.chiffre.Cipher;
import de.tubs.cs.iti.jcrypt.chiffre.FrequencyTables;
import de.tubs.cs.iti.jcrypt.chiffre.NGram;

/**
 * Klasse für die Chiffre mit laufendem Schlüssel.
 * 
 * @author Henning Basold, Raimar Bühmann (Vorlage von Martin Klußmann)
 * @version 1.0 - Tue Mar 30 16:23:47 CEST 2010
 */
public class RunningKey extends Cipher {
  
  private String keyFile;

  /**
   * Erzeugt einen neuen Schlüssel, der in der Klasse gespeichert wird,
   * damit durch einen anschließenden Methodenaufruf von
   * {@link #writeKey(BufferedWriter)}
   * der Schlüssel gespeichert werden kann.
   */
  public void makeKey() {
    BufferedReader standardInput = launcher.openStandardInput();
    boolean accepted = false;
    String msg = "Geeignete Werte für den Modulus werden in der Klasse "
        + "'CharacterMapping'\nfestgelegt. Probieren Sie ggf. einen Modulus "
        + "von 26, 27, 30 oder 31.\nDie Verschiebung muß größer oder gleich 0 "
        + "und kleiner als der gewählte\nModulus sein.";
    System.out.println(msg);
    // Frage jeweils solange die Eingabe ab, bis diese akzeptiert werden kann.
    do {
      System.out.print("Geben Sie den Modulus ein: ");
      try {
        modulus = Integer.parseInt(standardInput.readLine());
        if (modulus < 1) {
          System.out.println("Ein Modulus < 1 wird nicht akzeptiert. Bitte "
              + "korrigieren Sie Ihre Eingabe.");
        } else {
          // Prüfe, ob zum eingegebenen Modulus ein Default-Alphabet existiert.
          String defaultAlphabet = CharacterMapping.getDefaultAlphabet(modulus);
          if (!defaultAlphabet.equals("")) {
            msg = "Vordefiniertes Alphabet: '" + defaultAlphabet
                + "'\nDieses vordefinierte Alphabet kann durch Angabe einer "
                + "geeigneten Alphabet-Datei\nersetzt werden. Weitere "
                + "Informationen finden Sie im Javadoc der Klasse\n'Character"
                + "Mapping'.";
            System.out.println(msg);
            accepted = true;
          } else {
            msg = "Warnung: Dem eingegebenen Modulus kann kein Default-"
                + "Alphabet zugeordnet werden.\nErstellen Sie zusätzlich zu "
                + "dieser Schlüssel- eine passende Alphabet-Datei.\nWeitere "
                + "Informationen finden Sie im Javadoc der Klasse 'Character"
                + "Mapping'.";
            System.out.println(msg);
            accepted = true;
          }
        }
      } catch (NumberFormatException e) {
        System.out.println("Fehler beim Parsen des Modulus. Bitte korrigieren"
            + " Sie Ihre Eingabe.");
      } catch (IOException e) {
        System.err
            .println("Abbruch: Fehler beim Lesen von der Standardeingabe.");
        e.printStackTrace();
        System.exit(1);
      }
    } while (!accepted);
    accepted = false;
    do {
      try {
        System.out.print("Geben Sie den Pfad zur Datei mit dem Schlüsseltext ein: ");
        keyFile = standardInput.readLine();
        if ((new File(keyFile)).exists()) {
          accepted = true;
        } else {
          System.out.println("Datei existiert nicht!");
        }
      } catch (IOException e) {
        System.err
            .println("Abbruch: Fehler beim Lesen von der Standardeingabe.");
        e.printStackTrace();
        System.exit(1);
      }
    } while (!accepted);
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
      key.write(modulus + " ");
      key.write(keyFile);
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
      String line = key.readLine();
      int modulEnd = line.indexOf(' ');
      modulus = Integer.parseInt(line.substring(0, modulEnd));
      System.out.println("Modulus: " + modulus);
      keyFile = line.substring(modulEnd + 1);
      System.out.println("Key file: " + keyFile);
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

    // An dieser Stelle könnte man alle Zeichen, die aus der Klartextdatei
    // gelesen werden, in Klein- bzw. Großbuchstaben umwandeln lassen:
    // charMap.setConvertToLowerCase();
    // charMap.setConvertToUpperCase();

    try {
      FileReader key = new FileReader(keyFile);
      int shift = -1;
      // 'character' ist die Integer-Repräsentation eines Zeichens.
      int character;
      // 'characterSkipped' zeigt an, daß ein aus der Klartextdatei gelesenes
      // Zeichen mit dem gewählten Alphabet nicht abgebildet werden konnte.
      boolean characterSkipped = false;
      // Lese zeichenweise aus der Klartextdatei, bis das Dateiende erreicht
      // ist. Der Buchstabe a wird z.B. als ein Wert von 97 gelesen.
      while ((character = cleartext.read()) != -1
          && (shift = key.read()) != -1) {
        // Bilde 'character' auf dessen interne Darstellung ab, d.h. auf einen
        // Wert der Menge {0, 1, ..., Modulus - 1}. Ist z.B. a der erste
        // Buchstabe des Alphabets, wird die gelesene 97 auf 0 abgebildet:
        // mapChar(97) = 0.
        character = charMap.mapChar(character);
        shift = charMap.mapChar(shift);
        if (character != -1 && shift != -1) {
          // Das gelesene Zeichen ist im benutzten Alphabet enthalten und konnte
          // abgebildet werden. Die folgende Quellcode-Zeile stellt den Kern der
          // Caesar-Chiffrierung dar: Addiere zu (der internen Darstellung von)
          // 'character' zyklisch den 'shift' hinzu.
          character = (character + shift) % modulus;
          // Das nun chiffrierte Zeichen wird von der internen Darstellung in
          // die Dateikodierung konvertiert. Ist z.B. 1 das Ergebnis der
          // Verschlüsselung (also die interne Darstellung für b), so wird dies
          // konvertiert zu 98: remapChar(1) = 98. Der Wert 98 wird schließlich
          // in die Chiffretextdatei geschrieben.
          character = charMap.remapChar(character);
          ciphertext.write(character);
        } else {
          // Das gelesene Zeichen ist im benutzten Alphabet nicht enthalten.
          characterSkipped = true;
        }
      }
      if (characterSkipped) {
        System.out.println("Warnung: Mindestens ein Zeichen aus der "
            + "Klartextdatei oder dem Schlüsseltext ist im Alphabet nicht\nenthalten und wurde "
            + "überlesen.");
      }
      else if(character != -1 && shift == -1){
        System.out.println("Schlüsseltext zu kurz!");
      }
      cleartext.close();
      ciphertext.close();
    } catch (IOException e) {
      System.err.println("Abbruch: Fehler beim Zugriff auf Klar- oder "
          + "Chiffretextdatei.");
      e.printStackTrace();
      System.exit(1);
    }
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

    // Kommentierung analog 'encipher(cleartext, ciphertext)'.
    try {
      FileReader key = new FileReader(keyFile);
      int shift = -1;
      int character;
      while ((character = ciphertext.read()) != -1
          && (shift = key.read()) != -1) {
        character = charMap.mapChar(character);
        shift = charMap.mapChar(shift);
        if (character != -1 && shift != -1) {
          character = (character - shift + modulus) % modulus;
          character = charMap.remapChar(character);
          cleartext.write(character);
        } else {
          // Ein überlesenes Zeichen sollte bei korrekter Chiffretext-Datei
          // eigentlich nicht auftreten können.
        }
      }
      cleartext.close();
      ciphertext.close();
    } catch (IOException e) {
      System.err.println("Abbruch: Fehler beim Zugriff auf Klar- oder "
          + "Chiffretextdatei.");
      e.printStackTrace();
      System.exit(1);
    }
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

    try {

      // Einlesen der Daten der Häufigkeitstabelle. Je nachdem, ob der benutzte
      // Zeichensatz durch Angabe eines Modulus oder durch Angabe eines
      // Alphabets definiert wurde, wird auf unterschiedliche Tabellen
      // zugegriffen.
      // 'nGrams' nimmt die Daten der Häufigkeitstabelle auf.
      ArrayList<NGram> oneGrams = FrequencyTables.getNGramsAsList(1, charMap);
      ArrayList<NGram> biGrams = FrequencyTables.getNGramsAsList(2, charMap);
      ArrayList<NGram> triGrams = FrequencyTables.getNGramsAsList(3, charMap);
      // Bestimme das häufigste Zeichen aus der zugehörigen Unigramm-Tabelle.
      System.out.println("Häufigstes Zeichen in der Unigramm-Tabelle: \""
          + oneGrams.get(0).getCharacters() + "\"");
            
      Scanner stdIn = new Scanner(System.in);
      System.out.print("Textanfang eingeben (bei 0 beginnend): ");
      int textStart = Integer.parseInt(stdIn.nextLine());
      System.out.print("Textende eingeben: ");
      int textEnd = Integer.parseInt(stdIn.nextLine());
      System.out.println();
      
      ciphertext.skip(textStart);
      int textLeft = textEnd - textStart;
      
      // Bestimme das häufigste Zeichen des Chiffretextes.
      // 'character' ist die Integer-Repräsentation eines Zeichens.
      int character;
      Vector<Integer> textBlock = new Vector<Integer>(textLeft);
      // Lese zeichenweise aus der Chiffretextdatei, bis das Dateiende erreicht
      // ist.
      while (textLeft > 0 && (character = ciphertext.read()) != -1) {
        textLeft--;
        // Bilde 'character' auf dessen interne Darstellung ab.
        character = charMap.mapChar(character);
        textBlock.add(character);
      }
      ciphertext.close();
      
//      Vector<Vector<Integer>> possibleKeys = calculatePossibleKeys(textBlock, oneGrams);
      Quantities uniQuantities = Quantities.createLanguageQuantities(1, charMap, modulus);
//      Quantities biQuantities = Quantities.createLanguageQuantities(2, charMap, modulus);
//      Quantities triQuantities = Quantities.createLanguageQuantities(3, charMap, modulus);
      Vector<Quantities> possibleKeys = calculatePossibleKeys(textBlock, uniQuantities);

      System.out.println(possibleKeys);
      
      Vector<Vector<Integer>> keyTexts = calculateMostProbableKeyText(possibleKeys, triGrams);

      System.out.println(keyTexts);
      
    } catch (IOException e) {
      System.err.println("Abbruch: Fehler beim Lesen aus der "
          + "Chiffretextdatei.");
      e.printStackTrace();
      System.exit(1);
    }
  }

  private Vector<Quantities> calculatePossibleKeys(Vector<Integer> textBlock, Quantities languageQuantities) {
    Vector<Quantities> vKeys = new Vector<Quantities>(textBlock.size());

    int numRelevantChars = 5;
    
    for(Integer c : textBlock) {
      Quantities cCandidates = new Quantities(languageQuantities, modulus);
      vKeys.add(cCandidates);
      Quantity enc = new Quantity(c);
      
      for(int i = 0; i < numRelevantChars; ++i){
        cCandidates.add(new Quantity(0, 0, 0));
      }
      // Schlüssel
      for (Quantity key: languageQuantities) {
        int shift = enc.getShift(key, modulus);
        Quantity plain = languageQuantities.getQuantityWithInteger(shift);
        if (plain!=null) {
          double probability = key.getRelativeFrequency() * plain.getRelativeFrequency();
          for(int i = 0; i < numRelevantChars; ++i){
            if(cCandidates.get(i).getRelativeFrequency() < probability){
              System.out.println(key.toString()+" "+plain+" pro:"+probability);
              cCandidates.set(i, new Quantity(key.getInt(), 0, probability));
              break;
            }
          }
        }
      }
    }
    return vKeys;
  }

  private Vector<Vector<Integer>> calculateMostProbableKeyText(
      Vector<Quantities> possibleKeys, ArrayList<NGram> triGrams) {
    Vector<Vector<Integer>> keyTexts = new Vector<Vector<Integer>>();
    Vector<Integer> text = new Vector<Integer>();
    keyTexts.add(text);
    for (int i=0; i<possibleKeys.size()-3 ; i++) {
      List<Quantities> keys3 = possibleKeys.subList(i, i+3);
      text.addAll(calculateMostProbableTrigram(keys3, triGrams));
    }
    return keyTexts;
  }

  private Vector<Integer> calculateMostProbableTrigram(
      List<Quantities> keys3, ArrayList<NGram> triGrams) {
    return null;
  }


}
