package com.interdataworking.mm.alg;

import java.util.*;
import com.interdataworking.*;
import org.w3c.rdf.model.*;
import org.w3c.rdf.util.*;

public class StringMatcher implements UntypedGateway {

  static final double UNEQUAL_STRING_MATCH_PENALTY = 0.5; // 1.0: no penalty, 0: ignore completely
  static final double RESOURCE_LITERAL_MATCH_PENALTY = 0.1; // 1.0: no penalty, 0: ignore completely
  boolean USE_WORD_FREQ = true; // 1: no penalty, >1: higher penalty
  static final double FREQ_PENALTY = 1; // 1: no penalty, >1: higher penalty

  static final int MAXDIGNUM = 5;
  static final boolean TRY_OPAQUE_RESOURCES = true; // consider resources that have no namespace

  // convergence ensured when TRY_ALL_NODES and MIN_NODE_SIM > 0 !!!

  boolean TRY_ALL_NODES = false; // initially, consider all pairwise matches as possible

  protected static final double MIN_NODE_SIM = 0.001; // need to keep low; imagine there are 1000 dissimilar nodes vs. 1 similar node in the vicinity of (adjacent to)...


  // distortion

  int DISTORT = NONE;
  // int DISTORT = RANDOMIZE;
  //int DISTORT = AVERAGIZE;
  

  final static int NONE = 0;
  final static int RANDOMIZE = 1;
  final static int RANDOMIZE_PERCENT = 400; // in percent
  final static int AVERAGIZE = 2;


  HashSet
    initialMatch /* pairs that used only for init */,
    reinforceMatch /* pairs that are used for reinforcing */;
  HashMap strCompMap = new HashMap(); // string -> List

  Map wordFreqM1, wordFreqM2;

  Map strSim; // string -> MapPair, given string similarity

  // no spaces and more than MAXDIGNUM numbers -> don't consider
  public static boolean opaque(String s) {

    int digNum = 0;
    for(int i = 0; i < s.length(); i++) {

      char c = s.charAt(i);
      if(Character.isWhitespace(c))
	return false;

      if(Character.isDigit(c))
	digNum++;
    }
    return digNum > MAXDIGNUM;
  }

  public double computeStringSimilarity(String s1, String s2) {

    if(opaque(s1) || opaque(s2))
      return 0;

//     if(strSim != null) {
//       MapPair p = (MapPair)strSim.get(s1);
//       System.err.println("Got: " + p + " for " + s1);
//       if(p != null)
// 	return p.sim;
//     }

    List l1 = getWords(s1);
    List l2 = getWords(s2);

    return computeStringSimilarity(l1, l2);
  }

  public static double computeStringSimilarity(List l1, List l2) {

    return computeStringSimilarity(l1, l2, null, null);
  }

  public static double computeStringSimilarity(List l1, List l2, Map freq1, Map freq2) {

    if(l1.size() == 0 || l2.size() == 0)
      return 0;

    // calculate pairwise similarity
    double sim = 0;
    //    double prop = 1.0 / Math.min(l1.size(), l2.size());

    double prop = 2.0 / (l1.size() + l2.size());

    HashSet used = new HashSet();
    for(int i = 0; i < l1.size(); i++) {
      if(used.contains(l1.get(i)))
	continue;
      for(int j = 0; j < l2.size(); j++) {
	String t1 = (String)l1.get(i);
	String t2 = (String)l2.get(j);

	// if equals, full match; if prefix: half match
	double rs = wordMatch(t1, t2, prop);

	if(freq1 != null) {
	  int f = getFreq(freq1, t1);
	  if(f <= 0) throw new InternalError("Cannot happen: no freq1 for " + t1);
	  else if(f > 1)
	    f *= FREQ_PENALTY;
	  rs /= f;
	}

	if(freq2 != null) {
	  int f = getFreq(freq2, t2);
	  if(f <= 0) throw new InternalError("Cannot happen: no freq2 for " + t2);
	  else if(f > 1)
	    f *= FREQ_PENALTY;
	  rs /= f * FREQ_PENALTY;
	}

	if(rs > 0) {
	  sim += rs;
	  used.add(l1.get(i));
	}
      }
    }

//     if(sim > 0)
//       System.err.println("Similarity of " + s1 + " and " + s2 + " = " + sim);

    return sim;
  }

