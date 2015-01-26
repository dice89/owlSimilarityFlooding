package org.w3c.rdf.model;

/**
 * Provides methods for creating resources, literals and statements.
 */

public interface NodeFactory {

  /**
   * Creates a resource out of a URI
   */
  public Resource createResource(String uri) throws ModelException;

  /**
   * Creates a resource out of a string
   * @since 2000-10-21
   */
  public Resource createResource(String namespace, String localName) throws ModelException;

  /**
   * Creates a literal from a string
   */
  public Literal createLiteral(String str) throws ModelException;

  /**
   * Creates a literal from a boolean
   */
  public Literal createLiteral(boolean b) throws ModelException;

  /**
   * Creates a literal from a byte
   */
  public Literal createLiteral(byte b) throws ModelException;

  /**
   * Creates a literal from a char
   */
  public Literal createLiteral(char b) throws ModelException;

  /**
   * Creates a literal from a short
   */
  public Literal createLiteral(short b) throws ModelException;

  /**
   * Creates a literal from an int
   */
  public Literal createLiteral(int b) throws ModelException;

  /**
   * Creates a literal from a long
   */
  public Literal createLiteral(long b) throws ModelException;

  /**
   * Creates a literal from a float
   */
  public Literal createLiteral(float b) throws ModelException;

  /**
   * Creates a literal from a double
   */
  public Literal createLiteral(double b) throws ModelException;

  /**
   * Creates a triple
   */
  public Statement createStatement(Resource subject, Resource predicate, RDFNode object) throws ModelException;

  /**
   * Creates a resource with a unique ID
   */
  public Resource createUniqueResource() throws ModelException;

  /**
   * Creates an ordinal
   */
  public Resource createOrdinal(int i) throws ModelException;

}

