package edu.stanford.db.rdf.model.i;

import java.util.*;
import org.w3c.rdf.model.*;
import edu.stanford.db.xml.util.QName;

public class FindIndex {

  protected static boolean DEBUG = false;

  //  StatementImpl dummy;
  static final Comparator spoComparator = new SPOComparator();
  static final Comparator opComparator = new OPComparator();
  static final Comparator pComparator = new PComparator();

  // null is smaller than any RDFNode, MAX_RES is larger than any RDFNode
  static final ResourceImpl MAX_RES = new ResourceImpl("<MAX>");
  //  Statement MAX_STMT;

  TreeMap spoIdx, pIdx, opIdx;

  int useIdx;

  //  Iterator emptyIterator = new OneOrLessIterator(null);

  public FindIndex(NodeFactory f, int useIdx) {

    this.useIdx = useIdx;
    //    MAX_RES = new ResourceImpl("<MAX>");
    //    MAX_STMT = new StatementImpl(null, null, null);

    if((useIdx & ModelImpl.SPO_INDEX) != 0)
      spoIdx = new TreeMap(spoComparator); //new SPOComparator());
    if((useIdx & ModelImpl.P_INDEX) != 0)
      pIdx = new TreeMap(pComparator); // new PComparator());
    if((useIdx & ModelImpl.OP_INDEX) != 0)
      opIdx = new TreeMap(opComparator); // new OPComparator());
  }

  public int getUsedIndexes() {

    return useIdx;
  }

  void put(Map m, Statement st) {

    Object o = m.get(st);

    if(o == null) {
      //      System.err.println("PUT first " + st);
      m.put(st, st);
    }

    else if(o instanceof Statement) {

      HashSet s = new HashSet();
      s.add(o);
      s.add(st);
      m.put(st, s);
      //      System.err.println("PUT second " + st);

    } else { // set

      //      System.err.println("PUT " + ((Set)o).size() + "th " + st);
      ((Set)o).add(st);
    }
  }

  Resource limitResource(Resource r) {

    return r == null ? MAX_RES : r;
  }

  RDFNode limitNode(RDFNode r) {

    return r == null ? MAX_RES : r;
  }

  Iterator get(SortedMap m, Statement st) throws ModelException {

    return new PrefixIterator(st, m);
  }

  public int size() {

    return spoIdx.size();
  }

  void remove(Map m, Statement st) {

    Object o = m.get(st);
    if(o == null)
      return;
    else if(o instanceof Statement)
      m.remove(st);
    else { // set
      Set s = (Set)o;
      s.remove(st);
      if(s.size() == 0)
	m.remove(st);
    }
  }

  public void addLookup(Statement st) {

    //    System.err.println("ADDING " + st);
    put(spoIdx, st);
    put(opIdx, st);
    put(pIdx, st);
  }

  public void removeLookup(Statement st) {

    remove(spoIdx, st);
    remove(opIdx, st);
    remove(pIdx, st);
  }

  public Iterator multiget(Resource s, Resource p, RDFNode o) throws ModelException {

    // avoid consistency checking in StatementImpl
    StatementImpl dummy = new StatementImpl();
    dummy.s = s;
    dummy.p = p;
    dummy.o = o;

    if(DEBUG)
      System.err.println("FIND FOR " + dummy + ":");

    if(s == null) {
      if(o == null) {
	if(DEBUG)
	  System.err.println("USING p");
	if(pIdx != null)
	  return get(pIdx, dummy);
	return null; // no useful index
      } else {
	//	System.err.println("USING op");
	//	System.err.println(opIdx.toString());
	//	dummy = new StatementImpl(null, null, null);

	// opIdx is optimal. But what if it's missing?
	if(opIdx != null)
	  return get(opIdx, dummy);
	// pIdx would also work, although suboptimal
	if(p != null && pIdx != null)
	  return get(pIdx, dummy);
	return null; // no useful index at all
      }
    } else {
      if(spoIdx != null) {
	if(DEBUG)
	  System.err.println("USING spo");
	if(p == null)
	  dummy.o = null; // otherwise SPOComparer will fail
	return get(spoIdx, dummy);
      } else { // need to use suboptimal index
	if(o != null && opIdx != null)
	  return get(opIdx, dummy);
	if(p != null && pIdx != null)
	  return get(pIdx, dummy);
	return null; // no useful index
      }
    }
  }


//   public int compare(RDFNode n1, RDFNode n2) {

//     if(n1 instanceof Statement && n2 instanceof Statement)
//       return compare((Statement)n1, (Statement)n2);
//     else if(n1 instanceof Resource && n2 instanceof Resource)
//       return compare((Resource)n1, (Resource)n2);
//     else if(n1 instanceof Resource && n2 instanceof Literal)
//       return -1;
//     else if(n1 instanceof Literal && n2 instanceof Resource)
//       return 1;
//     else // both literal
//       return n1.getLabel().compareTo(n2.getLabel());
//   }

//   public int compare(Resource r1, Resource r2) {
//   }

