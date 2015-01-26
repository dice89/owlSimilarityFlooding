package org.w3c.rdf.util;

import org.w3c.rdf.model.*;
import org.w3c.rdf.syntax.*;

/**
 * A standard class used for parsing into an RDF model
 */

public class ModelConsumer implements RDFConsumer {
  
  Model model;
	NodeFactory nodeFactory;

  /**
   * @param an RDF model to fill with triples. This frequently will be an empty model.
   */
  public ModelConsumer(Model empty) throws ModelException {
    this.model = empty;
    nodeFactory = model.getNodeFactory();
  }

  /**
   * start is called when parsing of data is started
   */
  public void startModel () {}
  
  /**
   * end is called when parsing of data is ended
   */
  public void endModel () {}


	public NodeFactory getNodeFactory() {
		return nodeFactory;
	}
  
  /**
   * assert is called every time a new statement within
   * RDF data model is added
   */
  public void addStatement (Statement s) throws ModelException {

    model.add(s);

//     Resource r = s.subject();
//     System.out.println("CONSUMER SUBJECT " + r.getNamespace() + " --- " + r.getLocalName());
//     new Exception().printStackTrace(System.out);
  }

}

