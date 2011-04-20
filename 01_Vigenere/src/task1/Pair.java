package task1;

/**
 * Generic helper class for a pair object containing two objects named
 * first and second. The types of first and second are defined by the
 * generic type parameters.
 *
 * @param <FIRST> Type of first object.
 * @param <SECOND> Type of second object.
 */
public class Pair<FIRST, SECOND> implements Comparable<Pair<FIRST, SECOND>> {
  public final FIRST first;
  public final SECOND second;

  private Pair(FIRST first, SECOND second) {
    this.first = first;
    this.second = second;
  }

  public static <FIRST, SECOND> Pair<FIRST, SECOND> of(FIRST first,
      SECOND second) {
    return new Pair<FIRST, SECOND>(first, second);
  }

  /**
   * Implementation for sorting with collections.
   * The default sorting order is descending on first followed by second.
   */
  @Override
  public int compareTo(Pair<FIRST, SECOND> o) {
    int cmp = compare(first, o.first);
    return cmp == 0 ? compare(second, o.second) : cmp;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static int compare(Object o1, Object o2) {
    return o1 == null ? o2 == null ? 0 : -1 : o2 == null ? +1
        : ((Comparable) o2).compareTo(o1);
  }

  @Override
  public int hashCode() {
    return 31 * hashCode(first) + hashCode(second);
  }

  private static int hashCode(Object o) {
    return o == null ? 0 : o.hashCode();
  }

  @SuppressWarnings("rawtypes")
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Pair))
      return false;
    if (this == obj)
      return true;
    return equal(first, ((Pair) obj).first)
        && equal(second, ((Pair) obj).second);
  }

  private boolean equal(Object o1, Object o2) {
    return o1 == null ? o2 == null : (o1 == o2 || o1.equals(o2));
  }

  @Override
  public String toString() {
    return "(" + first + ", " + second + ')';
  }
}
