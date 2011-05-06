package task2;

import java.util.Comparator;

public class SortQuantityByDescWeight implements Comparator<Quantity> {

  /**
   * Sort by descending weight.
   */
  @Override
  public int compare(Quantity o1, Quantity o2) {
    return (int) Math.signum(o2.getWeight() - o1.getWeight());
  }

}
