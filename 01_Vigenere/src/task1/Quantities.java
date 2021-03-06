package task1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Vector;

import de.tubs.cs.iti.jcrypt.chiffre.CharacterMapping;
import de.tubs.cs.iti.jcrypt.chiffre.FrequencyTables;
import de.tubs.cs.iti.jcrypt.chiffre.NGram;

public class Quantities extends Vector<Quantity> {

  private static final long serialVersionUID = 7918874849850308089L;

  private final Quantities languageQuantities;
  private final int modulus;
  
  private int countAllChars;
  private Vector<Pair<Integer, Quantity>> guessedShifts;

  private Quantities(int size, int modulus) {
    super(size);
    languageQuantities = null;
    this.modulus = modulus;
  }

  /**
   * Static method to create quantities for a language specific alphabet.
   * @param charMap The used character mapping which is associated
   *    with the specific language. 
   * @return List of {@link Quantity} for the language specified by the given charMap.
   */
  public static Quantities createLanguageQuantities(CharacterMapping charMap, int modulus) {
    // Einlesen der Daten der Häufigkeitstabelle. Je nachdem, ob der benutzte
    // Zeichensatz durch Angabe eines Modulus oder durch Angabe eines
    // Alphabets definiert wurde, wird auf unterschiedliche Tabellen
    // zugegriffen.
    // 'nGrams' nimmt die Daten der Häufigkeitstabelle auf.
    System.out.println("Unigramm-Tabelle beginnend mit den häufigsten:");
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
   * @return The number of all letters in the associated text. It is the sum
   *         of all counters from each quantity given by
   *         {@link Quantity#getCount()}.
   * @see #add(Quantity)
   */
  public int getCountAllChars() {
    return countAllChars;
  }
  /**
   * Calculate the relative frequency and the shift
   * of all letters in this list of quantities.
   * The methods
   * {@link Quantity#calculateRelativeFrequency(int)} and
   * {@link Quantity#calculateShift(Quantity, int)} where called
   * for each Quantity and at the end
   * {@link #calculateShiftFrequencies()} is called.
   */
  public void calculateShiftsAndShiftFrequencies() {
    Iterator<Quantity> it = this.iterator();
    Iterator<Quantity> itLang = languageQuantities.iterator();
    while (it.hasNext() && itLang.hasNext()) {
      Quantity q = it.next();
      q.calculateRelativeFrequency(countAllChars);
      q.calculateShift(itLang.next(), modulus);
    }
    calculateShiftFrequencies();
  }

  /**
   * Calculate the frequency of equal shifts in all quantities
   * and sort the shifts by descending frequency.
   * Use {@link #getShiftFrequencies()} to get the result.
   */
  private void calculateShiftFrequencies() {
    // count shifts in an array with length of modulus
    int[] shiftCounts = new int[modulus];
    int relevantShifts = Math.min(size(), modulus); //10;
    for (int i=0; i<relevantShifts; i++) {
      shiftCounts[get(i).getShift()]++;
    }
    // create a sorted vector of pairs with counter of shift and shift-length
    guessedShifts = new Vector<Pair<Integer, Quantity>>();
    for(int i = 0; i < shiftCounts.length; i++) {
      if (shiftCounts[i]>0) {
        guessedShifts.add(Pair.of(shiftCounts[i], getFirstQuantityWithShift(i)));
      }
    }
    Collections.sort(guessedShifts); // implicit call of Pair.compareTo (descending)
  }

  /**
   * @return The frequencies of all shifts calculated by
   * {@link #calculateShiftFrequencies()}.
   */
  public Vector<Pair<Integer, Quantity>> getShiftFrequencies() {
    return guessedShifts;
  }

  /**
   * Search for a Quantity with a specific shift.
   * @param shift The shift value searching for.
   * @return The first Quantity with the given shift value.
   */
  private Quantity getFirstQuantityWithShift(int shift) {
    for (Quantity q: this) {
      if (q.getShift()==shift)
        return q;
    }
    return null;
  }

  /**
   * Swap the position of each pair in the list of quantities,
   * if they will have the same shift afterwards.
   * Automatically recalculate the frequency table of all shifts.
   * @see #calculateShiftFrequencies()
   */
  public void sortByChangingNeighbours() {
    int minSize = Math.min(size(), languageQuantities.size());
    for (int i=0; i<minSize-1; i++) {
      if (get(i).getShift()!=get(i+1).getShift()) { // shifts are different
        Collections.swap(this, i, i+1); // probe switching...
        get(i).calculateShift(languageQuantities.get(i), modulus);
        get(i+1).calculateShift(languageQuantities.get(i+1), modulus);
        if (get(i).getShift()!=get(i+1).getShift()) { // switching not successful
          Collections.swap(this, i, i+1); // switch back
          get(i).calculateShift(languageQuantities.get(i), modulus);
          get(i+1).calculateShift(languageQuantities.get(i+1), modulus);
        }
      }
    }
    calculateShiftFrequencies();
  }
  
}