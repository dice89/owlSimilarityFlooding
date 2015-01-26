/**
 * Copyright © Sergey Melnik (Stanford University, Database Group)
 *
 * All Rights Reserved.
 */


package edu.stanford.db.rdf.model.i;

import org.w3c.rdf.model.*;
import org.w3c.rdf.io.*;
import org.w3c.rdf.digest.*;

import java.util.*;
import java.io.*;

/**
 * @author Sergey Melnik <melnik@db.stanford.edu>
 *
 */

public class ModelImpl implements Model, Digestable, Digest, Serializable {

  //  static final boolean CHECK_CONSISTENCY = true;

//   static NodeFactory DEFAULT_NODE_FACTORY = new Registry();
  NodeFactory nodeFactory; // = DEFAULT_NODE_FACTORY;

  // whether triples and lookup are shared
  boolean shared = false;
  byte[] digest;

  //  int findCounter = 0;

  ModelImpl myClone = null;

  String uri;
  Hashtable triples;
  //  LongTable subjects, predicates, objects;

  // first find operation creates lookup table
  private FindIndex _findIndex;

  public static final int NO_INDEX = 0;
  public static final int SPO_INDEX = 1;
  public static final int OP_INDEX = 2;
  public static final int P_INDEX = 4;
  public static final int ALL_INDEX = SPO_INDEX | OP_INDEX | P_INDEX;
  

  public ModelImpl() {

    this(Registry.getDefaultRegistry(), ALL_INDEX);
  }

  public ModelImpl(int index) {

    this(Registry.getDefaultRegistry(), index);
  }

  public ModelImpl(NodeFactory f) {

    this(f, ALL_INDEX);
  }

  public ModelImpl(NodeFactory f, int index) {

    nodeFactory = f;
    triples = new Hashtable();
    _findIndex = new FindIndex(nodeFactory, index);
  }

  protected ModelImpl(String uri, Hashtable triples, FindIndex findIndex, NodeFactory f, boolean shared) {
    this.uri = uri;
    this.triples = triples;
    this.shared = shared;
    this.nodeFactory = f;
    _findIndex = findIndex;
  }


  /**
   * Set a base URI for the message.
   * Affects creating of new resources and serialization syntax.
   * <code>null</code> is just alright, meaning that
   * the serializer will use always rdf:about and never rdf:ID
   */
  public void setSourceURI(String uri) {
    this.uri = uri;
  }

  /**
   * Returns current base URI setting.
   */
  public String getSourceURI() {
    return uri;
  }

  // Model access

  /**
   * Number of triples in the model
   *
   * @return  number of triples
   */
  public int size() {
    return triples.size();
  }

  public boolean isEmpty() {
    return triples.isEmpty();
  }

  /**
   * Enumerate triples
   */
  public Enumeration elements() {
    return triples.elements();
  }

  /**
   * Tests if the model contains the given triple.
   *
   * @return  <code>true</code> if the triple belongs to the model;
   *          <code>false</code> otherwise.
   * @see     org.w3c.rdf.model.Statement
   */
  public boolean contains(Statement t) {
    return triples.containsKey(t);
  }

  // Model manipulation: add, remove, find

  /**
   * Adds a new triple to the model. A literal is created out of the string parameter <code>object</code>.
   * This method is just a shortcut.
   *
   * @see     org.w3c.rdf.model.Resource
   */

  public void add(Resource subject, Resource predicate, String object) throws ModelException {

    add(nodeFactory.createStatement(subject, predicate, nodeFactory.createLiteral(object)));
  }


  /**
   * Adds a new triple to the model. The object of the triple is an RDFNode.
   *
   * @see     org.w3c.rdf.model.Resource
   * @see     org.w3c.rdf.model.RDFNode
   */

  public void add(Resource subject, Resource predicate, RDFNode object) throws ModelException {

    add(nodeFactory.createStatement(subject, predicate, object));
  }

  /**
   * Adds a new triple to the model.
   *
   * @see     org.w3c.rdf.model.Statement
   */
  public void add( Statement t ) throws ModelException {

    makePrivate();

    triples.put(t, t);

    if(validLookup())
      _findIndex.addLookup(t);

    updateDigest(t);
  }

  void updateDigest(Statement t)  throws ModelException {

    digest = null;

    /*

    if(digest == null)
    return;

    try {
    Digest d = RDFDigestUtil.getStatementDigest(t);
    DigestUtil.xor(digest, d.getDigestBytes());
    } catch (DigestException exc) {

    throw new ModelException(exc.toString());
    }
    */
  }

  boolean validLookup() {

    return _findIndex.size() > 0;
  }

  void makePrivate() {

    // if we have a clone, tell clone to detach
    if(myClone != null && myClone.shared) {

      myClone.makePrivate();
      myClone = null;
    }

    if(shared) {

      triples = (Hashtable)triples.clone();
      _findIndex = new FindIndex(nodeFactory, _findIndex.getUsedIndexes());
      shared = false;
    }
  }

