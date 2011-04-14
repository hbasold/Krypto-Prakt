/*
 * jCrypt - Programmierumgebung für das Kryptologie-Praktikum
 * Studienarbeit am Institut für Theoretische Informatik der
 * Technischen Universität Braunschweig
 * 
 * Datei:        Vigenere.java
 * Beschreibung: Dummy-Implementierung der Vigenère-Chiffre
 * Erstellt:     30. März 2010
 * Autor:        Martin Klußmann
 */

package task1;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.StringTokenizer;
import java.util.Vector;

import de.tubs.cs.iti.jcrypt.chiffre.CharacterMapping;
import de.tubs.cs.iti.jcrypt.chiffre.Cipher;
import de.tubs.cs.iti.jcrypt.chiffre.FrequencyTables;
import de.tubs.cs.iti.jcrypt.chiffre.NGram;

/**
 * Dummy-Klasse für die Vigenère-Chiffre.
 * 
 * @author Martin Klußmann
 * @version 1.0 - Tue Mar 30 15:53:38 CEST 2010
 */
public class Vigenere extends Cipher {

  private int numberOfShifts;
  private int[] shifts;
  
  /**
   * Macht aus einer Liste von Zeichen einen lesbaren String.
   *
   * @param str
   * @return
   */
  @SuppressWarnings("unused")
  private String remapped(List<Integer> str){
    String r = new String();
    for(Integer c : str){
      r += (char)charMap.remapChar(c.intValue());
    }
    return r;
  }

  // Kasiski-Methode

  /**
   * Prüft, ob prefix in text an Position start vorhanden ist.
   */
  private boolean isPrefixAt(List<Integer> prefix, ArrayList<Integer> text, int start){
    ListIterator<Integer> textIter = text.listIterator(start);
    for(Integer c : prefix){
      if(textIter.hasNext()){
        if(textIter.next() != c){
          return false;
        }
      }
      else{
        return false;
      }
    }
    return true;
  }

  /**
   * Versucht prefix in text zu finden und gibt die Startposition zurück.
   * Dies ist die Position des ersten Auftretens.
   *
   * @param prefix
   * @param text
   * @param start
   * @return
   */
  private int findPrefix(List<Integer> prefix, ArrayList<Integer> text, int start){
    ListIterator<Integer> textIter = text.listIterator(start);
    int pos = start;
    boolean found = false;
    while(textIter.hasNext() && !found){
      Integer character = textIter.next();
      if(character == prefix.get(0)
          && isPrefixAt(prefix, text, pos)){
        found = true;
      }
      pos++;
    }

    // Schritt am Ende der Schleife rückgängig machen.
    return found ? (pos - 1) : -1;
  }

  /**
   * Finde in text das längste Textstück t mit |t| = l,
   * so dass text[first, l) == t == text[second, l).
   *
   * @param text
   * @param first
   * @param second
   * @return
   */
  private List<Integer> matchLongestPrefix(ArrayList<Integer> text, int first, int second){
    int pf1 = first;
    int pf2 = second;
    while(pf2 < text.size()
        && text.get(pf1) == text.get(pf2)){
      pf1++;
      pf2++;
    }
    return text.subList(first, pf1);
  }

  /**
   * Misst die Äbstände zwischen den Vorkommen von prefix in text.
   *
   * @param prefix
   * @param text
   * @return
   */
  private Vector<Integer> distances(List<Integer> prefix, ArrayList<Integer> text){
    Vector<Integer> dist = new Vector<Integer>();
    int pos = 0;
    int last = 0;
    boolean notFirst = false;
    while(pos < text.size()){
      int whereNext = findPrefix(prefix, text, pos);
      if(whereNext != -1){
        if(notFirst){
          int distance = whereNext - last;
          dist.add(distance);
        }
        else {
          notFirst = true;
        }
        pos = whereNext + prefix.size();
        last = whereNext;
      }
      else {
        break;
      }
    }

    return dist;
  }

