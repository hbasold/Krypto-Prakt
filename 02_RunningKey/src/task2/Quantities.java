package task2;

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Vector;

import de.tubs.cs.iti.jcrypt.chiffre.CharacterMapping;
import de.tubs.cs.iti.jcrypt.chiffre.FrequencyTables;
import de.tubs.cs.iti.jcrypt.chiffre.NGram;

public class Quantities extends Vector<Quantity> {

  private static final long serialVersionUID = 7918874849850308089L;

  private final int modulus;
  
  private int countAllChars;

  /**
   * Constructor for an alphabet quantities on chars.
   * @param languageQuantities The language specific alphabet quantities.
   * @see #createLanguageQuantities(CharacterMapping)
   */
  public Quantities(int modulus) {
    super();
    this.modulus = modulus;
  }

  /**
   * Static method to create quantities for a language specific alphabet.
   * @param n Number of chars in one NGram
   * @param charMap The used character mapping which is associated
   *    with the specific language. 
   * @return List of {@link Quantity} for the language specified by the given charMap.
   */
  public static Quantities createLanguageQuantities(int n, CharacterMapping charMap, int modulus) {
    // Einlesen der Daten der Häufigkeitstabelle. Je nachdem, ob der benutzte
    // Zeichensatz durch Angabe eines Modulus oder durch Angabe eines
    // Alphabets definiert wurde, wird auf unterschiedliche Tabellen
    // zugegriffen.
    // 'nGrams' nimmt die Daten der Häufigkeitstabelle auf.
    System.out.println("Unigramm-Tabelle beginnend mit den häufigsten:");
    ArrayList<NGram> nGrams = FrequencyTables.getNGramsAsList(n, charMap);
    Quantities quantities = new Quantities(modulus);
    for (NGram ngram: nGrams) {
//      System.out.println("cs="+n.getCharacters()+" is="+n.getIntegers()+" f="+n.getFrequency()+" mapto="+charMap.mapChar(Integer.parseInt(n.getIntegers())));
      Quantity q;
      StringTokenizer st = new StringTokenizer(ngram.getIntegers(), "_");
      int[] integers = new int[st.countTokens()];
      for (int i = 0; st.hasMoreElements(); i++) {
        integers[i] = charMap.mapChar(Integer.parseInt(st.nextToken()));
      }
      q = new Quantity(integers, ngram.getFrequency() / 100.0);
      quantities.add(q);
    }
    return quantities;
  }

  /**
   * Adds the quantity of a letter and counts all chars ever added.
   * @see Quantity#getCount()
   */
  @Override
  public synchronized boolean add(Quantity q) {
    countAllChars += q.getIntegers().length;
    return super.add(q);
  }

  public boolean add(int character) {
    countAllChars++;
    return super.add(new Quantity(character));
  }

  public void addNGrammAsUniGramms(Quantity quantity) {
    for (int c: quantity.getIntegers()) {
      add(new Quantity(c));
    }
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

  public Quantity findQuantity(Quantity plain) {
    for (Quantity q: this) {
      if (q.equals(plain.getIntegers())) {
        return q;
      }
    }
    return null;  }

  public Quantity findQuantityWithInteger(int shift) {
    for (Quantity q: this) {
      if (q.getInt()==shift) {
        return q;
      }
    }
    return null;
  }

  public Quantity findQuantityWithIntegers(int[] integers) {
    for (Quantity q: this) {
      if (q.equals(integers)) {
        return q;
      }
    }
    return null;
  }

  public boolean containsSequence(Quantities plain) {
    for (Quantity q: this) {
      boolean equals = true;
      int[] integers = q.getIntegers();
      for (int i=0; i<integers.length; i++) {
        if (integers[i]!=q.getInt()) {
          equals = false;
          break;
        }
      }
      if (equals) {
        return true;
      }
    }
    return false;
  }

  public Quantity decryptWithKey(int start, Quantity key) {
    return Quantity.decryptWithKey(this, start, key, modulus);
  }

  public Quantities decryptWithKey(Quantities key, int start, int end) {
    if(!(size() > start && end <= size() && key.size() > 0 && (end - start) <= key.size())){
      System.err.println("decrypt error: " + this + " " + key);
    }
    Quantities qs = new Quantities(modulus);
    for (int i = 0; i < (end - start); i++) {
      qs.add(get(i + start).decryptWithKey(key.get(i), modulus));
    }
    return qs;
  }

  public Vector<Quantity> decryptWithKey(Vector<Quantity> key, int start, int end) {
    if(!(size() > start && end <= size() && key.size() > 0 && (end - start) <= key.size())){
      System.err.println("decrypt error (" + start + ", " + end + "):" + this + " " + key);
    }
    Vector<Quantity> qs = new Vector<Quantity>();
    for (int i = 0; i < (end - start); i++) {
      qs.add(get(i + start).decryptWithKey(key.get(i), modulus));
    }
    return qs;
  }

  public Quantities decryptWithKey(Quantities key) {
    return decryptWithKey(key, 0, size());
  }

  public String remap(CharacterMapping charMap) {
    StringBuffer sb = new StringBuffer();
    for (Quantity q: this) {
      sb.append(q.remap(charMap));
    }
    return sb.toString();
  }


}