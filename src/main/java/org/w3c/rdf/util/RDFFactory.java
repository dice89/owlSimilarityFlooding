/**
 * Copyright © Sergey Melnik (Stanford University, Database Group)
 *
 * All Rights Reserved.
 */

package org.w3c.rdf.util;

import org.w3c.rdf.model.*;
import org.w3c.rdf.syntax.*;

/**
 * A factory used to create RDF parser/serializer, empty flat models,
 * schema-aware models out of flat models and schema registry.
 * This interface is provided to enable applications to use their
 * own implementations of the above mentioned components in a uniform way.
 *
 * @author Sergey Melnik <melnik@db.stanford.edu>
 */

public interface RDFFactory extends ModelFactory {

  /**
   * Creates a new RDF parser
   */
  public RDFParser createParser();

  /**
   * Creates a new RDF serializer
   */
  public RDFSerializer createSerializer();

  /**
   * Create an empty flat model.
   */
  public Model createModel();

  /**
   * Return the node factory to use for creating statements etc.
   */
  public NodeFactory getNodeFactory();

  /**
   * Create a schema-aware model using the given flat model.
   */
  //  public SchemaModel createSchemaModel(Model m);

  /**
   * Get the schema registry. The schema registry is
   * supposed to be unique, i.e. the same factory delivers
   * always the same registry object.
   */
  //  public SchemaRegistry registry();
}
