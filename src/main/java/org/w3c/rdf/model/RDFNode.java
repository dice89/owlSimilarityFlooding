/**
 * Copyright © World Wide Web Consortium, (Massachusetts Institute of
 * Technology, Institut National de Recherche en Informatique et en
 * Automatique, Keio University).
 *
 * All Rights Reserved.
 *
 * @author	Janne Saarela <jsaarela@w3.org>
 * @author	Sergey Melnik <melnik@db.stanford.edu>
 */
package org.w3c.rdf.model;

/**
 * An abstract RDF node. Can either be resource or literal, exclusively.
 */
public interface RDFNode {

  /**
   * The formal string label of the node.
   * URI in case of a resource, string in case of a literal.
   */
  public String getLabel() throws ModelException;
}

