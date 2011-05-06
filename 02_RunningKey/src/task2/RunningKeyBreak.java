package task2;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import de.tubs.cs.iti.jcrypt.chiffre.CharacterMapping;

public class RunningKeyBreak {
  private final Quantities[] nGramms;
  private final int modulus;

  private int[] g;
    
  /**
   * Constructor generates the language quantities specified by {@link #charMap}.
   * @param charMap The character map.
   * @param modulus The modulus used for encryption.
   */
  public RunningKeyBreak(CharacterMapping charMap, int modulus) {
    this.modulus = modulus;
    nGramms = new Quantities[3];
    for (int i = 0 ; i< 3; i++) {
      nGramms[i] = Quantities.createLanguageQuantities(i+1, charMap, modulus);
    }
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
    Vector<Vector<Vector<Quantity>>> weighted = lift(possibleKeys);
    // Exponentiell abnehmende Zahl an Möglichkeiten, wobei am Ende 50 Möglichkeiten
    // übrig bleiben sollen.
    int possiblitiesToGenerate = 50 * ((int)Math.ceil((Math.log(textBlock.size()) / Math.log(2))) - 1);
    for(int blockSize = 4; blockSize <= textBlock.size(); blockSize *= 2, possiblitiesToGenerate /= 2){
      System.out.println("Weighting " + possiblitiesToGenerate + " blocks (size " + blockSize + "):");
      weighted = weightKeys(weighted, textBlock, blockSize, possiblitiesToGenerate);
    }
    //weighted = weightKeys(weighted, textBlock, textBlock.size());
    return weighted;
  }

  /**
   * Macht aus einem String von Kandidaten einen String von Mengen von
   * Kandidaten-Substrings, wobei jeder Substring nur einen Buchstaben hat.
   * 
   * @param possibleKeys
   * @return
   */
  private Vector<Vector<Vector<Quantity>>> lift(Vector<Quantities> possibleKeys) {
    Vector<Vector<Vector<Quantity>>> str = new Vector<Vector<Vector<Quantity>>>();
    for(Quantities candidates : possibleKeys){
      Vector<Vector<Quantity>> cand = new Vector<Vector<Quantity>>();
      str.add(cand);
      for(Quantity c : candidates){
        Vector<Quantity> c_ = new Vector<Quantity>();
        c_.add(c);
        cand.add(c_);
      }
    }
    return str;
  }