  static int compareNodes(RDFNode n1, RDFNode n2) {

    if(n1 == null || n2 == MAX_RES)
      return -1;
    else if(n2 == null || n1 == MAX_RES)
      return 1;
    else
      //      return n1.toString().hashCode() - n2.toString().hashCode();
      //      return n1.hashCode() - n2.hashCode();
      {
	int c1 = n1.hashCode();
	int c2 = n2.hashCode();

	if(c1 < c2)
	  return -1;
	else if(c1 > c2)
	  return 1;
	else
	  return 0;
      }
  }

  /*
  private abstract class StatementKey {

    Statement st;

    public StatementKey(Statement st) {
      this.st = st;
    }

    public abstract StatementKey create(Statement st);

    public Statement getStatement() {
      return st;
    }
    public String toString() {
      return "[KEY " + st + "]";
    }
  }

  private class SPOKey extends StatementKey implements Comparable {

    public SPOKey(Statement st) {

      super(st);
    }

    public int compareTo(Object that) {

      return spoComparator.compare(this, that);
    }

    public boolean equals(Object that) {
      return spoComparator.compare(this, that) == 0;
    }

    public StatementKey create(Statement st) {
      return new SPOKey(st);
    }
  }
    
  private class OPKey extends StatementKey implements Comparable {

    public OPKey(Statement st) {

      super(st);
    }

    public int compareTo(Object that) {

      return opComparator.compare(this, that);
    }

    public boolean equals(Object that) {
      return opComparator.compare(this, that) == 0;
    }

    public StatementKey create(Statement st) {
      return new OPKey(st);
    }
  }
    
  private class PKey extends StatementKey implements Comparable {

    public PKey(Statement st) {

      super(st);
    }

    public int compareTo(Object that) {

      return pComparator.compare(this, that);
    }

    public boolean equals(Object that) {
      return pComparator.compare(this, that) == 0;
    }

    public StatementKey create(Statement st) {
      return new PKey(st);
    }
  }
  */    

  private static class SPOComparator implements Comparator
  {
    public int compare(Object o1, Object o2)
    {
//       if(!(o1 instanceof Statement)) {
// 	System.err.println("--- " + o1 + " (" + spoIdx.size() + ")");
//       }
      Statement st1 = (Statement)o1;
      Statement st2 = (Statement)o2;

      try {
	int diff = 0;
	RDFNode n1, n2;

	n1 = st1.subject();
	n2 = st2.subject();

	if((diff = compareNodes(n1, n2)) == 0) {

	  n1 = st1.predicate();
	  n2 = st2.predicate();

	  if((diff = compareNodes(n1, n2)) == 0) {

	    diff = compareNodes(st1.object(), st2.object());
	  }
	}
	//	return diff;

	if(DEBUG)
	  System.err.println("Comparing " + o1 + " and\n" +
			     "          " + o2 + " = " + diff);
 	return diff;

      } catch (ModelException any) {
	return 0;
      }
    }

    public boolean equals(Object object)
    {
      return object instanceof SPOComparator;
    }
  }