  /**
   * Findet Wiederholungen und Abstände zwischen diesen im Text.
   *
   * @param text
   * @return
   */
  private HashMap<List<Integer>, Vector<Integer> > getRepetitions(ArrayList<Integer> text){
    HashMap<List<Integer>, Vector<Integer> > reps = new HashMap<List<Integer>, Vector<Integer> >();

    List<Integer> prefix;
    for(int i = 0; i < text.size() - 3; i++){
      prefix = text.subList(i, i + 3);
      int where = findPrefix(prefix, text, i + 3);
      // Wiederholung gefunden
      if(where != -1){
        //System.out.println("Wiederholung von \"" + remapped(prefix) + "\" an Position " + i + " gefunden bei " + where );
        prefix = matchLongestPrefix(text, i, where);
        if(!reps.containsKey(prefix)){
          Vector<Integer> dist = distances(prefix, text);
          reps.put(prefix, dist);
          // System.out.println("Wiederholung von \"" + remapped(prefix) + "\" " + (dist.size() + 1) + " Mal gefunden.");
        }
        // gefundene Wiederholung überspringen
        i += prefix.size();
      }
    }

    return reps;
  }

  /**
   * Berechnet den größten gemeinsamen Teiler
   */
  private int gcd(int a, int b) {
    if (b == 0)
      return a;
    else
      return gcd(b, a % b);
  }

  /**
   * Berechnet den ggT aller Werte aus vals.
   *
   * @param vals
   * @return
   */
  private int gcd(Vector<Integer> vals){
    assert(vals.size() >= 2);
    int g = gcd(vals.get(0), vals.get(1));
    for(Integer v : vals){
      g = gcd(g, v);
    }
    return g;
  }

  /**
   * Berechnet den ggT aller Werte aus vals mit
   * Ausnahme der Werte, die zum den restlichen
   * teilerfremd sind.
   *
   * @param vals
   * @return
   */
  private int mostCommonGcd(Vector<Integer> vals){
    assert(vals.size() >= 2);
    int g = 0;
    for(Integer v : vals){
      int g_ = gcd(g, v);
      // nur übernehmen, wenn echter Teiler von allen
      if(g_ != 1){
        g = g_;
      }
    }
    return g;
  }

  /**
   * Vergleicht Vectoren nach ihrer Größe.
   *
   */
  public class SizeComparator implements Comparator<Vector<Integer> >{
    public int compare( Vector<Integer> a, Vector<Integer> b ) {
        return Integer.signum(a.size() - b.size());
    }
  }

  /**
   * Gibt die Menge der Abstände der /count/ am häufigsten
   * vorkommenden Wiederholungen zurück.
   *
   * @param repetitions
   * @param count
   * @return
   */
  private Vector<Integer> distancesMostFrequentRepetitions(HashMap<List<Integer>, Vector<Integer> > repetitions, int count){
    List<Vector<Integer>> vals = new ArrayList<Vector<Integer>>();
    vals.addAll(repetitions.values());
    Comparator<Vector<Integer>> comparator = new SizeComparator();
    java.util.Collections.sort( vals, comparator );

    Vector<Integer> dist = new Vector<Integer>();
    for(int i = 0; i < count; i++){
      dist.addAll(vals.get(i));
    }

    return dist;
  }

  /**
   * Berechnet der ggT der /count/ am häufigsten vorkommenden
   * Wiederholungen.
   *
   * @param repetitions
   * @param count
   * @return
   */
  private int gcdDistances(HashMap<List<Integer>, Vector<Integer> > repetitions, int count){
    return gcd(distancesMostFrequentRepetitions(repetitions, count));
  }

  /**
   * Berechnet der ggT der /count/ am häufigsten vorkommenden
   * Wiederholungen. Ignoriert dabei ungewöhnliche Abstände, die
   * teilerfremd zu allen anderen sind.
   *
   * @param repetitions
   * @param count
   * @return
   */
  private int mostCommonGcdDistances(HashMap<List<Integer>, Vector<Integer> > repetitions, int count){
    return mostCommonGcd(distancesMostFrequentRepetitions(repetitions, count));
  }