  private Vector<Vector<Vector<Quantity>>> weightKeys(Vector<Vector<Vector<Quantity>>> k,
      Quantities textBlock, int charsToAnalyse, int possiblitiesToGenerate) {
    
    int subStrSize = k.get(0).get(0).size();
    assert charsToAnalyse % subStrSize == 0;
    int subStrStep = charsToAnalyse / subStrSize;
    
    int steps = (int)((textBlock.size() + charsToAnalyse) / charsToAnalyse);
    
    Vector<Vector<Vector<Quantity>>> weightedStr = new Vector<Vector<Vector<Quantity>>>();
    
    for(int i = 0; i < steps; i += 1){
      int end = Math.min((i + 1) * subStrStep, k.size());
      int strEnd = Math.min((i + 1) * charsToAnalyse, textBlock.size());
      Vector<Vector<Quantity>> weighted
        = weightSubKey(k.subList(i * subStrStep, end),
            textBlock, i * charsToAnalyse, strEnd, possiblitiesToGenerate);
      
      if(!weighted.isEmpty()){
        weightedStr.add(weighted);
      }
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
   * @param list
   * @param textBlock
   * @param start
   * @param end
   * @param possiblitiesToGenerate 
   * @return Menge von Schlüssel-Texten sortiert nach ihrer Wahrscheinlichkeit 
   */
  private Vector<Vector<Quantity>> weightSubKey(
      List<Vector<Vector<Quantity>>> list, Quantities textBlock, int start, int end, int possiblitiesToGenerate) {
    
    Vector<Pair<Vector<Quantity>, Double>> weighted = new Vector<Pair<Vector<Quantity>, Double>>();
    
    CombinationIterator combs = new CombinationIterator(list);

    System.out.println("Weighting: ");
    while(combs.hasNext()){
      Vector<Quantity> key = flatten(combs.next());
      Vector<Quantity> plain = textBlock.decryptWithKey(key, start, end);
      double pPlain = getProbabilityOfText(plain);
      double pKey   = getProbabilityOfText(key);
      double w = pKey * pPlain;
      weighted.add(Pair.of(key, new Double(w)));
      //System.out.println("W( " + key + " ⇒ " + plain + " ) = " + w);
    }
    
    Collections.sort(weighted, new CompareBySecondDesc());
    
    Vector<Vector<Quantity>> best = getFirst(weighted.subList(0, Math.min(possiblitiesToGenerate, weighted.size())));
    
    return best;
  }

  /**
   * Macht aus einem String von Substrings einen flachen String
   * 
   * @param next
   * @return
   */
  private Vector<Quantity> flatten(Vector<Vector<Quantity>> next) {
    Vector<Quantity> str = new Vector<Quantity>();
    
    for(Vector<Quantity> sub : next){
      str.addAll(sub);
    }
    
    return str;
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
   * 
   * Erzeugt alle Kombinationen von Polygrammen aus subStr.
   *
   */
  class CombinationIterator implements Iterator<Vector<Vector<Quantity>>>{

    private Vector<Vector<Quantity>> current; // String von Substrings
    private Vector<Iterator<Vector<Quantity>>> combs; // Iteratoren über die Kandidaten für jeden Substring
    private List<Vector<Vector<Quantity>>> possibilies; // Anfangsiteratoren
    
    CombinationIterator(List<Vector<Vector<Quantity>>> list){
      current = new Vector<Vector<Quantity>>();
      combs = combinationsBegin(list, current);
      possibilies = list;
    }
    
    private Vector<Iterator<Vector<Quantity>>> combinationsBegin(
        List<Vector<Vector<Quantity>>> list, Vector<Vector<Quantity>> combined) {
      Vector<Iterator<Vector<Quantity>>> combs = new Vector<Iterator<Vector<Quantity>>>();
      
      for(Vector<Vector<Quantity>> qs : list){
        Iterator<Vector<Quantity>> iq = qs.iterator();
        combined.add(iq.next());
        combs.add(iq);
      }
      
      return combs;
    }
    
    @Override
    public boolean hasNext() {
      boolean hasNext = false;
      
      for(Iterator<Vector<Quantity>> iq : combs){
        hasNext = iq.hasNext();
        if(hasNext){
          break; // Optimierung
        }
      }
      
      return hasNext;
    }

    @Override
    public Vector<Vector<Quantity>> next() {
      // Rückwärts laufen
      for(int i = combs.size() - 1; i >= 0; --i){
        Iterator<Vector<Quantity>> iq = combs.get(i);
        if(iq.hasNext()){
          current.set(i, iq.next());
          break;
        }
        else {
          iq = possibilies.get(i).iterator();
          combs.set(i, iq);
          current.set(i, iq.next());
        }
      }
      
      return current;
    }

    @Override
    public void remove() { }   
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
        Quantity q = nGramms[n].findQuantityWithIntegers(integers);
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

    int numRelevantChars = 10;
    
    for(Quantity enc : textBlock) {
      Quantities cCandidates = new Quantities(modulus);
      vKeys.add(cCandidates);
      
      for(int i = 0; i < numRelevantChars; ++i){
        cCandidates.add(new Quantity());
      }
      // Schlüssel
      for (Quantity key: languageQuantities) {
        int shift = enc.getShift(key, modulus);
        Quantity plain = languageQuantities.findQuantityWithInteger(shift);
        if (plain!=null) {
          double probability = key.getRelativeFrequency() * plain.getRelativeFrequency();
          for(int i = 0; i < numRelevantChars; ++i){
            if(cCandidates.get(i).getRelativeFrequency() < probability){
              System.out.println(key.toString()+" "+plain+" pro:"+probability);
              cCandidates.set(i, new Quantity(key.getInt(), probability));
              break;
            }
          }
        }
      }
    }
    return vKeys;
  } 
}
