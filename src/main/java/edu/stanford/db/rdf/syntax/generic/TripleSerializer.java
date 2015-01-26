package edu.stanford.db.rdf.syntax.generic;

import org.w3c.rdf.model.*;
import org.w3c.rdf.syntax.*;
import java.util.*;
import java.io.Writer;
import java.io.IOException;
import edu.stanford.db.xml.util.GenericSerializer;
//import org.w3c.rdf.implementation.model.*;

/**
 * RDF serializer interface.
 */

public class TripleSerializer implements RDFSerializer {

  static final int MAX_STMT_CACHE = 1000;
  static final int MAX_NS = 1000;

  public static final String NS = "http://www.interdataworking.com/rdf/syntax/triples/0.1/";

  HashMap seenModels, seenStmts, seenNs;
  //  boolean firstIndent;

  /**
   * Serialize a given model into an XML character output stream.
   */
  public void serialize(Model m, Writer w) throws SerializationException, IOException, ModelException {

    //    firstIndent = true;
    seenModels = new HashMap();
    seenStmts = new HashMap();
    seenNs = new HashMap();
    w.write("<?xml version=\"1.0\"?>");
    writeModel(m, w, 0);
    seenModels = null;
    w.flush();
  }

  void indent(int depth, Writer w) throws IOException {

    //    if(!firstIndent)
      w.write('\n');
      //    firstIndent = false;
    for(int i=0; i < depth; i++)
      w.write('\t');
  }

  public void writeModel(Model m, Writer w, int depth) throws ModelException, IOException {

    String id = (String)seenModels.get(m);

    if(id == null)
      seenModels.put(m, id = "m" + seenModels.size());
    else {
      w.write("<model idref=\"" + id + "\"/>");
      return;
    }

    indent(depth, w);
    w.write("<model ");
    boolean writeStmtCacheDecl = false;
    if(depth == 0) {
      w.write("xmlns=\"" + NS + "\" ");
      writeStmtCacheDecl = true;
    }
    w.write("id=\"" + id + "\">");

    for(Enumeration en = m.elements(); en.hasMoreElements();) {

      if(writeStmtCacheDecl || seenStmts.size() >= MAX_STMT_CACHE || seenNs.size() >= MAX_NS) {
	indent(depth+1, w);
	w.write("<cache maxstmt=\"" + MAX_STMT_CACHE + "\" maxns=\"" + MAX_NS + "\"/>");
	seenStmts.clear();
	seenNs.clear();
	writeStmtCacheDecl = false;
      }

      Statement t = (Statement)en.nextElement();
      writeStatement(t, w, depth+1, true);
      w.flush();
    }
    //    writeStatement(new StatementImpl(new ResourceImpl("subj"), new ResourceImpl("pred"), m), w, depth+1, true);

    indent(depth, w);
    w.write("</model>\n");
  }

  void writeNode(String tag, RDFNode n, Writer w, int depth) throws ModelException, IOException {

    indent(depth, w);
    w.write("<" + tag + ">");
    if(n instanceof Literal) {
      String value = ((Literal)n).getLabel();
      boolean isCDATA = GenericSerializer.isCDATA(value);
      if(isCDATA) {
	w.write("<l xml:space=\"preserve\"><![CDATA[");
	GenericSerializer.writeCDATA(value, w);
	w.write("]]>");
      } else {
	w.write("<l>");
	w.write(n.getLabel());
      }
      w.write("</l>");
    } else if(n instanceof Statement)
      writeStatement((Statement)n, w, depth, false);
    else if(n instanceof Model) {
      writeModel((Model)n, w, depth);
    } else if(n instanceof Resource) {
      Resource r = (Resource)n;
      String ns = r.getNamespace();
      String name = r.getLocalName();
      if(ns != null && ns.length() > 0) {
	w.write("<r name=\"" + name + "\" ");
	String abb = (String)seenNs.get(ns);
	if(abb == null) {
	  seenNs.put(ns, abb = ("ns" + seenNs.size()));
	  w.write("ns=\"" + ns + "\" nsid=\"" + abb + "\"/>");
	} else
	  w.write("nsref=\"" + abb + "\"/>");
      } else
	w.write("<r uri=\"" + r.getURI() + "\"/>");
    } else { // resource
      w.write(n.getLabel());
    }
    w.write("</" + tag + ">");
  }

  void writeStatement(Statement st, Writer w, int depth, boolean forceBreak) throws ModelException, IOException {
    
    String id = (String)seenStmts.get(st);

    if(id == null)
      seenStmts.put(st, id = "s" + seenStmts.size());
    else {
      if(forceBreak)
	indent(depth, w);
      w.write("<stmt idref=\"" + id + "\"/>");
      return;
    }
    if(forceBreak)
      indent(depth, w);
    w.write("<stmt id=\"" + id + "\">");
    writeNode("s", st.subject(), w, depth+1);
    writeNode("p", st.predicate(), w, depth+1);
    writeNode("o", st.object(), w, depth+1);
    w.write("</stmt>");
  }
}

