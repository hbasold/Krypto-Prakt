package task1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import de.tubs.cs.iti.jcrypt.chiffre.CharacterMapping;

public class VigenereBreak {

  private final CharacterMapping charMap;
  private final Quantities languageQuantities;
  private final ArrayList<Integer> text;
  private final int modulus;
  
  private Vector<Quantities> vSortedQuantities;

  public VigenereBreak(CharacterMapping charMap, ArrayList<Integer> text, int modulus) {
    this.charMap = charMap;
    languageQuantities = Quantities.createLanguageQuantities(charMap, modulus);
    this.text = text;
    this.modulus = modulus;
  }
  
  /**
   * @param numberOfShifts The number of shifts.
   * @return Vector of a hash-map defining the integer of a char as the key
   *    and the quantity of this char as the value.
   */
  private Vector<HashMap<Integer, Integer> > getVectorOfQuantities(int numberOfShifts) {
    // create vector of quantities
    Vector<HashMap<Integer, Integer> > vQuantities = new Vector<HashMap<Integer, Integer> >(numberOfShifts);
    for (int i=0; i<numberOfShifts; i++) {
      HashMap<Integer, Integer> quantities = new HashMap<Integer, Integer>();
      vQuantities.add(quantities);
    }
    // count characters and add it to current shift
    int iShift = 0;
    for (Integer charInt: text) {
      HashMap<Integer, Integer> quantities = vQuantities.get(iShift);
      if (quantities.containsKey(charInt)) { // Zeichen bereits vorhanden
        quantities.put(charInt, quantities.get(charInt) + 1); // Anzahl erhöhen
      } else { // Zeichen noch nicht vorhanden
        quantities.put(charInt, 1);
      }
      iShift = (iShift + 1) % numberOfShifts;
    }
    return vQuantities;
  }

  /**
   * Use the text with a given length of period to calculate the
   * quantities of the chars separately for each list of quantities.
   * @param numberOfShifts The number of shifts.
   * @return Sorted list of quantities. Order is specified by
   * {@link Quantity#compareTo(Quantity)}.
   */
  public void sortByQuantities(int numberOfShifts) {
    Vector<HashMap<Integer, Integer> > vQuantities = getVectorOfQuantities(numberOfShifts);
    // create vector of NGrams
    vSortedQuantities = new Vector<Quantities>(numberOfShifts);
    for (HashMap<Integer, Integer> quantities: vQuantities) {
      Quantities sortedQuantities = new Quantities(languageQuantities, modulus);
      for (Integer key: quantities.keySet()) {
        sortedQuantities.add(new Quantity(key, quantities.get(key)));
      }
      java.util.Collections.sort(sortedQuantities); // implicit call of compareTo
      vSortedQuantities.add(sortedQuantities);
      sortedQuantities.calculateShifts();
    }
  }

  /**
   * Calls {@link Quantities#sortByChangingNeighbours()} for each list
   * of quantities.
   */
  public void sortByChangingNeighbours() {
    for (Quantities qs: vSortedQuantities) {
      qs.sortByChangingNeighbours();
    }
  }

  /**
   * @return List of all shift frequencies.
   */
  private Vector<Vector<Pair<Integer, Quantity>>> getListOfShiftFrequencies() {
    Vector<Vector<Pair<Integer, Quantity>>> shifts = new Vector<Vector<Pair<Integer, Quantity>>>(vSortedQuantities.size());
    for (Quantities qs: vSortedQuantities) {
      shifts.add(qs.getShiftFrequencies());
    }
    return shifts;
  }

  /**
   * Selects the first shift in each list of quantities to get a default result.
   * @return Array of the best shifts.
   */
  public int[] getBestShifts() {
    Vector<Vector<Pair<Integer, Quantity>>> vShifts = getListOfShiftFrequencies();
    int[] shifts = new int[vSortedQuantities.size()];
    for (int i=0; i<vSortedQuantities.size(); i++) {
      shifts[i] = vShifts.get(i).get(0).second.getShift();
    }
    return shifts;
  }

  /**
   * Prints a table with the relative frequencies of each letter
   * in descending order to console window.
   */
  public void printQuantities() {
    final int nMax = modulus/2;
    System.out.printf("     ");
    for (int n=0; n<nMax; n++) {
      Quantity q = languageQuantities.get(n);
      System.out.printf(" | %1s=%3s       : %3.1f%% ",
          remapToString(q.getInt()), q.getInt(), q.getRelativeFrequency());
    }
    System.out.println();
    for (int i=0; i<vSortedQuantities.size(); i++) {
      Quantities qs = vSortedQuantities.get(i);
      System.out.printf("i =%2d", i);
      for (int n=0; n<nMax; n++) {
        Quantity q = qs.get(n); 
        System.out.printf(" | %1s=%3d (+%3d): %3.1f%% ",
            remapToString(q.getInt()), q.getInt(), q.getShift(), q.getRelativeFrequency());
      }
      System.out.println();
    }
  }
  private String remapToString(int charInt) {
    return CharacterMapping.convertToString(charMap.remapChar(charInt));
  }

  /**
   * Prints a table with the shifts and their frequencies to console window.
   */
  public void printShifts() {
    System.out.println("Mögliche Shifts sortiert nach Häufigkeit pro Teiltext:");
    Vector<Vector<Pair<Integer, Quantity>>> vShifts = getListOfShiftFrequencies();
    int i = 0;
    for (Vector<Pair<Integer, Quantity>> vPair: vShifts) {
      System.out.printf("i = %2d", i);
      for (Pair<Integer, Quantity> p: vPair) {
        System.out.printf(" | %3d: %3dx", p.second.getShift(), p.first);
      }
      System.out.println();
      i++;
    }
  }

}
