package edu.stanford.db.rdf.vocabulary.order_20000527;

import org.w3c.rdf.model.*;

/**
 * This class provides convenient access to schema information.
 * DO NOT MODIFY THIS FILE.
 * It was generated automatically by edu.stanford.db.rdf.vocabulary.Generator
 */

public class RDFX {

  /** Namespace URI of this schema */
  public static final String _Namespace = "http://interdataworking.com/vocabulary/order-20000527#";

  /** Identifies the backward order of the relationship in a statement. */
  public static Resource backwardOrder;

  /** Identifies the forward order of the relationship in a statement. */
  public static Resource order;

  static {
    try {
      setNodeFactory(new org.w3c.rdf.implementation.model.NodeFactoryImpl());
    } catch (ModelException ex) { ex.printStackTrace(System.err); }
  }

  private static Resource createResource(NodeFactory f, String suffix) throws ModelException {
    return f.createResource(_Namespace, suffix);
  }

  public static void setNodeFactory(NodeFactory f) throws ModelException {

    backwardOrder = createResource(f, "backwardOrder");
    order = createResource(f, "order");
  }
}