  /**
   * Removes the triple from the model.
   *
   * @see     org.w3c.rdf.model.Statement
   */
  public void remove(Statement t) throws ModelException {

    makePrivate();

    triples.remove(t);
    //    System.out.println("REMOVED FROM MODEL: " + t + ", NEW SIZE: " + size());

    if(validLookup())
      _findIndex.removeLookup(t);

    updateDigest(t);
  }


  /**
   * General method to search for triples.
   * <code>null</code> input for any parameter will match anything.
   * <p>Example: <code>Model result = m.find( null, RDF.type, new Resource("http://...#MyClass") )</code>
   * <p>finds all instances of the class <code>MyClass</code>
   *
   * @see     org.w3c.rdf.model.Resource
   * @see     org.w3c.rdf.model.RDFNode
   */
  public Model find( Resource subject, Resource predicate, RDFNode object ) throws ModelException {

    Model res = new ModelImpl();// EMPTY_MODEL;

    if(triples.size() == 0)
      return res;

    if(subject == null && predicate == null && object == null)
      return this.duplicate();

    boolean createLookup = !validLookup();

    Iterator it = null;

    if(!createLookup)
      it = _findIndex.multiget(subject, predicate, object);

    // if no useful index available, or createLookup needed, just take the full list of triples
    if(it == null)
      it = triples.values().iterator();

//      if(createLookup)
//        System.err.println("FIND CAUSED CREATE LOOKUP");

    while(it.hasNext()) {

      Statement t = (Statement)it.next();

      if(createLookup) {
	_findIndex.addLookup(t);
      }

      if(matchStatement(t, subject, predicate, object)) {
	//	System.err.println("-- " + t);
	res.add(t);
      }
    }

//      if(createLookup)
//        System.err.println("CREATE LOOKUP DONE");

    /*
    if(CHECK_CONSISTENCY && !createLookup) {

      it = triples.values().iterator();

      int numFound = 0;
      HashSet set = new HashSet();
      while(it.hasNext()) {
	
	Statement t = (Statement)it.next();
	
	if(matchStatement(t, subject, predicate, object)) {
	  //	System.err.println("-- " + t);
	  numFound++;
	  set.add(t);
	}
      }

      if(numFound != res.size()) {
	System.err.println("Inconsistency in find: found " + res.size() + ", expected: " + numFound +
			   "\nChecking found set and retrying with DEBUG on");
	it = set.iterator();
	while(it.hasNext()) {
	  Statement st = (Statement)it.next();
	  System.err.println("Expected: " + st +
			     "\n        in SPO:" + _findIndex.spoIdx.containsValue(st));
	}

	_findIndex.DEBUG = true;
	StatementImpl.DEBUG = true;
	_findIndex.multiget(subject, predicate, object);
	System.exit(1);
      }
	
    }
    */

    return res;
  }


  boolean matchStatement(Statement triple, Resource subject, Resource predicate, RDFNode object) throws ModelException {

    if( subject != null && !triple.subject().equals(subject))
      return false;
      
    if(predicate != null && !triple.predicate().equals(predicate))
      return false;
      
    if(object != null && !triple.object().equals(object))
      return false;

    return true;
  }

  /**
   * Clone the model.
   */
  public Model duplicate() {

    // creates a model that shares triples and lookup with this model

    //    System.out.println("DUPLICATING " + myClone);

    if(myClone == null || !myClone.shared)
      return myClone = new ModelImpl(uri, triples, _findIndex, nodeFactory, true);
    else {
      // myClone is still shared
      // find the next clone in the chain and call its duplicate method
      return myClone.duplicate();
    }
  }

  public Object clone() {
    return duplicate();
  }

  /**
   * Creates empty model of the same Class
   */
  public Model create() {
    return new ModelImpl();
  }

  public NodeFactory getNodeFactory() {

    return nodeFactory;
  }
  // Resource implementation

  public String getLabel() throws ModelException {

    return getURI();
  }

  public String getNamespace() {

    return null;
  }

  public String getLocalName() throws ModelException {

    return getURI();
  }

  public Digest getDigest() {

    return this;
  }

  public boolean isMutable() {

    return true;
  }

  public String getURI() throws ModelException {

    if(isEmpty())
      return nodeFactory.createUniqueResource().toString();
    else
      try {
	return RDFDigestUtil.modelDigestToURI(getDigest());
      } catch (DigestException de) {
	throw new ModelException("Could not obtain model digest: " + de);
      }
  }

  // Digest implementation
  public String getDigestAlgorithm() {

    return RDFDigestUtil.getDigestAlgorithm();
  }


  public byte[] getDigestBytes() throws DigestException {

    try {
      if(digest == null)
	digest = RDFDigestUtil.computeModelDigest(this).getDigestBytes();
      
      byte[] result = new byte[digest.length];
      System.arraycopy(digest, 0, result, 0, digest.length);
      return result;
    } catch (ModelException exc) {
      throw new DigestException(exc.toString());
    }
  }

  Object writeReplace() throws ObjectStreamException {

    return new SerializableModel(this);
  }


  public String toString() {

    return "Model[" + getSourceURI() + " of size " + size() + "]";
  }
}
