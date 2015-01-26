package edu.stanford.db.rdf.syntax.generic;

import org.w3c.rdf.model.*;
import org.w3c.rdf.syntax.*;
import org.w3c.rdf.util.*;
import edu.stanford.db.rdf.syntax.generic.*;
import edu.stanford.db.xml.util.*;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

import java.util.*;
import java.io.*;
import java.net.*;

public class TripleParser extends GenericXML2RDF {

  static final String N_MODEL = "model";
  static final String N_STMT = "stmt";
  static final String N_CACHE = "cache";
  static final String N_S = "s";
  static final String N_P = "p";
  static final String N_O = "o";
  static final String N_R = "r";
  static final String N_L = "l";

  /*
  static final String N_ID = "id";
  static final String N_IDREF = "nsref";
  static final String N_URI = "uri";
  static final String N_NSID = "nsid";
  static final String N_NSREF = "nsref";
  */

  static final QName A_ID = new QName("id");
  static final QName A_IDREF = new QName("idref");
  static final QName A_URI = new QName("uri");
  static final QName A_NSID = new QName("nsid");
  static final QName A_NSREF = new QName("nsref");
  static final QName A_NS = new QName("ns");
  static final QName A_NAME = new QName("name");

  /*
  static final QName R_MODEL = new QName(NS, N_MODEL);
  static final QName R_STMT = new QName(NS, "stmt");
  static final QName R_CACHE = new QName(NS, "cache");
  static final QName R_S = new QName(NS, "s");
  static final QName R_P = new QName(NS, "p");
  static final QName R_O = new QName(NS, "o");
  static final QName R_R = new QName(NS, "r");
  static final QName R_L = new QName(NS, "l");

  static final int E_MODEL = 0;
  static final int E_STMT = 1;
  static final int E_S = 2;
  static final int E_P = 3;
  static final int E_O = 4;
  */

  HashMap seenModels, seenStmts, seenNs;
  //  Model root;
  List tokens = new ArrayList();
  int nesting = 0; // nesting depth

  public static final String NS = "http://www.interdataworking.com/rdf/syntax/triples/0.1/";

  //  int expect;

  public TripleParser() {
    super();
  }

  void push(Object o) {
    //    System.err.println("" + tokens.size() + " token: " + o);
    tokens.add(o);
  }

  Object pop() {
    return tokens.size() > 0 ? tokens.remove(tokens.size()-1) : null;
  }


  // implements ContentHandler
  public void startDocument() throws SAXException {

    super.startDocument();

    seenModels = new HashMap();
    seenStmts = new HashMap();
    seenNs = new HashMap();
    //    pushExpect(E_MODEL);
//     try {
//     } catch (Exception any) {
//       throw new SAXException(any);
//     }
  }

  public void endDocument() throws SAXException {

    super.endDocument();
    if(tokens.size() > 0)
      error("Remaining " + tokens.size() + " tokens on stack: " + tokens);
  }

  void error(String msg) throws SAXException {

    throw new SAXParseException(msg, locator);
  }

  /*
  void expected(QName el) throws SAXException {

    if(!el.equals(current.getName()))
      error("Element " + el + " expected, " + current.getName() + " encountered instead");
  }

  void pushExpect(int e) {

    states.add(e);
    expect = e;
  }

  void setExpect(int e) {

    expect = e;
  }

  void popExpect() {

    expect = states.remove(states.size()-1);
  }
  */

  public void startElement(String uri, String local, String qname, Attributes attr) throws SAXException {

    _startElement(uri, local, attr);
    // System.out.println("Started: " + current.getName());

    if(NS.equals(uri)) {

      if(N_MODEL.equals(local)) {

	if(nesting > 0)
	  error("Nested models are not supported in the current version of RDFConsumer");
	nesting++;

      } else if(N_STMT.equals(local)) {

	nesting++;
      }
    }

    /*
    QName name = current.getName();

    try {
      switch(expect) {

      case E_NODE:

	if(R_MODEL.equals(name)) {
	  setExpect(E_STMT);
	} else if(R_R.equals(name)) {
	  popExpect();
	} else if(R_L.equals(name)) {
	  popExpect();
	} else
	  error("Expected " + R_MODEL + " or " + R_R + " or " + R_L + ", encountered " + name + " instead");

      case E_MODEL:

	expected(R_MODEL);
	setExpect(E_STMT);
	break;

      case E_STMT:

	if(R_CACHE.equals(name)) {
	  // same expect
	} else if(R_STMT.equals(name)) {
	  setExpect(R_S);
	} else
	  error("Expected " + R_CACHE + " or " + R_STMT + ", encountered " + name + " instead");
	
	break;

      case E_S:

	expected(R_S);
	pushExpect(R_P);
	setExpect(E_NODE);
	break;

      case E_P:

	expected(R_P);
	pushExpect(R_O);
	setExpect(E_NODE);
	break;

      }


      XMLElement xe = (XMLElement)current;
      xe.nodeID = nodeFactory.createUniqueResource();
      XMLElement parent = (XMLElement)current.getParent();

      Resource elName = nodeFactory.createResource(xe.getName().getNamespace(), xe.getName().getLocalName());
      createStatement(xe.nodeID, NAME, elName);

      if(parent != null) {
	parent.childNum++;
	Statement st = createStatement(parent.nodeID,
				       CHILD,
				       xe.nodeID);
	createStatement(st, ORDER, nodeFactory.createLiteral(parent.childNum));
      } else
	root = xe;
      
      for(Enumeration en = xe.getAttributes(); en.hasMoreElements();) {
	
      	QName name = (QName)en.nextElement();
	String value = current.getAttribute(name);
	
	createStatement(xe.nodeID,
			nodeFactory.createResource(name.getNamespace(), name.getLocalName()),
			nodeFactory.createLiteral(value));
      }
    } catch (Exception any) {
      throw new SAXException(any);
    }
    */

  }

