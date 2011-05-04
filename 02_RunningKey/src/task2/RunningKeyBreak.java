package task2;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import de.tubs.cs.iti.jcrypt.chiffre.CharacterMapping;

public class RunningKeyBreak {

  private final CharacterMapping charMap;
  private final Quantities[] nGramms;
  private final int modulus;

  private int[] g;
  
  private Vector<Quantities> textKeys;
  
  /**
   * Constructor generates the language quantities specified by {@link #charMap}.
   * @param charMap The character map.
   * @param modulus The modulus used for encryption.
   */
  public RunningKeyBreak(CharacterMapping charMap, int modulus) {
    this.modulus = modulus;
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
    this.g = g;
    Vector<Quantities> possibleKeys = keyUnigramCandidates(textBlock, nGramms[0]);
    Vector<Quantities> k = calculatePossibleKeys(possibleKeys,textBlock);
    return k;
  }

  private Vector<Quantities> calculatePossibleKeys(Vector<Quantities> possibleKeys, Quantities textBlock) {
    // Schlüssel vorgeben
    
    Vector<Quantities> k = new Vector<Quantities>();    
    Quantities bestKey = new Quantities();
    
    for(int i = 0; i < possibleKeys.size() - 3; i += 3){
      Vector<Quantities> triGramCandidates = calcTrigramCandidates(possibleKeys.subList(i, i + 3));
      if(!triGramCandidates.isEmpty()){
        double maxWeight = -1;
        Quantities bestKeyTrigram = null;
        for(Quantities key : triGramCandidates) {
          Quantities plain = textBlock.decryptWithKey(key, i, i + 3);
          double pPlain = getProbabilityOfText(plain);
          double pKey   = getProbabilityOfText(key);
          double w = pKey * pPlain;
          if (w > maxWeight) {
            maxWeight = w;
            bestKeyTrigram = key;
            StringBuffer sb = new StringBuffer();
            sb.append("enc: ").append(textBlock.remap(charMap)) //.append(textBlock)
            .append(", key: ").append(key.remap(charMap)) //.append(key)
            .append(", plain: ").append(plain.remap(charMap)) //.append(plain)
            .append(", w=").append(w);
            System.out.println(sb.toString());
          }
        }
        System.out.println("bestTrigram = " + bestKeyTrigram.remap(charMap));
        bestKey.addAll(bestKeyTrigram);
      }
      else{
        System.out.println("no trigram found");
        bestKey.add(new Quantity(0));
        bestKey.add(new Quantity(0));
        bestKey.add(new Quantity(0));
      }
    }
    
    k.add(bestKey);
    return k;
  }

  /**
   * 
   * @param triGram Kandidaten für ein Trigramm (d.h. Liste von Kandidaten für jede Position)
   * @return Menge möglicher Trigramme, die gebildet werden können.
   */
  private Vector<Quantities> calcTrigramCandidates(List<Quantities> triGram) {
    assert triGram.size() == 3;
    assert triGram.get(0).get(0).getIntegers().length == 1;
    
    Vector<Quantities> cand = new Vector<Quantities>();
    
    for(Quantity t : nGramms[2]){
      if(matchTriGram(t, triGram)){
        cand.add(makeQuantities(t));
      }
    }
    
    return cand;
  }

  private Quantities makeQuantities(Quantity t) {
    Quantities q = new Quantities();
    for(int c : t.getIntegers()){
      q.add(new Quantity(c));
    }
    return q;
  }

  /**
   * Prüft, ob sich die Buchstaben aus triGramCand derart kombinieren
   * lassen, dass sich triGram ergibt.
   * 
   * @param triGram
   * @param triGramCand
   * @return
   */
  private boolean matchTriGram(Quantity triGram, List<Quantities> triGramCand) {
    assert triGram.getIntegers().length == triGramCand.size();
    
    Iterator<Quantities> it = triGramCand.iterator();
    boolean allMatched = false;
    for(int c : triGram.getIntegers()){
      allMatched = false;
      Quantities candidates = it.next();
      for(Quantity cc : candidates){
        if(c == cc.getInt()){
          allMatched = true;
          break;
        }
      }
    }
    
    return allMatched;
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

  private Vector<Quantities> keyUnigramCandidates(Quantities textBlock, Quantities languageQuantities) {
    Vector<Quantities> vKeys = new Vector<Quantities>(textBlock.size());

    int numRelevantChars = 5;
    
    for(Quantity enc : textBlock) {
      Quantities cCandidates = new Quantities(modulus);
      vKeys.add(cCandidates);
      
      for(int i = 0; i < numRelevantChars; ++i){
        cCandidates.add(new Quantity(0, 0, 0));
      }
      // Schlüssel
      for (Quantity key: languageQuantities) {
        int shift = enc.getShift(key, modulus);
        Quantity plain = languageQuantities.getQuantityWithInteger(shift);
        if (plain!=null) {
          double probability = key.getRelativeFrequency() * plain.getRelativeFrequency();
          for(int i = 0; i < numRelevantChars; ++i){
            if(cCandidates.get(i).getRelativeFrequency() < probability){
              System.out.println(key.toString()+" "+plain+" pro:"+probability);
              cCandidates.set(i, new Quantity(key.getInt(), 0, probability));
              break;
            }
          }
        }
      }
    }
    return vKeys;
  }

  private Vector<Quantities> calculateMostProbableKeyText(
      Vector<Quantities> possibleKeys, Quantities languageTriGrams) {
    Vector<Quantities> keyTexts = new Vector<Quantities>();
    Quantities text = new Quantities();
    keyTexts.add(text);
    for (int i=0; i<possibleKeys.size()-3 ; i++) {
      List<Quantities> keys3 = possibleKeys.subList(i, i+3);
      text.addAll(calculateMostProbableSequence(keys3, languageTriGrams));
    }
    return keyTexts;
  }

  /**
   * Liefert das wahrscheinlichste Trigramm für die aufeinanderfolgenden
   * Quantities in der Liste.
   * @param sequenceKeys Liste 
   * @param languageTriGrams
   * @return
   */
  private Quantities calculateMostProbableSequence(
      List<Quantities> sequenceKeys, Quantities languageTriGrams) {
    Quantity best = new Quantity();
    for (Quantity lng: languageTriGrams) {
      Quantities keys = new Quantities();
      for (int i = 0; i<lng.getIntegers().length; i++) {
        int c = lng.getInt(i);
        for (Quantity key: sequenceKeys.get(i)) {
          if (c==key.getInt()) {
            keys.add(key);
            break;
          }
        }
        if (keys.size()-1!=i) {
          break;
        }
      }
      if (best.getRelativeFrequency()<lng.getRelativeFrequency()) {
        best = lng;
      }
    }
    Quantities qs = new Quantities();
    for (int i: best.getIntegers()) {
      qs.add(new Quantity(i));
    }
    return qs;
  }
  
}
