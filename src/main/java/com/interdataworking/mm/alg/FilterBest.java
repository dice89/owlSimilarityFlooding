package com.interdataworking.mm.alg;

import com.interdataworking.*;
import org.w3c.rdf.model.*;
import org.w3c.rdf.util.*;
import java.util.*;

/**
 * Filters the best matches efficiently without sorting the pairs.
 * If unique flag is set, matches with more than one candidate are discarded.
 * (a, c, 1), (a, d, 1), (b, c, 1), (b, d, 1)
 *
 * This filter preserves the original order of pairs. It also removes all pairs with non-positive similarity.
 */

public class FilterBest implements UntypedGateway {

  MapPair[] pairs;

  public FilterBest() {
  }

  public static int[] append(int[] a, int p) {

    // scan array from back for the first terminator
    int firstVacant = -1;
    for(int l = a.length; --l >=0;)
      if(a[l] < 0)
	firstVacant = l;

    if(firstVacant >= 0) {
      a[firstVacant] = p;
      return a;
    }

    // need to extend array
    
    int[] res = new int[a.length * 2 + 1];
    System.arraycopy(a, 0, res, 0, a.length);
    Arrays.fill(res, a.length + 1, res.length, -1);
    res[a.length] = p;
    return res;
  }

  public void pass(Map map, Object n, MapPair p, int i, int[] penalty) {

    if(p.sim <= 0) { // trash this pair
      penalty[i]++;
      return;
    }

    int[] cand = (int[])map.get(n);

    if(cand == null) {

      cand = new int[2]; // reserve one element
      cand[0] = i;
      cand[1] = -1; // terminator
      map.put(n, cand);

    } else {

      double diff = pairs[cand[0]].sim - p.sim;

      if(diff < 0) {
	// nullify all and replace cand
	for(int j=0; j < cand.length; j++)
	  if(cand[j] >= 0)
	    penalty[cand[j]]++;
	  else
	    break; // terminator reached
	cand[0] = i;
	Arrays.fill(cand, 1, cand.length, -1); // fill with terminators

      } else if(diff > 0) {

	penalty[i]++;

      } else { // sim is equal

	cand = append(cand, i);
	map.put(n, cand);
      }
    }
  }

  // unique: at most one match candidate?
  public List getFilterBest(Collection arr, boolean unique) {

    pairs = new MapPair[arr.size()];
    arr.toArray(pairs);
    // initially, all positions are non-null

    // do a pass over left objects
    Map map1 = new HashMap(); // RDFNode -> int[] of positions

    // if an entry in this array remains zero, the pair will appear in the result
    int[] penalty = new int[pairs.length]; // filled with zeroes

    for(int i=0; i < pairs.length; i++) {

      MapPair p = pairs[i];
      //      System.err.println("\n-- left pass " + p);
      pass(map1, p.getLeft(), p, i, penalty);
    }

    Map map2 = new HashMap(); // map1 still needed if unique requested

    for(int i=0; i < pairs.length; i++) {

      MapPair p = pairs[i];
      //      System.err.println("\n-- right pass " + p);
      pass(map2, p.getRight(), p, i, penalty);
    }
    if(unique) {
      penalizeNonUnique(map1, penalty);
      penalizeNonUnique(map2, penalty);
    }

    List result = new ArrayList();

    for(int i=0; i < pairs.length; i++) {

      if(penalty[i] == 0)
	result.add(pairs[i]);
    }

    return result;
  }

  void penalizeNonUnique(Map map, int[] penalty) {

    for(Iterator it = map.values().iterator(); it.hasNext();) {

      int[] cand = (int[])it.next();

      int uniquePos = -1;

      for(int j=0; j < cand.length && cand[j] >= 0; j++) {

	if(penalty[cand[j]] == 0)
	  switch(uniquePos) {
	  case -1: uniquePos = cand[j]; break;
	  case -2: penalty[cand[j]]++; break;
	  default: penalty[uniquePos]++; penalty[cand[j]]++; uniquePos = -2; break;
	  }
      }
    }
  }

  public List execute(List input) throws ModelException {

    Model src = (Model)input.get(0);

    List l1 = MapPair.toMapPairs(src);
    List best = getFilterBest(l1, true);

    ArrayList l = new ArrayList();
    Model dest = MapPair.asModel(src.create(), best);
    l.add(dest);
    return l;
  }
  

  public int getMinInputLen() { return 1; }

  public int getMaxInputLen() { return 1; }

  public int getMinOutputLen() { return 1; }

  public int getMaxOutputLen() { return 1; }


  public static void main(String[] args) throws Exception {


    RDFFactory rf = new RDFFactoryImpl();
    NodeFactory nf = rf.getNodeFactory();

    Resource a = nf.createResource("a");
    Resource b = nf.createResource("b");
    Resource c = nf.createResource("c");
    Resource d = nf.createResource("d");
    Resource e = nf.createResource("e");

    List l = new ArrayList();

    l.add(new MapPair(d, b, 0.7));
    l.add(new MapPair(a, b, 0.5));
    l.add(new MapPair(c, b, 0.6));
    l.add(new MapPair(e, b, 0.7));
    l.add(new MapPair(d, c, 0.8));

    l.add(new MapPair(a, c, 0.5));
    l.add(new MapPair(a, d, 0.5));
    l.add(new MapPair(b, c, 0.5));
    l.add(new MapPair(b, d, 0.5));

    Collection result = new FilterBest().getFilterBest(l, true);
    MapPair.printMap(result, System.out);
  }
}
