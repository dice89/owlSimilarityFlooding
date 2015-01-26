package edu.stanford.db.xml.util;

import org.w3c.rdf.syntax.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

import java.util.*;
import java.io.*;
import java.net.*;
import java.lang.ref.WeakReference;

public class GenericParser extends DefaultHandler { /*EntityResolver, DTDHandler, DocumentHandler, */

  static final public String REVISION = "Generic XML parser v0.4 2002-03-30";

  protected ErrorHandler errorHandler = null;
  protected Locator locator = new LocatorImpl();
  protected InputSource source;
  protected Element current;

  // collect all namespaces here
  protected Hashtable namespaces = new Hashtable();

  static final String DEFAULT_PARSER = "org.brownell.xml.aelfred2.SAXDriver";

  protected String parserClass = null;
  protected int numInParent = 0;
  protected int depth = 0;

  public GenericParser () {

    this(false);
  }

  public GenericParser (boolean warn) {

    initXMLParser(warn);
  }

  // override EntityResolver implementation
  public InputSource resolveEntity(String publicId, String systemId) throws SAXException {

    System.err.println("[Warning] ignoring external entity " + systemId);
    return new InputSource(new CharArrayReader(new char[0]));
  }

  // @since 2000-10-31
  // optimization for string generation
  protected static WeakHashMap atoms = new WeakHashMap();


  public static QName createQName(String ns, String ln) {

    return new QName(create(ns), create(ln));
  }


  public static String create(String s) {

    if(s == null)
      return null;

    WeakReference ref = (WeakReference)atoms.get(s);
    if(ref == null)
      atoms.put(s, new WeakReference(s));
    else {
      Object o = ref.get();
      if(o == null) { // just trashed by GC
	atoms.put(s, new WeakReference(s));
      } else
	s = (String)o;
    }
    return s;
  }

  protected void initXMLParser(boolean warn) {

    try {
      if((parserClass = System.getProperty("org.xml.sax.parser")) == null) {
	if(warn) {
	  System.err.println("Warning: using the default XML parser (" + getDefaultParserClass() + ").\n" +
			     "Override for IBM xml4j is: -Dorg.xml.sax.parser=com.ibm.xml.parser.SAXDriver");
	  //	  System.getProperties().put("org.xml.sax.parser", getDefaultParserClass());
	}
      } else // parser has been explicitly overridden
	return;

    } catch (SecurityException any) {
      // I'm an applet
      if(warn)
	System.err.println("Warning: could not access system properties, using default XML parser (" + getDefaultParserClass() + ").");
    }
    parserClass = getDefaultParserClass();

    //    new Exception().printStackTrace(System.err);
  }

  protected String getDefaultParserClass() {
    
    return DEFAULT_PARSER;
  }

  protected String getParserClass() {

    return parserClass;
  }

  protected XMLReader createXMLReader() {

    //      XMLReader p = XMLReaderFactory.createXMLReader();
    return createXMLReader(getParserClass());
  }


  /**
   * Generate a warning message as a string
   */
    public void addWarning (String sMsg) throws SAXException {
      errorHandler.warning(new SAXParseException(sMsg, locator));
    }
  
  /**
   * Generate an error message as a string
   */
   public void addError (String sMsg) throws SAXException {
     errorHandler.error(new SAXParseException(sMsg, locator));
   }
  
//   public void setDocumentLocator (Locator locator) {

//     this.locator = locator;
//   }
  
  public void setErrorHandler (ErrorHandler handler) {
    this.errorHandler = handler;
  }

  protected String getSourceURI() {

    return source.getSystemId();
  }

  public void startElement (String uri, String name, String qName, Attributes atts) throws SAXException {

    _startElement(uri, name, atts);
    System.out.println("Start element: " + current.getName());
  }

  public Enumeration getNamespaces() {

    return namespaces.keys();
  }

  public int getDepth() {

    return depth;
  }

  protected void addNamespace(String uri) {

    if(!"".equals(uri))
      namespaces.put(uri, uri);
  }

  protected void _startElement (String uri, String name, Attributes al) throws SAXException {

    Element el = createElement();
    el.setName(new QName(uri, name));
    el.setNumInParent(numInParent);
    numInParent = 1;
    addNamespace(uri);

    // add attributes
    for (int x = 0; x < al.getLength(); x++) {
      String aURI = al.getURI(x);
      addNamespace(aURI);

      // take element's namespace if none specified
      // FIXED: this was not XML-NS compliant!
//       if("".equals(aURI))
// 	aURI = uri;
      QName aName = new QName(aURI, al.getLocalName(x));
      String aValue = al.getValue (x);
      //      System.out.println("Adding property: " + aName + "=" + aValue);
      el.setAttribute(aName, aValue);
    }

    if(current == null)
      current = el;
    else {
      current.setChild(el);
      el.setParent(current);
      current = el;
    }

    depth++;
  }

  protected Element createElement() {
    return new Element();
  }

  public void endElement (String uri, String name, String qName) throws SAXException {

    System.out.println("End element: " + current.getName());
    _endElement(uri, name);
  }

  protected void _endElement (String uri, String name) throws SAXException {

    numInParent = current.getNumInParent() + 1;
    current = current.getParent();
    depth--;
  }

  protected boolean preserveWhiteSpace() {
    return false;
  }

  public void characters (char ch[], int start, int length)
    throws SAXException {
  
      String s = new String (ch, start, length);
      String sTrimmed = s.trim();
      if (sTrimmed.length() > 0 || preserveWhiteSpace()) {
	// System.out.println("Adding characters: \"" + s + "\"");
	current.setValue(sTrimmed);
      }
  }

  public static XMLReader createXMLReader(String className) {

    try {
      // Get the named class.
      Class c = Class.forName(className);
      // Instantiate the parser.
      return (XMLReader)(c.newInstance());
    } catch (Exception any) {
      throw new RuntimeException("Could not instantiate XML parser: " + any);
    }
  }    

  public static InputSource getInputSource(String urlStr) throws MalformedURLException, IOException {

    InputStream in = null;
    URL url = null;

    if(urlStr != null) {

      try {
	url = new URL (urlStr);
      } catch (Exception e) {
	url = new URL ("file", null, urlStr);
      }
      in = url.openStream();
      
    } else {
      // read from stdin
      in = System.in;
    }
    
    Reader r = new BufferedReader(new InputStreamReader(in));
    InputSource source = new InputSource(r);
    if(urlStr != null)
      source.setSystemId(url.toString());

    return source;
  }

  protected void _parse(String url) throws IOException, MalformedURLException {

    InputSource source = getInputSource(url);
    ErrorStore handler = new ErrorStore();

    try {

      XMLReader p = createXMLReader();
      //      Parser p = ParserFactory.makeParser();

      // Register the handlers
      p.setEntityResolver(this);
      //      p.setDTDHandler (this);
      p.setContentHandler(this);
      p.setErrorHandler (handler); // errorHandler != null ? errorHandler : this);

      p.parse ( source );

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
      System.err.println("Usage: java [-Dorg.xml.sax.parser=<classname>] GenericParser " +
			 "[ URI | filename ]");
      System.err.println ("This is revision " + REVISION);
      System.exit(1);
    }

    GenericParser gp = new GenericParser();
    gp._parse(args[0]);

    Enumeration en = gp.getNamespaces();
    while(en.hasMoreElements())
      System.out.println("Namespaces used: " + en.nextElement());
  }

}

