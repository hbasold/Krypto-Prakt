package task1;

/**
 * Data class for a letter in an alphabet to store
 * associated information.
 * @see Quantities
 */
public class Quantity implements Comparable<Quantity> {

  private int integer;
  private int count;
  private double frequency;
  private double deltaFrequency;
  private int shift;
  
  /**
   * Constructor for letters with the most information.
   * @param integer The integer representing the letter beginning with 0.
   * @param count The counter of this letter in a text.
   * @param relativeFrequency The relative frequency of this letter in a text.
   */
  public Quantity(int integer, int count, double relativeFrequency) {
    this.integer = integer;
    this.count = count;
    this.frequency = relativeFrequency;
  }
  /**
   * Constructor for letters, where there is no information about
   * the relative frequency.
   * @see Quantity#Quantity(int, int, double)
   */
  public Quantity(int integer, int count) {
    this(integer, count, -1);
  }
  /**
   * @return Same as integer in constructor.
   * @see Quantity#Quantity(int, int, double)
   */
  public int getInt() {
    return integer;
  }
  /**
   * @return Same as counter in constructor.
   * @see Quantity#Quantity(int, int, double)
   */
  public int getCount() {
    return count;
  }
  /**
   * Calculate a new relative frequency.
   * @param countAllChars
   *    The number of chars in the text this letter is associated with.
   * @see #getRelativeFrequency()
   */
  protected void calculateRelativeFrequency(int countAllChars) {
    frequency = 100.0 * count / countAllChars;
  }
  /**
   * @return Same as relativeFrequency in constructor.
   * @see Quantity#Quantity(int, int, double)
   */
  public double getRelativeFrequency() {
    return frequency;
  }
  /**
   * @param shift The shift of this letter in an encrypted text.
   */
  public void calculateAll(Quantity languageQuantity, int modulus) {
    shift = (integer - languageQuantity.integer + modulus) % modulus;
    deltaFrequency = Math.abs(frequency - languageQuantity.frequency);
  }
  /**
   * @return The shift of this letter used for the encryption.
   */
  public int getShift() {
    return shift;
  }
  /**
   * @return The shift of this letter used for the encryption.
   */
  public double getDeltaRelativeFrequency() {
    return deltaFrequency;
  }
  
  /**
   * Sort quantity in descending order by counter.
   * @return 0 if quantity of this object and the quantity of the given object are equal.
   * A positive integer if the quantity of this object is less
   * than the quantity of the given object, a negative integer otherwise.
   */
  @Override
  public int compareTo(Quantity o) {
    return o.count - count;
  }

}
