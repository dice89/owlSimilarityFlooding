package com.interdataworking.mm;

import java.util.*;
import org.w3c.rdf.model.*;
import org.w3c.rdf.implementation.model.*;

public class MapVocabulary {

  static final String NS = "http://www.interdataworking.com/vocabulary/map/1.0#";

  public Resource
    Map, src, dest, sim, inverse, loop;

  public static MapVocabulary create() {

    try {
      return new MapVocabulary(new NodeFactoryImpl());
    } catch (ModelException ex) {
      return null;
    }
  }

  public MapVocabulary(NodeFactory f) throws ModelException {
    
    Map = f.createResource(NS, "MapEntry");
    src = f.createResource(NS, "src");
    dest = f.createResource(NS, "dest");
    sim = f.createResource(NS, "similarity");
    inverse = f.createResource(NS, "inverse");

    loop = f.createResource(NS, "loop"); // used for set semantics in Range, Domain (selectors)
  }

  public static Model asModel(Model empty, Set s) throws ModelException {

    if(s == null)
      return null;

    NodeFactory f = empty.getNodeFactory();
    MapVocabulary M = new MapVocabulary(f);

    for(Iterator it = s.iterator(); it.hasNext();) {

      Resource r = (Resource)it.next();
      empty.add(f.createStatement(r, M.loop, r));
    }
    return empty;
  }

  public static Set asSet(Model m) throws ModelException {

    if(m == null)
      return null;

    MapVocabulary M = new MapVocabulary(m.getNodeFactory());

    Set res = new HashSet();

    for(Enumeration en = m.find(null, M.loop, null).elements(); en.hasMoreElements();) {

      Statement st = (Statement)en.nextElement();
      Resource r = (Resource)st.subject();
      res.add(r);
    }

    return res;
  }
}
