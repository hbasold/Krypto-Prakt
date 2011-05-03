package task2;

import java.util.Vector;

import de.tubs.cs.iti.jcrypt.chiffre.CharacterMapping;

public class RunningKeyBreak {

  private final CharacterMapping charMap;
  private final int modulus;
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
    this.modulus = modulus;
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
    KeyFactory kf = new KeyFactory(nGramms[2], textBlock.size());
    double maxProbability = 0;
    Quantities bestKey = textBlock;
    while (kf.hasNext()) {
      Quantities key = kf.getNext();
//      System.out.println(key.remap(charMap));
      double pKey   = getProbabilityOfText(key);
      double pPlain = getProbabilityOfText(textBlock.decryptWithKey(key));
      
//      StringBuffer sb = new StringBuffer();
//      sb.append(textBlock.decryptWithKey(key).remap(charMap));
//      sb.append(", key: ");
//      sb.append(key.remap(charMap));
//      System.out.println(sb.toString());

      double p = pKey * pPlain;
      if (p > maxProbability) {
        maxProbability = p;
        bestKey = key;
      }
    }
    textKeys.add(bestKey);
  }

  private double getProbabilityOfText(Quantities key) {
    return 0;
  }

  private String remapToString(int charInt) {
    return CharacterMapping.convertToString(charMap.remapChar(charInt));
  }

}
