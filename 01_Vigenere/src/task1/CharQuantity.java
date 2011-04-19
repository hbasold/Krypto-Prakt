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

  @Override
  public int compareTo(CharQuantity o) {
    return o.quantity - quantity;
  }

}
