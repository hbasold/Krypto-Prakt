package task1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeMap;
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
  
  public String remapToString(int charInt) {
    return CharacterMapping.convertToString(charMap.remapChar(charInt));
  }
  public int remap(int charInt) {
    return charMap.remapChar(charInt);
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
   * quantities of the chars separately for each shift.
   * @param numberOfShifts The number of shifts.
   * @return Sorted list of quantities. Order is specified by
   * {@link Quantity#compareTo(Quantity)}.
   */
  public Vector<Quantities > getVectorOfSortedQuantities(int numberOfShifts) {
    Vector<HashMap<Integer, Integer> > vQuantities = getVectorOfQuantities(numberOfShifts);
    // create vector of NGrams
    vSortedQuantities = new Vector<Quantities>(numberOfShifts);
    for (HashMap<Integer, Integer> quantities: vQuantities) {
      Quantities sortedQuantities = new Quantities(languageQuantities, modulus);
      for (Integer key: quantities.keySet()) {
        sortedQuantities.add(new Quantity(key, quantities.get(key)));
      }
      java.util.Collections.sort(sortedQuantities); // implicit call of compareTo
      sortedQuantities.calculateAll();
      vSortedQuantities.add(sortedQuantities);
    }
    return vSortedQuantities;
  }

  public Quantities getLanguageQuantities() {
    return languageQuantities;
  }

  public Vector<Vector<Pair<Integer, Integer>>> getGuessedShifts() {
    Vector<Vector<Pair<Integer, Integer>>> shifts = new Vector<Vector<Pair<Integer, Integer>>>(vSortedQuantities.size());
    for (Quantities qs: vSortedQuantities) {
      shifts.add(qs.getGuessedShift());
    }
    return shifts;
  }

}