  // if s1==s2 return max, else 0 <= res < max
  public static double wordMatch(String s1, String s2, double max) {

    int l1 = s1.length();
    int l2 = s2.length();

    double maxWithPenalty = max * UNEQUAL_STRING_MATCH_PENALTY;

    int len = Math.min(l1, l2);
    int maxLen = Math.max(l1, l2);

    if(len < 1) // TODO: does one letter match make sense? probably...
      return 0; // does not make much sense

    // prefix match?
    double forPrefix = 0;

    int i=0;
    while(i < len) {
      if(s1.charAt(i) != s2.charAt(i)) {
	forPrefix = maxWithPenalty * ((double)i/l1) * ((double)i/l2); // (..) instead of maxLen
	break;
      } else
	i++;
    }

    // one letter does not count
    if(i == 1)
      forPrefix = 0;
    else if(i == len)
      forPrefix = maxWithPenalty * ((double)i/l1) * ((double)i/l2);

    double result;
    
    if(i == len && i == l1 && i == l2)  { // equals

      result = max;
      
    } else { // not equal

      // suffix match
      double forSuffix = 0;
      int j = 0;
      while(j < len) {
	if(s1.charAt(l1-1-j) != s2.charAt(l2-1-j)) {
	  forSuffix = maxWithPenalty * ((double)j/l1) * ((double)j/l2); // / (l1-j + l2-j + len) * j;
	  break;
	} else
	  j++;
      }
      //      System.err.println("Suffix stopped at " + j + " with " + forSuffix);

      // one letter does not count
      if(j == 1)
	forSuffix = 0;
      else if(j == len)
	forSuffix = maxWithPenalty * ((double)j/l1) * ((double)j/l2);

      result = Math.max(forPrefix, forSuffix);
    }

//     if(result > 0)
//       System.err.println("Word match <" + s1 + ", " + s2 + ", " + max + ">=" + result);
    return result;
  }

  // use map as cache
  public static List getWords(String s, Map m) {

    List l = (List)m.get(s);
    if(l == null)
      m.put(s, l = getWords(s));
    return l;
  }

  public static List getWords(String s) {

    Iterator it = new StringComponentIterator(s);
    ArrayList l = new ArrayList();
    while(it.hasNext()) {
      String c = ((String)it.next()).toLowerCase();
      //      System.err.println("getWords: Component " + c + " of " + s);
      l.add(c);
    }
    return l;
  }

  public int getMinInputLen() { return 2; }

  public int getMaxInputLen() { return 3; }

  public int getMinOutputLen() { return 1; }

  public int getMaxOutputLen() { return 1; }

  public List execute(List input) throws ModelException {

    Model m1 = (Model)input.get(0);
    Model m2 = (Model)input.get(1);

    if(input.size() >= 3) {
      Model strm = (Model)input.get(2);
      strSim = MapPair.toStringMap(MapPair.toMapPairs(strm));
      System.err.println("Using exact similarity mappping with " + strSim.size() + " entries obtained from " + strm.size() + " model");
    } else
      strSim = null; // empty

    Collection result = initialGuess(m1, m2);

    switch(DISTORT) {

    case RANDOMIZE: MapPair.randomizeMap(result, RANDOMIZE_PERCENT); break;
    case AVERAGIZE: MapPair.averagizeMap(result); break;

    }

    // RETURN
    ArrayList l = new ArrayList();

    Model map = MapPair.asModel(m1.create(), new ArrayList(result).toArray());
    l.add(map);
    return l;
  }

  /** returns a map: word -> frequency **/
  Map getWordFreq(Model m) throws ModelException {

    Map wordFreq = new HashMap();

    for(Enumeration en1 = RDFUtil.getNodes(m).elements(); en1.hasMoreElements();) {
      
      RDFNode n1 = (RDFNode)en1.nextElement();
      if(n1 instanceof Statement)
	continue;

      //      System.err.println("getWordFreq1: " + n1);

      String s = getTextualContent(n1);
      if(s == null)
	continue;

      //      System.err.println("getWordFreq2: " + s);

      List l = StringMatcher.getWords(s, strCompMap);

      //      System.err.println("getWordFreq3: " + l);

      for(int i=0; i < l.size(); i++)
	incFreqOf(wordFreq, (String)l.get(i));

      //      System.err.println("getWordFreq4");
    }

    return wordFreq;
  }

