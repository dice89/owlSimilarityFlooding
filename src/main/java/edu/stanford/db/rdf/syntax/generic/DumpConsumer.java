package edu.stanford.db.rdf.syntax.generic;

import java.util.*;
import java.io.*;
import org.w3c.rdf.model.*;
import org.w3c.rdf.syntax.*;
import org.w3c.rdf.implementation.model.StatementImpl;
import org.w3c.rdf.implementation.model.NodeFactoryImpl;

public class DumpConsumer implements RDFConsumer, RDFSerializer {
      
  int num = 0;
  NodeFactory f = new NodeFactoryImpl();

  public void startModel () {}
  public void endModel () {
    System.out.println("Total statements: " + num);
  }

  public NodeFactory getNodeFactory() {
    return f;
  }

  public void addStatement (Statement s) {
    System.out.println(s.toString());
    num++;
  }

  public void serialize(Model model, Writer w) throws SerializationException, IOException, ModelException {

    for(Enumeration en = model.elements(); en.hasMoreElements();) {

      Statement st = (Statement)en.nextElement();
      w.write(st.toString());
      w.write('\n');
    }
    w.flush();
  }

}