  /**
   * Implementierung der Kasiski-Methode.
   *
   * Zuerst werden Wiederholungen im Text gesucht und die Abstände
   * zwischen den Auftreten bestimmt. Danach wird der ggT der
   * häufigsten Abstände bestimmt.
   *
   * @param text Text, der untersucht werden soll.
   * @param top Zahl der am häufigsten vorkommenden Wiederholungen, die untersucht werden soll.
   * @param ignoreUncommon Gibt an, ob Abstände, die teilerfremd zu anderen sind, ignoriert werden sollen.
   * @return ggT der Abstände der häufigsten Wiederholungen
   */
  private int gcdKasiski(ArrayList<Integer> text, int top, boolean ignoreUncommon){
    HashMap<List<Integer>, Vector<Integer> > repetitions = getRepetitions(text);
    if(ignoreUncommon){
      return mostCommonGcdDistances(repetitions, top);
    }
    else{
      return gcdDistances(repetitions, top);
    }
  }
  
  /**
   * Berechnet alle Teiler von k.
   *
   * @param k
   * @return
   */
  private HashSet<Integer> factors(int k){
    HashSet<Integer> fs = new HashSet<Integer>();
    int f = 1;
    while(!fs.contains(f)){
      if(k % f == 0){
        fs.add(f);
        fs.add(k / f);
      }
    }
    return fs;
  }

  // Implementierung von Friedman-Test

  /**
   * @param unigrams Tabelle mit Häufigkeiten im Alphabet.
   * @return Summe über alle Quadrate der Buchstabenhäufigkeiten im Alphabet.
   */
  private double getNu(ArrayList<NGram> unigrams) {
    Iterator<NGram> it = unigrams.iterator();
    double nu = 0;
    while (it.hasNext()) {
      double pi = it.next().getFrequency() / 100.0;
      nu += pi * pi;
    }
    return nu;
  }

  /**
   * Berechnet den Koinzidenzindex eines Textes.
   *
   * @param quantities
   * @param numberOfCharsInChiffreText
   * @return Variable IC
   */
  private double getIC(HashMap<Integer, Integer> quantities,
      int numberOfCharsInChiffreText) {
    double IC_numerator = 0;
    for (Integer i : quantities.values()) {
      IC_numerator += i * (i - 1);
    }
    return IC_numerator
        / (numberOfCharsInChiffreText * (numberOfCharsInChiffreText - 1));
  }

  /**
   * Berechnet den Erwartungswert des Koninzidenzindex E(IC) zu einer gegebenen Periode und
   * einer Verteilung in einem Alphabet.
   *
   * @param quantities
   * @param numberOfCharsInChiffreText
   * @return Variable IC
   */
  @SuppressWarnings("unused")
  private double getExpectedIC(ArrayList<NGram> unigrams, int numberOfCharsInChiffreText, int period) {
    double nu = getNu(unigrams);
    int N = numberOfCharsInChiffreText;
    double textPart = (1.0 / period) * ((N - period) / (N - 1.0)) * nu;
    double alphabetPart = ((period - 1.0) / period) * (N / (N - 1.0)) * (1.0 / unigrams.size());
    return textPart + alphabetPart;
  }

  /**
   * Schätzt die Periode der Vigenere-Chiffre anhand eines Chiffretextes.
   *
   * @param alphabetSize
   * @param nu
   * @param numberOfCharsInChiffreText
   * @param IC
   * @return
   */
  private double guessPeriod(int alphabetSize, double nu,
      int numberOfCharsInChiffreText, double IC) {
    int N = numberOfCharsInChiffreText;
    double numerator = (nu - (1.0 / alphabetSize)) * N;
    double denominator = (N - 1) * IC - (1.0 / alphabetSize) * N + nu;
    return numerator / denominator;
  }

