package edu.stanford.db.rdf.syntax.generic;

import org.w3c.rdf.model.*;
import org.w3c.rdf.implementation.model.StatementImpl;
import org.w3c.rdf.syntax.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import edu.stanford.db.xml.util.*;

import java.util.*;
import java.io.*;
import java.net.*;

public class GenericXML2RDF extends GenericParser implements RDFParser {

  static final public String REVISION = "Generic RDF/XML parser v0.3 2000-10-31";

  protected NodeFactory nodeFactory;
  protected RDFConsumer consumer;

  public GenericXML2RDF () {

    super();
  }

  public GenericXML2RDF (boolean warn) {
    
    super(warn);
  }

  protected Resource createResource(String str) throws ModelException {

    // check whether we can extract namespace
    Enumeration en = getNamespaces();
    //    System.out.println("NS CHECK for " + str);
    while(en.hasMoreElements()) {

      String ns = (String)en.nextElement();
      //      System.out.println("NS CHECK " + str + " against " + ns);
      if(str.startsWith(ns))
	return createResource(ns, str.substring(ns.length()));
    }

    //    System.out.println("OPAQUE RESOURCE: " + str);
    return nodeFactory.createResource(create(str));
  }

  protected Resource createResource(String namespace, String name) throws ModelException {

    Resource r = nodeFactory.createResource(create(namespace), create(name));
    //    System.out.println("RESOURCE " + r.getNamespace() + " --- " + r.getLocalName());
    return r;
  }

  protected Resource createResource(QName name) throws ModelException {

    return createResource(name.getNamespace(), name.getLocalName());
  }


  protected Literal createLiteral(String str, boolean isXML)  throws ModelException {

    return nodeFactory.createLiteral(create(str));
  }
                                 
  protected Literal createLiteral(String str)  throws ModelException {

    return nodeFactory.createLiteral(create(str));
  }

  protected Statement createStatement(Resource subject, Resource predicate, RDFNode object) throws ModelException {

    Statement st;
    consumer.addStatement(st = nodeFactory.createStatement(subject, predicate, object));
    return st;
  }

  public void parse(InputSource source, RDFConsumer consumer) throws SAXException {

    this.source = source;
    this.consumer = consumer;

    try {
      this.nodeFactory = consumer.getNodeFactory();
      consumer.startModel();

      XMLReader p = createXMLReader();
      //      Parser p = ParserFactory.makeParser();

      // Register the handlers
      p.setEntityResolver(this);
      //      p.setDTDHandler (this);
      p.setContentHandler(this);
      p.setErrorHandler (errorHandler != null ? errorHandler : this);

      p.parse ( source );

      consumer.endModel();

    } catch (Exception any) {
      if(any instanceof SAXException)
      	throw (SAXException)any;
      else
      	throw new SAXException("Fatal error", any);
    }
  }

  protected static void _main(String url, RDFParser parser) throws IOException, MalformedURLException {

    InputSource source = getInputSource(url);
    RDFConsumer consumer = new DumpConsumer();
    ErrorStore handler = new ErrorStore();

    parser.setErrorHandler(handler);

    try {
      parser.parse(source, consumer);
    } catch (SAXException se) {
      System.err.println("Error during parsing: " + se.getMessage());
      Exception ex = (se.getException() != null) ? se.getException() : se;
      ex.printStackTrace(System.err);
    }
    
    String sErrors = handler.errors ();
    if (sErrors != null && sErrors.length() > 0)
      System.err.println ("Errors during parsing:\n"+sErrors);
  }

  public static void main (String args[]) throws Exception {

    if(args.length != 1) {
      System.err.println("Usage: java [-Dorg.xml.sax.parser=<classname>] GenericXML2RDF " +
			 //			 "[-fetch_schemas | -f] [-stream | -s] [-robust | -r] [ URI | filename ]");
			 "[ URI | filename ]");
      System.err.println ("This is revision " + REVISION);
      System.exit(1);
    }

    GenericXML2RDF gp;
    _main(args[0], gp = new GenericXML2RDF());

    Enumeration en = gp.getNamespaces();
    while(en.hasMoreElements())
      System.out.println("Namespaces used: " + en.nextElement());
  }

}

