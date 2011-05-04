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
  public Vector<Vector<Quantity>> getMostProbableKeys(Quantities textBlock, int[] g) {
    this.g = g;
    Vector<Quantities> possibleKeys = keyUnigramCandidates(textBlock, nGramms[0]);
    Vector<Vector<Quantity>> k = possibleTrigramKey(possibleKeys,textBlock);
    return k;
  }

  /**
   * Berechnet eine Menge von möglichen Strings aus Uni- oder Trigrammen.
   * 
   * Dabei werden die möglichen Zeichen für den String durchlaufen und es wird
   * versucht aus diesen Trigramme zu machen. Wenn dies nicht möglich ist, wird
   * beim nächsten Zeichen fortgefahren und das übersprungene behält seine
   * ursprünglichen Möglichkeiten.
   * 
   * @param possibleKeys
   * @param textBlock
   * @return Menge von Strings aus Uni- oder Trigrammen (Quantity bel. kann n-Gramm sein)
   */
  private Vector<Vector<Quantity>> possibleTrigramKey(Vector<Quantities> possibleKeys, Quantities textBlock) {
    // Schlüssel vorgeben
    
    // "String" mit mehreren Trigramm-Kandidaten pro Position
    Vector<Vector<Quantity>> k = new Vector<Vector<Quantity>>();
    
    for(int i = 0; i < possibleKeys.size() - 3; ++i){
      Vector<Quantity> triGramCandidates = calcTrigramCandidates(possibleKeys.subList(i, i + 3));
      if(!triGramCandidates.isEmpty()){
        k.add(triGramCandidates);
//        double maxWeight = 0;
//        Quantities bestKeyTrigram = null;
//        for(Quantity trigramKey : triGramCandidates) {
//          Quantities plain = textBlock.decryptWithKey(makeQuantities(trigramKey), i, i + 3);
//          // TODO: nutze letztes Trigramm mit zur Bewertung
//          
//          double pPlain = getProbabilityOfText(plain);
//          double pKey   = getProbabilityOfText(makeQuantities(trigramKey));
////          System.out.println("pPlian = " + pPlain + " pKey = " + pKey);
//          double w = pKey * pPlain;
//          if (w > maxWeight) {
//            maxWeight = w;
//            bestKeyTrigram = trigramKey;
//            StringBuffer sb = new StringBuffer();
//            sb.append("enc: ").append(textBlock.remap(charMap)) //.append(textBlock)
//            .append(", key: ").append(trigramKey.remap(charMap)) //.append(key)
//            .append(", plain: ").append(plain.remap(charMap)) //.append(plain)
//            .append(", w=").append(w);
//            System.out.println(sb.toString());
//          }
//        }
//        System.out.println("bestTrigram = " + bestKeyTrigram.remap(charMap));
//        bestKey.addAll(bestKeyTrigram);
        i += 2; // um 3 weitergehen (s. Schleifenkopf)
      }
      else{
        System.out.println("no trigram found");
        k.add(possibleKeys.get(i));
      }
    }
    
    return k;
  }

  /**
   * 
   * @param triGram Kandidaten für ein Trigramm (d.h. Liste von Kandidaten für jede Position)
   * @return Menge möglicher Trigramme, die gebildet werden können.
   */
  private Vector<Quantity> calcTrigramCandidates(List<Quantities> triGram) {
    assert triGram.size() == 3;
    assert triGram.get(0).get(0).getIntegers().length == 1;
    
    Vector<Quantity> cand = new Vector<Quantity>();
    
    for(Quantity t : nGramms[2]){
      if(matchTriGram(t, triGram)){
        cand.add(t);
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
      for (int i=0; i<text.size()-n; i++) {
        for (int j=0; j < n + 1; j++) {
          integers[j] = text.get(i+j).getInt();
        }
//        System.out.println("integers = " + integers);
        Quantity q = nGramms[n].getQuantityWithIntegers(integers);
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
