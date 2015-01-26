/**
 * Copyright © Sergey Melnik (Stanford University, Database Group)
 *
 * All Rights Reserved.
 */

package org.w3c.rdf.util;

import org.w3c.rdf.model.*;
import org.w3c.rdf.syntax.*;
//import org.w3c.rdf.implementation.model.*;
import edu.stanford.db.rdf.model.i.*;
import org.w3c.rdf.implementation.syntax.sirpac.*;
//import org.w3c.rdf.implementation.syntax.strawman.*;
import java.util.*;

/**
 * A default implementation of the RDFFactory interface.
 *
 * @author Sergey Melnik <melnik@db.stanford.edu>
 */

public class RDFFactoryImpl implements RDFFactory {

    // use strawman parser
  //    boolean strawman = false;

  //  static SchemaRegistry registry;

  /*
  public SchemaRegistry registry() {
    if(registry == null)
      registry = new SchemaRegistryImpl( this );
    return registry;
  }
  */

    public RDFFactoryImpl() {
    }

//     public RDFFactoryImpl(boolean strawman) {
// 	this.strawman = strawman;
//     }

  public RDFParser createParser() {
    
//       if(strawman)
// 	  return new StrawmanParser();
//       else {
	  SiRPAC parser = new SiRPAC();
	  parser.setRobustMode(true);
	  parser.ignoreExternalEntities(true);
	  return parser;
	  //      }
  }

  public RDFSerializer createSerializer() {

//     Map ns = new HashMap();
//     ns.put("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "MYRDF");
//     return new SiRS(ns);
    return new SiRS();
  }

  public Model createModel() {

    return new ModelImpl();
  }

  public NodeFactory getNodeFactory() {

    return new ModelImpl().getNodeFactory();
  }

  /*
  public SchemaModel createSchemaModel(Model m) {

    if(m instanceof SchemaModel)
      return (SchemaModel)m;
    return new SchemaModelImpl( (SchemaRegistryImpl)registry(), m );
  }
  */  
}

