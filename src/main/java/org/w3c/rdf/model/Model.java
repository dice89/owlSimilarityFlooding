/**
 * Copyright © Sergey Melnik (Stanford University, Database Group)
 *
 * All Rights Reserved.
 */

package org.w3c.rdf.model;

import java.util.Enumeration;

/**
 * This is a programming interface to an RDF model.
 * An RDF model is a directed labeled graph.
 * This interface represents a model as container of triples.
 * RDF should implement <code>getURI()</code> according to the digest algorithm provided in <tt>RDFDigestUtil</tt>.
 *
 * @see org.w3c.tools.crypt.DigestUtil
 * @see org.w3c.rdf.util.RDFDigestUtil
 *
 * @author Sergey Melnik <melnik@db.stanford.edu>
 */

public interface Model extends Resource {

  // Model interface

  /**
   * Set a base URI for the model.
   */
  public void setSourceURI(String uri) throws ModelException;

  /**
   * Returns current base URI setting.
   */
  public String getSourceURI() throws ModelException;

  // Model access

  /**
   * Number of triples in the model
   *
   * @return  number of triples, -1 if unknown
   * 
   * @seeAlso org.w3c.rdf.model.VirtualModel
   */
  public int size() throws ModelException;

  /**
   * true if the model contains no triples
   */
  public boolean isEmpty() throws ModelException;

  /**
   * Enumerate triples
   */
  public Enumeration elements() throws ModelException;

  /**
   * Tests if the model contains the given triple.
   *
   * @return  <code>true</code> if the triple belongs to the model;
   *          <code>false</code> otherwise.
   */
  public boolean contains(Statement t) throws ModelException;

  // Model manipulation: add, remove, find

  /**
   * Adds a new triple to the model.
   */
  public void add(Statement t) throws ModelException;

  /**
   * Removes the triple from the model.
   */
  public void remove(Statement t) throws ModelException;

  /**
   * True if the model supports <tt>add()</tt> and <tt>remove()</tt> methods.
   * A model may change behavior of this function over time.
   */
  public boolean isMutable() throws ModelException;

  /**
   * General method to search for triples.
   * <code>null</code> input for any parameter will match anything.
   * <p>Example: <code>Model result = m.find( null, RDF.type, m.getNodeFactory().createResource("http://...#MyClass") )</code>
   * <p>finds all instances of the class <code>MyClass</code>
   */
  public Model find( Resource subject, Resource predicate, RDFNode object ) throws ModelException;

  /**
   * Search for a single triple.
   * <code>null</code> input for any parameter will match anything.
   *
   * @return  a single triple if such was found;<br>
   *          <code>null</code> if nothing was found;<br>
   *          otherwise a RuntimeException is thrown.
  public Statement find1( Resource subject, Resource predicate, RDFnode object );
   */

  /**
   * Clone the model.
   */
  public Model duplicate() throws ModelException;

  /**
   * Creates empty model of the same Class
   */
  public Model create() throws ModelException;

  /**
   * Returns the node factory for this model
   */
  public NodeFactory getNodeFactory() throws ModelException;

  /**
   * Creates a unique unnamed resource
   */
  //  public Resource noname();
}


