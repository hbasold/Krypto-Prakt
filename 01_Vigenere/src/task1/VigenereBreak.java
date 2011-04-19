package task1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

public class VigenereBreak {

  ArrayList<Integer> text;
  
  public VigenereBreak(ArrayList<Integer> text) {
    this.text = text;
  }
  
  private Vector<HashMap<Integer, Integer> > getVectorOfQuantities(int numberOfShifts) {
    // create vector of quantities
    Vector<HashMap<Integer, Integer> > vQuantities = new Vector<HashMap<Integer, Integer> >(numberOfShifts);
    for (int i=0; i<numberOfShifts; i++) {
      HashMap<Integer, Integer> quantities = new HashMap<Integer, Integer>();
      vQuantities.add(quantities);
    }
    // count characters and add it to current shift
    int iShift = 0;
    for (Integer charInt: text) {
      HashMap<Integer, Integer> quantities = vQuantities.get(iShift);
      if (quantities.containsKey(charInt)) { // Zeichen bereits vorhanden
        quantities.put(charInt, quantities.get(charInt) + 1); // Anzahl erhöhen
      } else { // Zeichen noch nicht vorhanden
        quantities.put(charInt, 1);
      }
      iShift = (iShift + 1) % numberOfShifts;
    }
    return vQuantities;
  }

  public Vector<Quantities > getVectorOfSortedQuantities(int numberOfShifts) {
    Vector<HashMap<Integer, Integer> > vQuantities = getVectorOfQuantities(numberOfShifts);
    // create vector of NGrams
    Vector<Quantities> vSortedQuantities = new Vector<Quantities>(numberOfShifts);
    for (HashMap<Integer, Integer> quantities: vQuantities) {
      Quantities sortedQuantities = new Quantities(quantities.size());
      for (Integer key: quantities.keySet()) {
        sortedQuantities.add(new CharQuantity(key, quantities.get(key)));
      }
      java.util.Collections.sort(sortedQuantities);
      vSortedQuantities.add(sortedQuantities);
    }
    return vSortedQuantities;
  }
  
}
