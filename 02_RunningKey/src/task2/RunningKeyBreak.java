package task2;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
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
   * @return Mögliche Schlüssel-Texte. (String von Mengen möglicher Schlüsseltexte)
   */
  public Vector<Vector<Vector<Quantity>>> getMostProbableKeys(Quantities textBlock, int[] g) {
    this.g = g;
    Vector<Quantities> possibleKeys = keyUnigramCandidates(textBlock, nGramms[0]);
    Vector<Vector<Quantity>> k = possibleTrigramKey(possibleKeys);
    Vector<Vector<Vector<Quantity>>> weighted = weightKeys(k, textBlock);
    return weighted;
  }

  private Vector<Vector<Vector<Quantity>>> weightKeys(Vector<Vector<Quantity>> k,
      Quantities textBlock) {
    int charsToAnalyse = 6;
    
    Vector<Vector<Vector<Quantity>>> weightedStr = new Vector<Vector<Vector<Quantity>>>();
    
    Iterator<Vector<Quantity>> strIter = k.iterator();
    int blockNum = 0;
    while(strIter.hasNext()){
      // Zeichen/Trigramme sammeln
      Vector<Vector<Quantity>> subStr = new Vector<Vector<Quantity>>();
      int i = 0;
      for(; i < charsToAnalyse && strIter.hasNext(); ++i){
        Vector<Quantity> cs = strIter.next();
        i += cs.get(0).getIntegers().length - 1; // wenn Trigramm, gehe um 2 weiter
        subStr.add(cs);
      }
      
      // Evtl. weniger Text als angefordert vorhanden
      Vector<Vector<Quantity>> weighted;
      if(i < charsToAnalyse){
        weighted
          = weightSubKey(subStr,
              textBlock, blockNum * charsToAnalyse, (blockNum + 1) * charsToAnalyse - (charsToAnalyse - i));
      }
      else{
        weighted
          = weightSubKey(subStr,
              textBlock, blockNum * charsToAnalyse, (blockNum + 1) * charsToAnalyse);
      }
      
      weightedStr.add(weighted);
      
      ++blockNum;
    }
    
    return weightedStr;
  }
  
  /**
   * 
   * Vergleichen von Paaren nur auf Basis des zweiten Wertes.
   *
   */
  class CompareBySecondDesc implements Comparator<Pair<Vector<Quantity>, Double>>{
    @Override
    public int compare(Pair<Vector<Quantity>, Double> o1,
        Pair<Vector<Quantity>, Double> o2) {
      return (int) Math.signum((o2.second.doubleValue() - o1.second.doubleValue()));
    }    
  }

  /**
   * 
   * 
   * 
   * @param subStr
   * @param textBlock
   * @param start
   * @param end
   * @return Menge von Schlüssel-Texten sortiert nach ihrer Wahrscheinlichkeit 
   */
  private Vector<Vector<Quantity>> weightSubKey(
      Vector<Vector<Quantity>> subStr, Quantities textBlock, int start, int end) {
    
    Vector<Pair<Vector<Quantity>, Double>> weighted = new Vector<Pair<Vector<Quantity>, Double>>();
    
    CombinationIterator combs = new CombinationIterator(subStr);

    System.out.println("Weighting: ");
    while(combs.hasNext()){
      Vector<Quantity> combined = combs.next();
      Vector<Quantity> key = flatten(combined);
      Vector<Quantity> plain = textBlock.decryptWithKey(key, start, end);
      double pPlain = getProbabilityOfText(plain);
      double pKey   = getProbabilityOfText(key);
      double w = pKey * pPlain;
      weighted.add(Pair.of(key, new Double(w)));
      //System.out.println("W( " + key + " ⇒ " + plain + " ) = " + w);
    }
    
    Collections.sort(weighted, new CompareBySecondDesc());
    
    int numBest = 100;
    Vector<Vector<Quantity>> best = getFirst(weighted.subList(0, Math.min(numBest, weighted.size())));
    
    return best;
  }

  /**
   * Extrahiert aus einer Liste von Paaren eine Liste der
   * Werte an erster Stelle.
   * 
   * @param subList
   * @return
   */
  private Vector<Vector<Quantity>> getFirst(
      List<Pair<Vector<Quantity>, Double>> subList) {
    Vector<Vector<Quantity>> str = new Vector<Vector<Quantity>>();
    for(Pair<Vector<Quantity>, Double> p : subList){
      str.add(p.first);
    }
    return str;
  }

  /**
   * Macht aus einem String von Polygrammen einen aus Unigrammen.
   * 
   * @param current
   * @return
   */
  private Vector<Quantity> flatten(Vector<Quantity> polyStr) {
    Vector<Quantity> uniStr = new Vector<Quantity>();
    
    for(Quantity cs : polyStr){
      for(int c : cs.getIntegers()){
        uniStr.add(new Quantity(c));
      }
    }
    
    return uniStr;
  }
  
  /**
   * 
   * Erzeugt alle Kombinationen von Polygrammen aus subStr.
   *
   */
  class CombinationIterator implements Iterator<Vector<Quantity>>{

    private Vector<Quantity> current;
    private Vector<Iterator<Quantity>> combs;
    
    CombinationIterator(Vector<Vector<Quantity>> subStr){
      current = new Vector<Quantity>();
      combs = combinationsBegin(subStr, current);
    }
    
    private Vector<Iterator<Quantity>> combinationsBegin(
        Vector<Vector<Quantity>> subStr, Vector<Quantity> combined) {
      Vector<Iterator<Quantity>> combs = new Vector<Iterator<Quantity>>();
      
      for(Vector<Quantity> qs : subStr){
        Iterator<Quantity> iq = qs.iterator();
        combined.add(iq.next());
        combs.add(iq);
      }
      
      return combs;
    }
    
    @Override
    public boolean hasNext() {
      boolean hasNext = false;
      
      for(Iterator<Quantity> iq : combs){
        hasNext = iq.hasNext();
        if(hasNext){
          break; // Optimierung
        }
      }
      
      return hasNext;
    }

    @Override
    public Vector<Quantity> next() {
      // Rückwärts laufen
      for(int i = combs.size() - 1; i >= 0; --i){
        Iterator<Quantity> iq = combs.get(i);
        if(iq.hasNext()){
          current.set(i, iq.next());
          break;
        }
      }
      
      return current;
    }

    @Override
    public void remove() { }   
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
   * @return Menge von Strings aus Uni- oder Trigrammen (Quantity bel. kann n-Gramm sein)
   */
  private Vector<Vector<Quantity>> possibleTrigramKey(Vector<Quantities> possibleKeys) {
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

  private double getProbabilityOfText(Vector<Quantity> plain) {
    double resultSum = 0;
    for (int n=0; n<3; n++) { // Uni-Gramme, Di-Gramme und Tri-Gramme
      int[] integers = new int[n+1];
      double sum = 0;
      for (int i=0; i<plain.size()-n; i++) {
        for (int j=0; j < n + 1; j++) {
          integers[j] = plain.get(i+j).getInt();
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
