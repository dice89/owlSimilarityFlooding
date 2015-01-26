package org.w3c.rdf.vocabulary.rdf_schema_19990303;

import org.w3c.rdf.model.*;

/**
 * This class provides convenient access to schema information.
 * DO NOT MODIFY THIS FILE.
 * It was generated automatically by edu.stanford.db.rdf.vocabulary.Generator
 */

public class RDFS {

  /** Namespace URI of this schema */
  public static final String _Namespace = "http://www.w3.org/TR/1999/PR-rdf-schema-19990303#";

  /** This represents the set Containers. */
  public static Resource Container;

  /** Resources used to express RDF Schema constraints. */
  public static Resource ConstraintResource;

  /** This represents the set of atomic values, eg. textual strings. */
  public static Resource Literal;

  /** This is how we associate a class with  properties that its instances can have */
  public static Resource domain;

  /** Properties that can be used in a   schema to provide constraints */
  public static Resource range;

  /** Indicates a resource that provides information about the subject resource */
  public static Resource seeAlso;

  /** The concept of a property. */
  public static Resource Property;

  /** Indicates a resource containing and defining the subject resource. */
  public static Resource isDefinedBy;

  /** Provides a human-readable version of a resource name */
  public static Resource label;

  /** Indicates specialization of properties */
  public static Resource subPropertyOf;

  /** This is the class that the properties _1,_2, ... that are used to represent lists and are an instance of */
  public static Resource ContainerMembershipProperty;

  /** Indicates membership of a class */
  public static Resource subClassOf;

  /** Use this for descriptions */
  public static Resource comment;

  /** Properties used to express RDF Schema constraints. */
  public static Resource ConstraintProperty;

  /** The concept of Class */
  public static Resource Class;

  /** The most general class */
  public static Resource Resource;

  static {
    try {
      setNodeFactory(new org.w3c.rdf.implementation.model.NodeFactoryImpl());
    } catch (ModelException ex) { ex.printStackTrace(System.err); }
  }

  private static Resource createResource(NodeFactory f, String suffix) throws ModelException {
    return f.createResource(_Namespace, suffix);
  }

  public static void setNodeFactory(NodeFactory f) throws ModelException {

    Container = createResource(f, "Container");
    ConstraintResource = createResource(f, "ConstraintResource");
    Literal = createResource(f, "Literal");
    domain = createResource(f, "domain");
    range = createResource(f, "range");
    seeAlso = createResource(f, "seeAlso");
    Property = createResource(f, "Property");
    isDefinedBy = createResource(f, "isDefinedBy");
    label = createResource(f, "label");
    subPropertyOf = createResource(f, "subPropertyOf");
    ContainerMembershipProperty = createResource(f, "ContainerMembershipProperty");
    subClassOf = createResource(f, "subClassOf");
    comment = createResource(f, "comment");
    ConstraintProperty = createResource(f, "ConstraintProperty");
    Class = createResource(f, "Class");
    Resource = createResource(f, "Resource");
  }
}

