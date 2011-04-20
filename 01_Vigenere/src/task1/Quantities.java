package task1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import de.tubs.cs.iti.jcrypt.chiffre.CharacterMapping;
import de.tubs.cs.iti.jcrypt.chiffre.FrequencyTables;
import de.tubs.cs.iti.jcrypt.chiffre.NGram;

public class Quantities extends Vector<Quantity>
    implements Comparator<Quantity>
{

  private static final long serialVersionUID = 7918874849850308089L;

  private final Quantities languageQuantities;
  private final int modulus;
  
  private int countAllChars;
  private Vector<Pair<Integer, Integer>> guessedShifts;

  private Quantities(Quantities qs) {
    languageQuantities = qs.languageQuantities;
    this.modulus = qs.modulus;
    for (Quantity q: qs) {
      add(q);
    }
  }
  private Quantities(int size, int modulus) {
    super(size);
    languageQuantities = null;
    this.modulus = modulus;
  }

  /**
   * Static method to create a language specific alphabet quantities.
   * @param charMap The used character mapping which is associated
   *    with the specific language. 
   * @return The language specific alphabet quantities.
   */
  public static Quantities createLanguageQuantities(CharacterMapping charMap, int modulus) {
    ArrayList<NGram> nGrams = FrequencyTables.getNGramsAsList(1, charMap);
    Quantities quantities = new Quantities(nGrams.size(), modulus);
    for (NGram n: nGrams) {
//      System.out.println("cs="+n.getCharacters()+" is="+n.getIntegers()+" f="+n.getFrequency()+" mapto="+charMap.mapChar(Integer.parseInt(n.getIntegers())));
      Quantity q = new Quantity(
          charMap.mapChar(Integer.parseInt(n.getIntegers())),
          (int) n.getFrequency()*10, //dummy with one precision after point
          n.getFrequency());
      quantities.add(q);
    }
    return quantities;
  }

  /**
   * Constructor for an alphabet quantities on chars.
   * @param languageQuantities The language specific alphabet quantities.
   * @see #createLanguageQuantities(CharacterMapping)
   */
  public Quantities(Quantities languageQuantities, int modulus) {
    super(languageQuantities.size());
    this.languageQuantities = languageQuantities;
    this.modulus = modulus;
  }

  /**
   * Adds the quantity of a letter and counts all chars ever added.
   * @see Quantity#getCount()
   */
  @Override
  public synchronized boolean add(Quantity q) {
    countAllChars += q.getCount();
    return super.add(q);
  }

  /**
   * Calculate the relative frequency and the shift
   * of all letters in this vector of quantity.
   * @see Quantity#getRelativeFrequency()
   */
  public void calculateAll() {
    Iterator<Quantity> it = languageQuantities.iterator();
    for (Quantity q: this) {
      q.calculateRelativeFrequency(countAllChars);
      q.calculateAll(it.next(), modulus);
    }
    // guess shift
//    Quantities qBestFit = new Quantities(languageQuantities, modulus);
//    Collections.sort(qBestFit, this);
    int[] shiftCounts = new int[modulus];
    int relevantShifts = Math.min(size(), modulus); //10;
    for (int i=0; i<relevantShifts; i++) {
      shiftCounts[get(i).getShift()]++;
    }
    guessedShifts = getIndexOfMaxElement(shiftCounts, 3);
    Collections.sort(guessedShifts);
//    guessedShift = qBestFit.get(0).getShift();
  }
  
  private Vector<Pair<Integer, Integer>> getIndexOfMaxElement(int[] shiftCounts, int count) {
    int max[] = new int[count];
    int indexMax[] = new int[count];
    for (int i=0 ; i<shiftCounts.length; i++) {
      for(int j = 0; j < count; ++j){
        if (shiftCounts[i] > max[j]) {
          max[j] = shiftCounts[i];
          indexMax[j] = i;
          break;
        }
      }
    }
    Vector<Pair<Integer, Integer>> maxIndices = new Vector<Pair<Integer, Integer>>();
    for(int j = 0; j < count; ++j) {
      maxIndices.add(Pair.of(max[j], indexMax[j]));
    }
    return maxIndices;
  }

  public int decrypt(int index) {
    return (get(index).getInt()-get(index).getShift()+modulus) % modulus;
  }

  public Vector<Pair<Integer, Integer>> getGuessedShift() {
    return guessedShifts;
  }

  @Override
  public int compare(Quantity o1, Quantity o2) {
    return (int)(1000*(o2.getDeltaRelativeFrequency() - o1.getDeltaRelativeFrequency()));
  }

}