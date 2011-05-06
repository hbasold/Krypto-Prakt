package task2;

import de.tubs.cs.iti.jcrypt.chiffre.CharacterMapping;

/**
 * Data class for a letter in an alphabet to store
 * associated information.
 * @see Quantities
 */
public class Quantity implements Comparable<Quantity> {

  private int[] integers;
  private double frequency;
  private int position;
  private Quantity key;
  private double weight;

  /**
   * Constructor for letters with the most information.
   * @param integers The list of integers representing the letter beginning with 0.
   * @param relativeFrequency The relative frequency of this letter in a text.
   * @param position The position of this quantity in a text.
   */
  public Quantity(int[] integers, double relativeFrequency, int position) {
    this.integers = integers;
    this.frequency = relativeFrequency;
    this.position = position;
  }
  /**
   * Constructor for letters with the most information.
   * Default of position is 0.
   * @see Quantity#Quantity(int[], double, int)
   */
  public Quantity(int[] integers, double relativeFrequency) {
    this(integers, relativeFrequency, 0);
  }
  /**
   * Constructor for letters with the most information.
   * @see Quantity#Quantity(int[], double, int)
   */
  public Quantity(int integer, double relativeFrequency) {
    this(new int[1], relativeFrequency, 0);
    integers[0] = integer;
  }
  /**
   * Constructor for letters with position.
   * @see Quantity#Quantity(int[], double, int)
   */
  public Quantity(int integer, int position) {
    this(new int[1], 0, position);
    integers[0] = integer;
  }
  /**
   * Constructor for letters.
   * @see Quantity#Quantity(int[], double, int)
   */
  public Quantity(int integer) {
    this(new int[1], 0, 0);
    integers[0] = integer;
  }
  /**
   * Constructor for letters, where there is no information about
   * the relative frequency.
   * @see Quantity#Quantity(int, int, double)
   */
  public Quantity() {
    this(new int[1], 0, 0);
  }
  /**
   * @return Same as integer in constructor.
   * @see Quantity#Quantity(int, int, double)
   */
  public int getInt() {
    return integers[0];
  }
  /**
   * @param index The index in the list of integers.
   * @return The value in the list of integers at the given index,
   *         which is given in the constructor.
   * @see Quantity#Quantity(int[], int, double)
   */
  public int getInt(int i) {
    return integers[i];
  }
  /**
   * @return The list of integers given in the constructor.
   * @see Quantity#Quantity(int[], int, double)
   */
  public int[] getIntegers() {
    return integers;
  }
  /**
   * @return The length of the integers array given in the constructor.
   */
  public int size() {
    return integers.length;
  }
  /**
   * @return Same as relativeFrequency in constructor.
   * @see Quantity#Quantity(int, int, double)
   */
  public double getRelativeFrequency() {
    return frequency;
  }
  /**
   * @return The position in the text, if given in the constructor, otherwise false.
   */
  public int getPosition() {
    return position;
  }
  public Quantity getKey() {
    return key;
  }
  public double getWeight() {
    return weight;
  }
  /**
   * Sort quantity in >>descending<< order by size followed by frequency.
   * @return 0 if quantity of this object and the quantity of the given object are equal.
   * A positive integer if the quantity of this object is less
   * than the quantity of the given object, a negative integer otherwise.
   */
  @Override
  public int compareTo(Quantity o) {
    int result = o.integers.length - integers.length;
    if (result==0) {
      result = (int) Math.signum(o.frequency - frequency);
    }
    return result;
  }
  
  public boolean equals(int[] integers) {
    for (int i=0; i<this.integers.length; i++) {
      if (this.integers[i]!=integers[i]) {
        return false;
      }
    }
    return true;
  }
  
  public int getShift(Quantity key, int modulus) {
    return (integers[0] - key.integers[0] + modulus) % modulus;
  }

  /**
   * (p+k) % m = e
   * p = (e-k+m) % m
   * @param key
   * @param modulus
   * @return
   */
  public Quantity decryptWithKey(Quantity key, int modulus) {
    int[] newIntegers = new int[key.size()];
    for (int i=0; i<key.size(); i++) {
      newIntegers[i] = (integers[i] - key.integers[i] + modulus) % modulus;
    }
    return new Quantity(newIntegers, 0);
  }
  
  private static int decryptWithKey(int encrypted, int key, int modulus) {
    return (encrypted - key + modulus) % modulus;
  }
  public static Quantity decryptWithKey(Quantities quantities, int start, Quantity key, int modulus) {
    Quantity plain = new Quantity(new int[key.size()], 0, start);
    for (int i = 0; i < key.size(); i++) {
      plain.integers[i] = decryptWithKey(quantities.get(start+i).integers[0], key.integers[i], modulus);
    }
    return plain;
  }
  
  @Override
  public String toString() {
    if (integers.length==1) {
      return Integer.toString(integers[0]);
    } else {
      StringBuffer sb = new StringBuffer();
      sb.append(integers[0]);
      for (int i=1; i<integers.length; i++) {
        sb.append(' ');
        sb.append(integers[i]);
      }
      return sb.toString();
    }
  }
  public String remap(CharacterMapping charMap) {
    char[] cs = new char[getIntegers().length];
    int i = 0;
    for (int q : getIntegers()) {
      cs[i] = (char) charMap.remapChar(q);
      if (cs[i]<32) {
        cs[i] = '#';
      }
      i++;
    }
    return new String(cs);
  }
  public void copyKeyFromAndCalculateWeight(Quantity newKey) {
    key = new Quantity(newKey.integers, newKey.frequency, newKey.position);
    key.position = position;
    key.key = this;
    weight = frequency * key.frequency;
    key.weight = weight;
  }
  public void copyFrequencyFrom(Quantity matchingTriGram) {
    frequency = matchingTriGram.frequency;
  }
  public boolean hasDirectOverlapping(Quantity q) {
    if (position<=q.position) { // sicherstellen, dass diese Zeichen immer links von den gegebene Zeichen stehen
      int delta = q.position - position;
      if (integers.length<=delta) {
        return false;
      } else {
        for (int i=0; i<integers.length-delta; i++) {
          if (integers[delta+i]!=q.integers[i]) {
            return false;
          }
        }
        return true;
      }
    }
    return q.hasDirectOverlapping(this); // Gleiche Methode mit vertauschten Argumenten aufrufen
  }
  public void swapValues(Quantity q) {
    int[] tmpIntegers = integers;
    integers = q.integers;
    q.integers = tmpIntegers;
    double tmpDouble = frequency;
    frequency = q.frequency;
    q.frequency = tmpDouble;
    int tmpInt = position;
    position = q.position;
    q.position = tmpInt;
    tmpDouble = weight;
    weight = q.weight;
    q.weight = tmpDouble;
    Quantity tmpQuantity = key;
    key = q.key;
    q.key = tmpQuantity;
  }
  public void swapPlainAndKey() {
    swapValues(getKey());
  }

}
