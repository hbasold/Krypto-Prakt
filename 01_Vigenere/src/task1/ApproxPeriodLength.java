package task1;

import java.util.Iterator;
import java.util.Vector;

/**
 *  Implementierung des Friedman-Test
 */

public class ApproxPeriodLength {

  /**
   * @param ngrams Tabelle mit Häufigkeiten im Alphabet.
   * @return Summe über alle Quadrate der Buchstabenhäufigkeiten im Alphabet.
   */
  private static double getNu(Quantities ngrams) {
    Iterator<Quantity> it = ngrams.iterator();
    double nu = 0;
    while (it.hasNext()) {
      double pi = it.next().getRelativeFrequency() / 100.0;
      nu += pi * pi;
    }
    return nu;
  }

  /**
   * Berechnet den Koinzidenzindex eines Textes.
   * @param quantities Häufigkeiten der Buchstaben
   * @param numberOfCharsInChiffreText N
   * @return Variable IC
   */
  private static double getIC(Quantities quantities,
      int numberOfCharsInChiffreText) {
    double IC_numerator = 0;
    for (Quantity q: quantities) {
      int i = q.getCount();
      IC_numerator += i * (i - 1);
    }
    return IC_numerator
        / (numberOfCharsInChiffreText * (numberOfCharsInChiffreText - 1));
  }

  /**
   * Schätzt die Periode der Caesar-Chiffre anhand eines Chiffretextes.
   * @param languageQuantities Liste mit relativen Haufigkeiten der Buchstaben in der Sprache.
   * @param quantities List mit relativen Häufigkeiten des Buchstaben im aktuellen Text.
   * @return Geschätze Periodenlänge.
   */
  private static double guessPeriod(
        Quantities languageQuantities,
        Quantities quantities)
  {
    double nu = getNu(languageQuantities);
    int alphabetSize = languageQuantities.size();
    int N = quantities.getCountAllChars();
    double IC = getIC(quantities, N);
    double numerator = (nu - (1.0 / alphabetSize)) * N;
    double denominator = (N - 1) * IC - (1.0 / alphabetSize) * N + nu;
    return numerator / denominator;
  }

  /**
   * Schätzt die Periode der Vigenere-Chiffre anhand eines Chiffretextes.
   * @param languageQuantities Liste mit relativen Haufigkeiten der Buchstaben in der Sprache.
   * @param numberOfCharsInChiffreText N.
   * @param quantities List mit relativen Häufigkeiten des Buchstaben im aktuellen Text.
   * @return Geschätze Periodenlänge.
   */
  public static double guessPeriod(Quantities languageQuantities,
        Vector<Quantities> vectorOfQuantities)
  {
    double sum = 0;
    int periodLength = vectorOfQuantities.size();
    for (Quantities qs: vectorOfQuantities) {
      sum += ApproxPeriodLength.guessPeriod(languageQuantities, qs);
    }
    return sum / periodLength;
  }

  /**
   * Berechnet den Erwartungswert des Koinzidenzindex E(IC) zu einer gegebenen Periode und
   * einer Verteilung in einem Alphabet.
   *
   * @param quantities
   * @param numberOfCharsInChiffreText
   * @return Variable IC
   */
  public static double getExpectedIC(Quantities languageQuantities, int numberOfCharsInChiffreText, int period) {
    double nu = getNu(languageQuantities);
    int N = numberOfCharsInChiffreText;
    double textPart = (1.0 / period) * ((N - period) / (N - 1.0)) * nu;
    double alphabetPart = ((period - 1.0) / period) * (N / (N - 1.0)) * (1.0 / languageQuantities.size());
    return textPart + alphabetPart;
  }

}