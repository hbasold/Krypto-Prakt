package task1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import de.tubs.cs.iti.jcrypt.chiffre.NGram;

/**
 *  Implementierung des Friedman-Test
 */

public class FriedmannTest {

  /**
   * @param unigrams Tabelle mit Häufigkeiten im Alphabet.
   * @return Summe über alle Quadrate der Buchstabenhäufigkeiten im Alphabet.
   */
  private static double getNu(ArrayList<NGram> unigrams) {
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
   * @param quantities Map mit Häufigkeiten der Buchstaben
   * @param numberOfCharsInChiffreText N
   * @return Variable IC
   */
  private static double getIC(HashMap<Integer, Integer> quantities,
      int numberOfCharsInChiffreText) {
    double IC_numerator = 0;
    for (Integer i : quantities.values()) {
      IC_numerator += i * (i - 1);
    }
    return IC_numerator
        / (numberOfCharsInChiffreText * (numberOfCharsInChiffreText - 1));
  }

  /**
   * Schätzt die Periode der Vigenere-Chiffre anhand eines Chiffretextes.
   * @param nGrams Liste mit N-Gramen
   * @param numberOfCharsInChiffreText N.
   * @param quantities Map mit Häufigkeiten der Buchstaben.
   * @return Geschätze Periodenlänge.
   */
  public static double guessPeriod(
        ArrayList<NGram> nGrams,
        int numberOfCharsInChiffreText,
        HashMap<Integer, Integer> quantities)
  {
    double nu = getNu(nGrams);
    int alphabetSize = nGrams.size();
    int N = numberOfCharsInChiffreText;
    double IC = getIC(quantities, N);
    double numerator = (nu - (1.0 / alphabetSize)) * N;
    double denominator = (N - 1) * IC - (1.0 / alphabetSize) * N + nu;
    return numerator / denominator;
  }

  /**
   * Berechnet den Erwartungswert des Koinzidenzindex E(IC) zu einer gegebenen Periode und
   * einer Verteilung in einem Alphabet.
   *
   * @param quantities
   * @param numberOfCharsInChiffreText
   * @return Variable IC
   */
  public static double getExpectedIC(ArrayList<NGram> nGrams, int numberOfCharsInChiffreText, int period) {
    double nu = getNu(nGrams);
    int N = numberOfCharsInChiffreText;
    double textPart = (1.0 / period) * ((N - period) / (N - 1.0)) * nu;
    double alphabetPart = ((period - 1.0) / period) * (N / (N - 1.0)) * (1.0 / nGrams.size());
    return textPart + alphabetPart;
  }

}