  static int getFreq(Map wordFreq, String word) {

    IntHolder i = (IntHolder)wordFreq.get(word);
    return i == null ? 0 : i.value;
  }

  static void incFreqOf(Map wordFreq, String word) {

    IntHolder i = (IntHolder)wordFreq.get(word);
    if(i == null)
      wordFreq.put(word, new IntHolder());
    else
      i.value++;
  }


  // set similarities of equals nodes to 1, others to 0
  // initialize both tables

  // returns list of MapPairs
  public Collection initialGuess(Model m1, Model m2)  throws ModelException {

    HashSet initialMatch = new HashSet();
    HashSet reinforceMatch = new HashSet();

    // calculate global frequencies of words
    if(USE_WORD_FREQ) {
      this.wordFreqM1 = getWordFreq(m1);
      this.wordFreqM2 = getWordFreq(m2);
    } else {
      this.wordFreqM1 = null;
      this.wordFreqM2 = null;
    }

    //    System.err.println("Freq2: " + wordFreqM2);
    
    for(Enumeration en1 = RDFUtil.getNodes(m1).elements(); en1.hasMoreElements();) {
      
      RDFNode n1 = (RDFNode)en1.nextElement();
      if(n1 instanceof Statement)
	continue;
      
      for(Enumeration en2 = RDFUtil.getNodes(m2).elements(); en2.hasMoreElements();) {
	
	RDFNode n2 = (RDFNode)en2.nextElement();
	if(n2 instanceof Statement)
	  continue; // ignore statements

	//	System.err.println("Node similarity: " + n1 + " and " + n2);

	double sim = computeNodeSimilarity(n1, n2);

	boolean isL1 = n1 instanceof Literal;
	boolean isL2 = n2 instanceof Literal;

	if(isL1 != isL2)
	  sim *= RESOURCE_LITERAL_MATCH_PENALTY;

	boolean toInitial = false;
	if(sim < MIN_NODE_SIM && TRY_ALL_NODES) {
	  if(sim == 0)
	    toInitial = true;
	  sim = MIN_NODE_SIM;
	}

	if(sim > 0) {
	  MapPair p = new MapPair(n1, n2, sim);
// 	  if(toInitial)
// 	    initialMatch.add(p);
// 	  else
	    reinforceMatch.add(p);
	  //	  System.err.println("Adding " + p);
	}
      }
    }

    return reinforceMatch;
  }

  boolean isOpaqueResource(RDFNode n1) throws ModelException {

    if(n1 instanceof Resource) {
      String ns = ((Resource)n1).getNamespace();
      if((!TRY_OPAQUE_RESOURCES && ns == null) ||
	 (ns != null && ns.startsWith("urn:rdf:"))) // StringMatcher.opaque(ns)))
	return true;
    }
    return false;
  }

  double computeNodeSimilarity(RDFNode n1, RDFNode n2) throws ModelException {

//     if(n1 instanceof Statement || n2 instanceof Statement)
//       return 0;

    if(n1.equals(n2))
      return 1.0;

    String s1 = getTextualContent(n1);
    String s2 = getTextualContent(n2);

    double sim = 0;

    if(s1 != null && s2 != null) {

      if(strSim != null) {
	MapPair p = (MapPair)strSim.get(s1);
	// 	if(p != null)
// 	  System.err.println("Got: " + p + " for " + s1);
	if(p != null && s2.equals(p.getRightNode().getLabel()))
	  return p.sim;
      }

      List l1 = StringMatcher.getWords(s1, strCompMap);
      List l2 = StringMatcher.getWords(s2, strCompMap);

      sim = computeStringSimilarity(l1, l2, wordFreqM1, wordFreqM2);
    }

    // use a damping factor when matching a literal against a resource
    if(sim > 0 &&
       (n1 instanceof Literal && n2 instanceof Resource) ||
       (n2 instanceof Literal && n1 instanceof Resource))
      sim /= 2;

    return sim;
  }

  String getTextualContent(RDFNode n) throws ModelException {

    if(isOpaqueResource(n))
      return null;

    String s = n instanceof Literal ? n.getLabel() : ((Resource)n).getLocalName();

    if(StringMatcher.opaque(s))
      return null;

    return s;
  }

}

class IntHolder {

  int value = 1;
}