  /**
   * Analysiert den durch den Reader <code>ciphertext</code> gegebenen
   * Chiffretext, bricht die Chiffre bzw. unterstützt das Brechen der Chiffre
   * (ggf. interaktiv) und schreibt den Klartext mit dem Writer
   * <code>cleartext</code>.
   * 
   * @param ciphertext
   *          Der Reader, der den Chiffretext liefert.
   * @param cleartext
   *          Der Writer, der den Klartext schreiben soll.
   */
  public void breakCipher(BufferedReader ciphertext, BufferedWriter cleartext) {
    try {
      // Einlesen der Daten der Häufigkeitstabelle. Je nachdem, ob der benutzte
      // Zeichensatz durch Angabe eines Modulus oder durch Angabe eines
      // Alphabets definiert wurde, wird auf unterschiedliche Tabellen
      // zugegriffen.
      // 'nGrams' nimmt die Daten der Häufigkeitstabelle auf.
      ArrayList<NGram> nGrams = FrequencyTables.getNGramsAsList(1, charMap);
      double nu = getNu(nGrams);
      System.out.println("Nu=" + nu);
      // Bestimme das häufigste Zeichen aus der zugehörigen Unigramm-Tabelle.
      System.out.println("Häufigstes Zeichen in der Unigramm-Tabelle: \""
          + nGrams.get(0).getCharacters() + "\"");
      // Bestimme das häufigste Zeichen des Chiffretextes.
      // 'character' ist die Integer-Repräsentation eines Zeichens.
      int character;
      // 'number' zählt alle Zeichen im Chiffretext.
      int number = 0;
      // 'quantities' enthält zu allen aufgetretenen Zeichen (keys der Hashmap)
      // deren zugehörige Anzahlen (values der Hashmap).
      HashMap<Integer, Integer> quantities = new HashMap<Integer, Integer>();

      ArrayList<Integer> text = new ArrayList<Integer>();
      // Lese zeichenweise aus der Chiffretextdatei, bis das Dateiende erreicht
      // ist.
      while ((character = ciphertext.read()) != -1) {
        number++;
        // Bilde 'character' auf dessen interne Darstellung ab.
        character = charMap.mapChar(character);
        // Zeichen speichern
        text.add(character);
        // Erhöhe die Anzahl für dieses Zeichen bzw. lege einen neuen Eintrag
        // für dieses Zeichen an.
        if (quantities.containsKey(character)) {
          quantities.put(character, quantities.get(character) + 1);
        } else {
          quantities.put(character, 1);
        }
      }
      ciphertext.close();


      // Explizites ausrechnen des Koinzidenzindex und seinem Erwartungswert
      //System.out.println("IC = " + getIC(quantities, number));
      //System.out.println("d=4 ⇒ E(IC) = " + getExpectedIC(nGrams, number, 4));
      //System.out.println("d=5 ⇒ E(IC) = " + getExpectedIC(nGrams, number, 5));
      //System.out.println("d=6 ⇒ E(IC) = " + getExpectedIC(nGrams, number, 6));
      //System.out.println("d=7 ⇒ E(IC) = " + getExpectedIC(nGrams, number, 7));
      //System.out.println("d=8 ⇒ E(IC) = " + getExpectedIC(nGrams, number, 8));

      // Schätze Periode
      double d = guessPeriod(nGrams.size(), nu, number,
          getIC(quantities, number));
      System.out.println("Geschätzte Periode: " + d);

      // Kasiski-Methode
      int gcdDists = gcdKasiski(text, 5, false);
      System.out.println("ggT der Abstände der am 5 häufigsten aufgetretenen Wiederholungen (mit mehr als 3 Zeichen): " + gcdDists);
      System.out.println(" ⇒ mögliche Perioden: " + factors(gcdDists));
      //int gcdDistsNoUncommon = gcdKasiski(text, 5, true);
      //System.out.println("ggT der Abstände der am 5 häufigsten aufgetretenen Wiederholungen (mit mehr als 3 Zeichen), die nicht teilerfremd zu den anderen sind: " + gcdDistsNoUncommon);

      // Suche das häufigste Zeichen in 'quantities'.
      // 'currKey' ist der aktuell betrachtete Schlüssel der Hashmap (ein
      // Zeichen des Chiffretextalphabets).
      int currKey = -1;
      // Der Wert zum aktuellen Schlüssel (die Anzahl, mit der 'currKey' im
      // Chiffretext auftrat).
      int currValue = -1;
      // Die bisher größte Anzahl.
      int greatest = -1;
      // Das bisher häufigste Zeichen.
      int mostFrequented = -1;
      Iterator<Integer> it = quantities.keySet().iterator();
      while (it.hasNext()) {
        currKey = it.next();
        currValue = quantities.get(currKey);
        if (currValue > greatest) {
          greatest = currValue;
          mostFrequented = currKey;
        }
      }
      // Das häufigste Zeichen 'mostFrequented' des Chiffretextes muß vor der
      // Ausgabe noch in Dateikodierung konvertiert werden.
      System.out.println("Häufigstes Zeichen im Chiffretext: "
          + (char) charMap.remapChar(mostFrequented));

      // Berechne die im Chiffretext verwendete Verschiebung.
      int computedShift = mostFrequented
          - charMap.mapChar(Integer.parseInt(nGrams.get(0).getIntegers()));
      if (computedShift < 0) {
        computedShift += modulus;
      }
      // shift = computedShift;
      // System.out.println("Schlüssel ermittelt.");
      // System.out.println("Modulus: " + modulus);
      // System.out.println("Verschiebung: " + shift);

    } catch (IOException e) {
      System.err.println("Abbruch: Fehler beim Lesen aus der "
          + "Chiffretextdatei.");
      e.printStackTrace();
      System.exit(1);
    }

  }

