package task2;

public class KeyFactory {
  private final Quantities nGramms;
  private final int keySize;
  private final int n;
  private final int countNGramms;
  private final int maxNGrammsPerKey;
  private final int maxKeys;
  private int currentKeyNumber;

  public KeyFactory(Quantities nGramms, int keySize) {
    this.nGramms = nGramms;
    this.keySize = keySize;
    n = nGramms.get(0).getIntegers().length;
    countNGramms = nGramms.size();
    maxNGrammsPerKey = (int) Math.ceil(keySize / (double) n);
    maxKeys = (int) Math.pow(countNGramms, maxNGrammsPerKey);
    currentKeyNumber = 0;
    System.out.println("Schlüsselanzahl insgesamt: "+maxKeys);
  }
  
  public boolean hasNext() {
    return currentKeyNumber < maxKeys;
  }
  
  public Quantities getNext() {
    Quantities key = new Quantities();
    // Zeichen aus Tri-Grammen aufbauen
    int digit = currentKeyNumber;
    for (int p = 0; p<maxNGrammsPerKey; p++) {
      key.addNGrammAsUniGramms(nGramms.get(digit % countNGramms));
      digit = digit / countNGramms;
    }
    // Ggf. Zeichen am Ende abschneiden, damit der Schlüssel
    // genauso lang ist wie der Text.
    while (key.size()>keySize) {
      key.remove(key.size()-1);
    }
    currentKeyNumber++;
//    if (currentKeyNumber % countNGramms == 100) {
//      currentKeyNumber += countNGramms-100;
//    }
    return key;
  }
  
}