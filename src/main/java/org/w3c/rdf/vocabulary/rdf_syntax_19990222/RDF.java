package org.w3c.rdf.vocabulary.rdf_syntax_19990222;

import org.w3c.rdf.model.*;

/**
 * This class provides convenient access to schema information.
 * DO NOT MODIFY THIS FILE.
 * It was generated automatically by edu.stanford.db.rdf.vocabulary.Generator
 */

public class RDF {

  /** Namespace URI of this schema */
  public static final String _Namespace = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

  /** A triple consisting of a predicate, a subject, and an object. */
  public static Resource Statement;

  /** A collection of alternatives */
  public static Resource Alt;

  /** Identifies the object of a statement when representing the statement in reified form */
  public static Resource object;

  /** Identifies the resource that a statement is describing when representing the statement in reified form */
  public static Resource subject;

  /** Identifies the principal value (usually a string) of a property when the property value is a structured resource */
  public static Resource value;

  /** Identifies the property used in a statement when representing the statement in reified form */
  public static Resource predicate;

  /** A name of a property, defining specific meaning for the property */
  public static Resource Property;

  /** An ordered collection */
  public static Resource Seq;

  /** Identifies the Class of a resource */
  public static Resource type;

  /** An unordered collection */
  public static Resource Bag;

  static {
    try {
      setNodeFactory(new org.w3c.rdf.implementation.model.NodeFactoryImpl());
    } catch (ModelException ex) { ex.printStackTrace(System.err); }
  }

  private static Resource createResource(NodeFactory f, String suffix) throws ModelException {
    return f.createResource(_Namespace, suffix);
  }

  public static void setNodeFactory(NodeFactory f) throws ModelException {

    Statement = createResource(f, "Statement");
    Alt = createResource(f, "Alt");
    object = createResource(f, "object");
    subject = createResource(f, "subject");
    value = createResource(f, "value");
    predicate = createResource(f, "predicate");
    Property = createResource(f, "Property");
    Seq = createResource(f, "Seq");
    type = createResource(f, "type");
    Bag = createResource(f, "Bag");
  }
}

