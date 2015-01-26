package org.w3c.rdf.syntax;

import org.w3c.rdf.model.*;
import java.io.Writer;
import java.io.IOException;

/**
 * RDF serializer interface.
 */

public interface RDFSerializer {

  /**
   * Serialize a given model into an XML character output stream.
   */
  public void serialize(Model model, Writer w) throws SerializationException, IOException, ModelException;

}

