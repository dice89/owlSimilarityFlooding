
package org.w3c.rdf.syntax;

import org.w3c.rdf.model.*;


/**
 * RDFParser passes triples to RDFConsumer
 *
 * @see org.w3c.rdf.syntax.RDFParser
 */

public interface RDFConsumer {

  /**
   * start is called when parsing of data is started
   */
  public void startModel () throws ModelException;
  
  /**
   * end is called when parsing of data is ended
   */
  public void endModel () throws ModelException;

  /**
   * node factory to be used by the parser for creating resources and literals passed to addStatement
   */
  public NodeFactory getNodeFactory() throws ModelException;
  
  /**
   * assert is called every time a new statement within
   * RDF data model is added
   */
  public void addStatement (Statement s) throws ModelException;
 
}

