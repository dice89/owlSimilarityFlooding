/**
 * Simple Statement class
 *
 *
 * @author Sergey Melnik
 * @author Janne Saarela
 */
package org.w3c.rdf.model;

import org.w3c.rdf.model.*;

/**
 * An RDF statement.<p>
 * 
 * Statements must implement <code>getURI()</code> according to a standard MD5-based algorithm.
 *
 * @see org.w3c.tools.crypt.DigestUtil
 * @see org.w3c.rdf.util.RDFDigestUtil
 */

public interface Statement extends Resource {

  /**
   * @return subject of the triple
   */
  public Resource subject() throws ModelException;

  /**
   * @return predicate of the triple
   */
  public Resource predicate() throws ModelException;

  /**
   * @return object of the triple
   */
  public RDFNode object() throws ModelException;

  /**
   * @return the model that contains this triple, <code>null</code> if none
   */
  //  public RDFModel getOwnerModel();
}