  private static class PComparator implements Comparator
  {
    public int compare(Object o1, Object o2)
    {
      Statement st1 = (Statement)o1;
      Statement st2 = (Statement)o2;

      try {
	int diff = 0;
	RDFNode n1, n2;

	n1 = st1.predicate();
	n2 = st2.predicate();
	if((diff = compareNodes(n1, n2)) != 0)
	  return diff;

	// need this since upper boundary in subMap is not inclusive
	// either subject or object is OK
	if(st2.subject() == MAX_RES)
	  return -1;
	else if(st1.subject() == MAX_RES)
	  return 1;
	else
	  return 0;

      } catch (ModelException any) {
	return 0;
      }
    }

    public boolean equals(Object object)
    {
      return object instanceof PComparator;
    }
  }

  private static class OPComparator implements Comparator
  {
    public int compare(Object o1, Object o2)
    {
      Statement st1 = (Statement)o1;
      Statement st2 = (Statement)o2;

      try {
	int diff = 0;
	RDFNode n1, n2;

	n1 = st1.object();
	n2 = st2.object();
	if((diff = compareNodes(n1, n2)) != 0)
	  return diff;

	n1 = st1.predicate();
	n2 = st2.predicate();
	if((diff = compareNodes(n1, n2)) != 0)
	  return diff;

	// need this since upper boundary in subMap is not inclusive
	if(st2.subject() == MAX_RES)
	  return -1;
	else if(st1.subject() == MAX_RES)
	  return 1;
	else
	  return 0;
	
      } catch (ModelException any) {
	return 0;
      }
    }

    public boolean equals(Object object)
    {
      return object instanceof OPComparator;
    }
  }

  /*
  private class OneOrLessIterator implements Iterator {

    Object o;

    public OneOrLessIterator(Object o) {

      this.o = o;
    }

    public void remove() {
      // do nothing
    }

    public boolean hasNext() {

      return o != null;
    }

    public Object next() {

      if (o == null)
	throw new NoSuchElementException();
      else {
	Object p = o;
	o = null;
	return p;
      }
    }
  }
  */

  private class PrefixIterator implements Iterator {

    //    Statement st; // search statement
    Iterator it; // iterator over sets/statements
    int maxIt; // increment it maxIt times at most
    Object n;
    Iterator setIt;
    //    Comparator comparator;

    public PrefixIterator(Statement st, SortedMap m) throws ModelException {

      //      this.st = st;

      //      this.comparator = m.comparator();

      // if all of s,p,o are set, then upper boundary was left unspecified,
      // iterate it only once.
      if(st.subject() != null && st.predicate() != null && st.object() != null) {
	maxIt = 1;
	this.it = m.tailMap(st).values().iterator();
      } else {
	maxIt = Integer.MAX_VALUE;
	Statement limit = new StatementImpl(limitResource(st.subject()),
					    limitResource(st.predicate()),
					    limitNode(st.object()));
	this.it = m.subMap(st, limit).values().iterator();
      }

      step();
    }

    void step() {

      n = null;

      if(setIt == null || !setIt.hasNext()) {

	if(it.hasNext() && maxIt > 0) {
	  Object o = it.next();
	  maxIt--;
	  if(DEBUG)
	    System.err.println("-- main it next: " + o);
	  if(o instanceof Statement)
	    n = o;
	  else if(o instanceof Set) {
	    setIt = ((Set)o).iterator();
	    n = setIt.next();
	    if(DEBUG)
	      System.err.println("-- set it next 1: " + n);
	  } else 
	    throw new ClassCastException("Invalid entry " + o + ", must be set or statement");

	} // else no more entries

      } else { // current set is not yet done, hasNext is true

	n = setIt.next();
	if(DEBUG)
	  System.err.println("-- set it next 2: " + n);
      }

      /*
      if(n == null)
	return;

      System.err.println("-- compare: " + comparator.compare(st, n));
      if(comparator.compare(st, n) > 0)
	n = null;
      */
    }
    
    public void remove() {
      // do nothing
    }

    public boolean hasNext() {

      return n != null;
    }

    public Object next() {

      if (n == null)
	throw new NoSuchElementException();
      else {
	Object p = n;
	n = null;
	step();
	return p;
      }
    }
  }

}
