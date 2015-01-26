package org.w3c.rdf.syntax;

import org.xml.sax.InputSource;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.w3c.rdf.model.ModelException;

/**
 * RDF parser interface
 */

public interface RDFParser {

  /**
   * Parse from the given SAX/XML input source.
   */
  public void parse(InputSource source, RDFConsumer consumer) throws SAXException;

  public void setErrorHandler (ErrorHandler handler);

}