  public void endElement(String uri, String local, String qname)  throws SAXException {

    try {

      QName name = current.getName();
      uri = name.getNamespace();
      local = name.getLocalName();

      if(NS.equals(uri)) {

	if(N_R.equals(local)) {

	  String ruri = current.getAttribute(A_URI);

	  if(ruri != null) {
	    push(nodeFactory.createResource(ruri));

	  } else {

	    String rname = current.getAttribute(A_NAME);

	    //	    System.err.println("Resource name: " + rname);

	    String rnsref = current.getAttribute(A_NSREF);
	    String rns = null;
	  
	    if(rnsref != null) {
	      // resolve from cache
	      rns = (String)seenNs.get(rnsref);
	      if(rns == null)
		error("Unresolved namespace reference: " + rnsref);

	    } else {
	      rns = current.getAttribute(A_NS);
	      String rnsid = current.getAttribute(A_NSID);
	      if(rnsid != null) {
		// put to cache
		seenNs.put(rnsid, rns);
	      }
	    }
	    // put token to stack
	    push(nodeFactory.createResource(rns, rname));
	  }

	} else if(N_L.equals(local)) {

	  push(nodeFactory.createLiteral(current.getValue()));

	} else if(N_CACHE.equals(local)) {

	  // reset cache
	  seenNs.clear();
	  seenStmts.clear();

	} else if(N_S.equals(local)) {

	  // make subject on stack
	  push(N_S);

	} else if(N_P.equals(local)) {

	  push(N_P);

	} else if(N_O.equals(local)) {

	  push(N_O);

	} else if(N_STMT.equals(local)) {

	  // statement reference?
	  String idref = current.getAttribute(A_IDREF);
	  if(idref != null) {
	    Statement st = (Statement)seenStmts.get(idref);
	    if(st == null)
	      error("Dangling statement reference: " + idref);
	    else {
	      if(nesting > 2)
		push(st);
	      else if(nesting == 2) { // one for model, one for statement
		consumer.addStatement(st);
	      }
	    }

	  } else {
	    // new statement definition

	    // read o,p,s from stack
	    Object s = null, p = null, o = null;

	    for(int i=0; i < 3; i++) {

	      Object type = pop();

	      if(N_O.equals(type))
		o = pop();
	      else if(N_P.equals(type))
		p = pop();
	      else if(N_S.equals(type))
		s = pop();
	      else
		error("Invalid token on stack, subject, predicate or object expected");
	    }

	    if(!(s instanceof Resource) || !(p instanceof Resource) || !(o instanceof RDFNode))
	      error("Invalid statement, subject, predicate or object missing or type is wrong");

	    Statement st = createStatement((Resource)s, (Resource)p, (RDFNode)o);
	    if(nesting > 2)
	      push(st);

	    String id = current.getAttribute(A_ID);
	    if(id != null)
	      seenStmts.put(id, st);
	  }

	  nesting--;


	} else if(N_MODEL.equals(local)) {

	  nesting--;

	  /*
	  // model reference?
	  String idref = current.getAttribute(A_IDREF);
	  if(idref != null) {
	    Model m = (Model)seenModels.get(idref);
	    if(m == null)
	      error("Dangling model reference: " + idref);
	    else {
	      push(m);
	    }

	  } else {
	    // new model definition, do nothing
	  }
	  */
	}

	//	  System.out.println("Ended: " + current.getName());

      }

    } catch (Exception any) {
      throw new SAXException(any);
    }

    _endElement(uri, local);
  }

  protected Statement createStatement(Resource subject, Resource predicate, RDFNode object) throws ModelException {

    Statement st = nodeFactory.createStatement(subject, predicate, object);
    if(nesting == 2) // one for model, one for statement
      consumer.addStatement(st);
    return st;
  }

  public static void main(String[] args) throws Exception {

    _main(args[0], new TripleParser());
  }
}

