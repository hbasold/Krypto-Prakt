package task1;


public class CharQuantity implements Comparable<CharQuantity> {

  private int charInt;
  private int quantity;
  
  public CharQuantity(int charInt, int quantity) {
    this.charInt = charInt;
    this.quantity = quantity;
  }

  public int getCharInt() {
    return charInt;
  }

  public int getQuantity() {
    return quantity;
  }

  /**
   * Sort quantity in descending order.
   * @return 0 if quantity of this object and the quantity of the given object are equal.
   * A positive integer if the quantity of this object is less
   * than the quantity of the given object, a negative integer otherwise.
   */
  @Override
  public int compareTo(CharQuantity o) {
    return o.quantity - quantity;
  }

}
