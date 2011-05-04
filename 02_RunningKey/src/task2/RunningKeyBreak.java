package task2;

import java.util.Vector;

import de.tubs.cs.iti.jcrypt.chiffre.CharacterMapping;

public class RunningKeyBreak {

  private final CharacterMapping charMap;
  private final Quantities[] nGramms;

  private Quantities textBlock;
  private int[] g;
  
  private Vector<Quantities> textKeys;
  
  /**
   * Constructor generates the language quantities specified by {@link #charMap}.
   * @param charMap The character map.
   * @param modulus The modulus used for encryption.
   */
  public RunningKeyBreak(CharacterMapping charMap, int modulus) {
    this.charMap = charMap;
    nGramms = new Quantities[3];
    for (int i = 0 ; i< 3; i++) {
      nGramms[i] = Quantities.createLanguageQuantities(i+1, charMap, modulus);
    }
    textKeys = new Vector<Quantities>();
  }
  
  /**
   * 
   * @param textBlock The text to break.
   * @param g Die Gewichte, die der Benutzer festgelegt hat.
   * @return Mögliche Schlüssel-Texte.
   */
  public Vector<Quantities> getMostPossibleKeys(Quantities textBlock, int[] g) {
    this.textBlock = textBlock;
    this.g = g;
    calculatePossibleKeys();
    return textKeys;
  }

  private void calculatePossibleKeys() {
    double maxWeight = 0;
    Quantities bestKey = textBlock;
    // Schlüssel vorgeben
    KeyFactory kf = new KeyFactory(nGramms[2], textBlock.size());
    while (kf.hasNext()) {
      Quantities key = kf.getNext();
      Quantities plain = textBlock.decryptWithKey(key);
      double pPlain = getProbabilityOfText(plain);
      double pKey   = getProbabilityOfText(key);
      double w = pKey * pPlain;
      if (w > maxWeight) {
        maxWeight = w;
        bestKey = key;
        StringBuffer sb = new StringBuffer();
        sb.append("enc: ").append(textBlock.remap(charMap)) //.append(textBlock)
        .append(", key: ").append(key.remap(charMap)) //.append(key)
        .append(", plain: ").append(plain.remap(charMap)) //.append(plain)
        .append(", w=").append(w);
        System.out.println(sb.toString());
      }
    }
    textKeys.add(bestKey);
  }

  private double getProbabilityOfText(Quantities text) {
    double resultSum = 0;
    for (int n=0; n<3; n++) { // Uni-Gramme, Di-Gramme und Tri-Gramme
      int[] integers = new int[n+1];
      double sum = 0;
      Quantities qs = nGramms[n];
      for (int i=0; i<text.size()-n; i++) {
        for (int j=0; j<n+1; j++) {
          integers[j] = text.get(i+j).getInt();
        }
        Quantity q = qs.getQuantityWithIntegers(integers);
        if (q!=null) {
          sum += q.getRelativeFrequency();
        }
      }
      resultSum += g[n]*sum;
    }
    return resultSum;
  }

}
