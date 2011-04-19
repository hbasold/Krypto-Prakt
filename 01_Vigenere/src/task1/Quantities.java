package task1;

import java.util.Vector;

public class Quantities extends Vector<CharQuantity> {

  private static final long serialVersionUID = 1L;

  private int numberOfChars;

  /**
   * @see Vector#Vector(int)
   */
  public Quantities(int size) {
    super(size);
  }

  @Override
  public synchronized boolean add(CharQuantity q) {
    numberOfChars += q.getQuantity();
    return super.add(q);
  }

  /**
   * Calculates the quantity of a specific char from all quantities
   * added to this vector.
   * @param i The index of the CharQuantity in this vector.
   * @return The quantity of the given CharQuantity in percent.  
   */
  public double getQuantityInPercent(int i) {
    return 100.0 * get(i).getQuantity() / numberOfChars;
  }

}