  /**
   * Entschlüsselt den durch den Reader <code>ciphertext</code> gegebenen
   * Chiffretext und schreibt den Klartext mit dem Writer <code>cleartext</code>
   * .
   * 
   * @param ciphertext
   *          Der Reader, der den Chiffretext liefert.
   * @param cleartext
   *          Der Writer, der den Klartext schreiben soll.
   */
  public void decipher(BufferedReader ciphertext, BufferedWriter cleartext) {
    // Kommentierung analog 'encipher(cleartext, ciphertext)'.
    try {
      int character;
      int vigenereState = 0;
      while ((character = ciphertext.read()) != -1) {
        character = charMap.mapChar(character);

        if (character != -1) {
          character = (character + modulus - shifts[vigenereState]) % modulus;
          character = charMap.remapChar(character);
          cleartext.write(character);
          vigenereState = (vigenereState + 1) % numberOfShifts;
        } else {
          // Ein überlesenes Zeichen sollte bei korrekter Chiffretext-Datei
          // eigentlich nicht auftreten können.
          System.err.println("Fehler: Unbekanntes Zeichen im Chiffretext!");
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
   * Verschlüsselt den durch den Reader <code>cleartext</code> gegebenen
   * Klartext und schreibt den Chiffretext mit dem Writer
   * <code>ciphertext</code>.
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
      // 'character' ist die Integer-Repräsentation eines Zeichens.
      int character;
      // 'characterSkipped' zeigt an, daß ein aus der Klartextdatei gelesenes
      // Zeichen mit dem gewählten Alphabet nicht abgebildet werden konnte.
      boolean characterSkipped = false;
      // Lese zeichenweise aus der Klartextdatei, bis das Dateiende erreicht
      // ist. Der Buchstabe a wird z.B. als ein Wert von 97 gelesen.
      int vigenereState = 0;
      while ((character = cleartext.read()) != -1) {
        // Bilde 'character' auf dessen interne Darstellung ab, d.h. auf einen
        // Wert der Menge {0, 1, ..., Modulus - 1}. Ist z.B. a der erste
        // Buchstabe des Alphabets, wird die gelesene 97 auf 0 abgebildet:
        // mapChar(97) = 0.
        character = charMap.mapChar(character);
        if (character != -1) {
          character = (character + shifts[vigenereState]) % modulus;
          character = charMap.remapChar(character);
          ciphertext.write(character);
          vigenereState = (vigenereState + 1) % numberOfShifts;
        } else {
          // Das gelesene Zeichen ist im benutzten Alphabet nicht enthalten.
          characterSkipped = true;
        }
      }
      if (characterSkipped) {
        System.out.println("Warnung: Mindestens ein Zeichen aus der "
            + "Klartextdatei ist im Alphabet nicht\nenthalten und wurde "
            + "überlesen.");
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
   * Erzeugt einen neuen Schlüssel.
   * 
   * @see #readKey readKey
   * @see #writeKey writeKey
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
    // Anzahl abfragen
    accepted = false;
    do {
      try {
        System.out.print("Geben Sie die Anzahl der Verschiebungen ein: ");
        numberOfShifts = Integer.parseInt(standardInput.readLine());
        if (numberOfShifts > 0) {
          accepted = true;
        } else {
          System.out.println("Geben Sie eine korrekte Anzahl ein!");
        }
      } catch (NumberFormatException e) {
        System.out.println("Fehler beim Parsen der Anzahl.");
      } catch (IOException e) {
        System.err
            .println("Abbruch: Fehler beim Lesen von der Standardeingabe.");
        e.printStackTrace();
        System.exit(1);
      }
    } while (!accepted);
    // shifts einlesen
    shifts = new int[numberOfShifts];
    for (int i = 0; i < numberOfShifts; i++) {
      accepted = false;
      do {
        try {
          System.out.print("Geben Sie die " + i + ". Verschiebung ein: ");
          shifts[i] = Integer.parseInt(standardInput.readLine());
          if (shifts[i] >= 0 && shifts[i] < modulus) {
            accepted = true;
          } else {
            System.out.println("Diese Verschiebung ist nicht geeignet. Bitte "
                + "korrigieren Sie Ihre Eingabe.");
          }
        } catch (NumberFormatException e) {
          System.out.println("Fehler beim Parsen der Verschiebung. Bitte "
              + "korrigieren Sie Ihre Eingabe.");
        } catch (IOException e) {
          System.err
              .println("Abbruch: Fehler beim Lesen von der Standardeingabe.");
          e.printStackTrace();
          System.exit(1);
        }
      } while (!accepted);
    }
  }

  /**
   * Liest den Schlüssel mit dem Reader <code>key</code>.
   * 
   * @param key
   *          Der Reader, der aus der Schlüsseldatei liest.
   * @see #makeKey makeKey
   * @see #writeKey writeKey
   */
  public void readKey(BufferedReader key) {
    try {
      StringTokenizer st = new StringTokenizer(key.readLine(), " ");
      modulus = Integer.parseInt(st.nextToken());
      System.out.println("Modulus: " + modulus);
      numberOfShifts = Integer.parseInt(st.nextToken());
      System.out.println(numberOfShifts + " Verschiebungen: ");
      shifts = new int[numberOfShifts];
      for (int i = 0; i < numberOfShifts; i++) {
        shifts[i] = Integer.parseInt(st.nextToken());
        System.out.println(" " + shifts[i]);
      }
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
   * <p>
   * Der Modulus und die Verschiebung werden durch ein Leerzeichen getrennt in
   * die Schlüsseldatei geschrieben. Eine solche Schlüsseldatei hat also das
   * folgende Format:
   * 
   * <pre style="background-color:#f0f0f0; border:1pt silver solid; * padding:3px">
   * modulus numberOfShifts shift0 shift1 ...
   * </pre>
   * 
   * </p>
   * 
   * @param key
   *          Der Writer, der in die Schlüsseldatei schreibt.
   * @see #makeKey makeKey
   * @see #readKey readKey
   */
  public void writeKey(BufferedWriter key) {

    try {
      key.write(modulus + " " + numberOfShifts);
      for (int i = 0; i < numberOfShifts; i++) {
        key.write(" " + shifts[i]);
      }
      key.newLine();
      key.close();
    } catch (IOException e) {
      System.out.println("Abbruch: Fehler beim Schreiben oder Schließen der "
          + "Schlüsseldatei.");
      e.printStackTrace();
      System.exit(1);
    }
  }
}
