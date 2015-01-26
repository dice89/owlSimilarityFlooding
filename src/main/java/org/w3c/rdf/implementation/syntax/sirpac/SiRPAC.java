/**
 * SiRPAC - Simple RDF Parser & Compiler
 *
 * ==========================================================================
 * WARNING: THIS IS NOT THE ORIGINAL W3C RELEASE OF SIRPAC!
 *
 * This version has been modified by Sergey Melnik (melnik@db.stanford.edu)

 * It contains incompatible changes:
 *  1. The classes Resource, Literal, Triple are now interfaces.
 *  2. The class Property has been removed since it is not possible to
 *     determine whether a particular resource is a property at
 *     parsing time.
 * It also contains numerous bug fixes.
 *
 * ==========================================================================
 *
 * ORIGINAL COPYRIGHT STATEMENT:
 *
 * Copyright � World Wide Web Consortium, (Massachusetts Institute of
 * Technology, Institut National de Recherche en Informatique et en
 * Automatique, Keio University).
 *
 * All Rights Reserved.
 *
 * Please see the full Copyright clause at
 * <http://www.w3.org/Consortium/Legal/copyright-software.html>
 *
 * This program translates RDF descriptions into corresponding
 * triple representation.
 * This version uses SAX V1.0 available at <http://www.microstar.com/XML/SAX/>
 *
 * $Log: SiRPAC.java,v $
 * Revision 1.4  2001/10/13 01:43:55  stefan
 *
 *
 *
 * BUG NUMBER:
 * CONDITION:
 * FIX: Sergey fixed namespace bug
 *
 * Revision 1.3  2001/09/17 03:39:16  stefan
 * *** empty log message ***
 *
 * Revision 1.14  1999/05/06 12:30:57  jsaarela
 * Fixed bug related to resource="#id" reference management.
 *
 * Revision 1.21  1999/05/06 12:20:18  jsaarela
 * Fixed bug related to resource="#id" management.
 *
 * Revision 1.20  1999/05/04 14:52:43  jsaarela
 * Literal value now tells if it is well-formed XML.
 * Improved entity management in Data nodes.
 *
 * Revision 1.19  1999/04/26 14:51:27  jsaarela
 * URI resolution improved.
 *
 * Revision 1.18  1999/04/01 09:32:48  jsaarela
 * SiRPAC distribution release V1.11 on 1-Apr-99
 *
 * Revision 1.17  1999/03/10 08:54:40  jsaarela
 * Management of parseType="Literal" and "Resource" now equally
 * tested.
 *
 * Revision 1.16  1999/01/13 15:00:30  jsaarela
 * Finished conformance testing with PR-rdf-syntax-19990105 version
 * of the RDF M&S spec.
 *
 *
 * @author      Janne Saarela <jsaarela@w3.org>
 */
package org.w3c.rdf.implementation.syntax.sirpac;

import org.w3c.rdf.model.*;
import edu.stanford.db.xml.util.*;
import edu.stanford.db.rdf.syntax.generic.DumpConsumer;
import org.w3c.rdf.util.*;
import org.w3c.rdf.syntax.*;
import org.w3c.rdf.vocabulary.rdf_syntax_19990222.RDF;
import org.w3c.rdf.vocabulary.rdf_schema_19990303.RDFS;
import edu.stanford.db.rdf.vocabulary.order_20000527.RDFX;

import org.xml.sax.HandlerBase;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.AttributeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.DTDHandler;
import org.xml.sax.DocumentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;
import org.xml.sax.Parser;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.xml.sax.helpers.*;

import java.net.URL;
import java.util.*;
import java.io.*;

/**
 * Modified version of SiRPAC adapted to support the new API, streaming, robust parsing etc.
 */

public class SiRPAC implements EntityResolver, DTDHandler, DocumentHandler,
                               /*ErrorHandler, DataSource,*/ RDFParser {

  final static public String    REVISION = "$Id: SiRPAC.java,v 1.4 2001/10/13 01:43:55 stefan Exp $";
  public final static String    RDFMS = RDF._Namespace; // new String ("http://www.w3.org/1999/02/22-rdf-syntax-ns#");
  public final static String    RDFSCHEMA = RDFS._Namespace; // new String ("http://www.w3.org/TR/1999/PR-rdf-schema-19990303#");
  public final static String    XMLSCHEMA = new String ("xml");

  static final QName UNQUALIFIED_parseType = new QName(null, "parseType");
  static final QName RDFMS_parseType = new QName(RDFMS, "parseType");
  static final String RDFMS_parseType_Resource = "Resource";
  static final String XML_space = XMLSCHEMA + "space";
  static final String XML_space_preserve = "preserve";
  static final String XMLNS = "xmlns";
  static final String XMLNS_COLON = "xmlns:";
  static final String _RDF = "RDF";
  static final String DESCRIPTION = "Description";
  //  static final String RDFMS_type = RDFMS + "type";
  static final QName UNQUALIFIED_about = new QName(null, "about");
  static final QName RDFMS_about = new QName(RDFMS, "about");
  static final QName RDFMS_bagID = new QName(RDFMS, "bagID");
  static final QName UNQUALIFIED_ID = new QName(null, "ID");
  static final QName RDFMS_ID = new QName(RDFMS, "ID");
  static final QName UNQUALIFIED_resource = new QName(null, "resource");
  static final QName RDFMS_resource = new QName(RDFMS, "resource");
  static final String RDFMS_aboutEach = RDFMS + "aboutEach";
  static final String RDFMS_aboutEachPrefix = RDFMS + "aboutEachPrefix";
  static final String RDFMS_RDF = RDFMS + "RDF";
  static final String RDFMS_Description = RDFMS + "Description";
  static final String RDFMS_Seq = RDFMS + "Seq";
  static final String RDFMS_Alt = RDFMS + "Alt";
  static final String RDFMS_Bag = RDFMS + "Bag";

  // SM:experimental
  boolean ENABLE_EXPERIMENTAL = true;

  public void useExperimentalFeatures(boolean b) {

    ENABLE_EXPERIMENTAL = b;
  }

  private Stack         m_namespaceStack = new Stack ();
  private Stack         m_elementStack = new Stack ();
  private Element               m_root = null;
  //    private Vector          m_triples = new Vector ();
  private String                m_sSource = null;
  private boolean                m_sSourceModified = false;

  /**
   * The walk-through of RDF schemas requires two lists
   * shared by all SiRPAC instances
   * 1. s_vNStodo - list of all namespaces SiRPAC should still vist
   * 2. s_vNSdone - list of all namespaces SiRPAC has gone through
   */

   private Vector s_vNStodo = new Vector ();
   private Vector s_vNSdone = new Vector ();

  /**
   * The following two variables may be changed on the fly
   * to change the behaviour of the parser
   */
  private boolean               m_bCreateBags = false;
  private boolean               m_bFetchSchemas = false;

  /**
   * The following flag indicates whether the XML markup
   * should be stored into a string as a literal value
   * for RDF
   */
  private Stack         m_parseTypeStack = new Stack ();
  private Stack         m_parseElementStack = new Stack ();
  private String                m_sLiteral = new String ();

  /**
   * Support for multiple RDFConsumer objects that can
   * receive notifications about new triples
   */
  //    private Vector          m_consumers = new Vector ();
  private RDFConsumer m_consumer;

  /**
   * The current source of data
   */
  private InputSource m_RDFsource = null;
  //    private RDFSource               m_RDFsource = null;


  /**
   * Which XML parser to use
   */
  private String                m_sXMLParser = null;

  boolean ignoreExternalEntities = false;
  NodeFactory nodeFactory;


  static final String DEFAULT_PARSER = "org.brownell.xml.aelfred2.SAXDriver";

  public SiRPAC () {

    try {
      if(System.getProperty("org.xml.sax.parser") == null) {
// 	System.err.println("Warning: using the default XML parser (" + DEFAULT_PARSER + ").\n" +
// 			   "Override for IBM xml4j is: -Dorg.xml.sax.parser=com.ibm.xml.parser.SAXDriver");
	System.getProperties().put("org.xml.sax.parser", DEFAULT_PARSER);
      }
    } catch (SecurityException any) {
      // I'm an applet
      m_sXMLParser = DEFAULT_PARSER;
    }
  }

  public SiRPAC (String sXMLParser) {

    m_sXMLParser = sXMLParser;
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
    errorHandler.fatalError(new SAXParseException(sMsg, locator));
  }

  /**
   * Methods to determine whether we are parsing
   * parseType="Literal" or parseType="Resource"
   */
  public boolean parseLiteral() {
    if (!m_elementStack.empty()) {
      for (int x = m_elementStack.size()-1; x >= 0; x--) {
        Element e = (Element)m_elementStack.elementAt(x);
        String sParseType = e.getAttribute(RDFMS_parseType);
        if (sParseType != null) {
          if (!sParseType.equals (RDFMS_parseType_Resource)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  public boolean parseResource() {
    if (!m_elementStack.empty()) {
      for (int x = m_elementStack.size()-1; x >= 0; x--) {
        Element e = (Element)m_elementStack.elementAt(x);
        String sParseType = e.getAttribute(RDFMS_parseType);
        if (sParseType != null) {
          if (sParseType.equals (RDFMS_parseType_Resource))
            return true;
        }
      }
    }
    return false;
  }


  public boolean preserveWhiteSpace() {
    if (!m_elementStack.empty()) {
      int x = m_elementStack.size()-1;
      //      for (int x = m_elementStack.size()-1; x >= 0; x--) {
      Element e = (Element)m_elementStack.elementAt(x);
      String sParseType = e.getAttribute(XML_space);
      if (sParseType != null) {
                                //        System.err.println("---PRESERVE " + (m_elementStack.size()-1-x) + " " +  "preserve".equals(sParseType));
        return XML_space_preserve.equals(sParseType);
      }
      /*
        if (!sParseType.equals ("Resource")) {

        System.out.println("--- CHECKING (" + x + "): " + e.name() + " " + this);
        if(e.preserveWhiteSpace()) {
        System.err.println("--- DO PRESERVE!");
        return true;
        }*/
      //      }
    }
    return false;
  }

  /**
   * createBags method allows one to determine whether SiRPAC
   * produces Bag instances for each Description block.
   * The default setting is to generate them.
   */
  public void createBags (boolean b) {
    m_bCreateBags = b;
  }

  /**
   * Set whether parser recursively fetches and parses
   * every RDF schema it finds in the namespace declarations
   */
  public void fetchSchemas (boolean b) {
    m_bFetchSchemas = b;
  }

  /**
   * setSource methods saves the name of the source document for
   * later inspection if needed
   */
  public void setSource (String sSource) {

    m_sSource = sSource;

    if(m_sSource != null && !(sSource.endsWith("#") || sSource.endsWith("/") || sSource.endsWith(":"))) {
      m_sSource += "#";
      m_sSourceModified = true;
    }

    // update list of known namespaces
    //    System.out.println("SET SOURCE TO " + m_sSource);
    if(m_sSource != null)
      s_vNSdone.addElement (m_sSource);
  }

  public String source () {
    return m_sSource;
  }

  /**
   * Return all non-RDF namespace URIs recognized by the parser
   */
  public Enumeration listNamespaces () {
    return s_vNSdone.elements();
  }

  /**
   * Return the full namespace URI for a given prefix <i>sPrefix</i>.
   * The default namespace is identified with <i>xmlns</i> prefix.
   * The namespace of <i>xmlns</i> attribute is an empty string.
   */
  public String namespace (String sPrefix) throws SAXException {
    if (sPrefix == null) {
      sPrefix = XMLNS;
    }
    for (int x = m_namespaceStack.size()-1; x >=0; x--) {
      Hashtable ht = (Hashtable)m_namespaceStack.elementAt (x);
      String sURI = (String)ht.get (sPrefix);
      if (sURI != null)
        return sURI;
    }
    /**
     * Give error only if
     * 1. the prefix is not from the reserved xml namespace
     * 2. the prefix is not xmlns which is to look for the default
     *    namespace
     */
    if (sPrefix.equals (XMLSCHEMA)) {
      return XMLSCHEMA;
    } else if (sPrefix.equals (XMLNS)) {
      return "";
    } else {
      addError ("Unresolved namespace prefix "+sPrefix);
    }
    return "";
  }

  /**
   * One can register multiple RDFConsumer objects which will
   * receive notifications about new triples
   */
  /*
    public void register (RDFConsumer c) {
    m_consumers.addElement (c);
    }
  */

  /**
   * Remove an RDFConsumer object from SiRPAC
   */
  /*
    public void unregister (RDFConsumer c) {
    m_consumers.removeElement(c);
    }
  */
  /**
   * Notify all registered consumers that were
   * are about to start parsing
   */
  /*
    public void startConsumers () {
    for (int x = 0; x < m_consumers.size(); x++) {
    RDFConsumer consumer = (RDFConsumer)m_consumers.elementAt(x);
    consumer.start (this);
    }
    }
  */
  /**
   * Notify all registered consumers that were
   * are at the end of the parsing process
   */
  /*
    public void endConsumers () {
    for (int x = 0; x < m_consumers.size(); x++) {
    RDFConsumer consumer = (RDFConsumer)m_consumers.elementAt(x);
    consumer.end (this);
    }
    }
  */

  public void setRDFSource (InputSource source) {
    m_RDFsource = source;
  }

  public InputSource getRDFSource () {
    return m_RDFsource;
  }

  public void setErrorHandler (ErrorHandler handler) {
    this.errorHandler = handler;
  }

  public void parse(InputSource source, RDFConsumer consumer) throws SAXException {

    if(errorHandler == null)
      errorHandler = new ErrorStore();
    //          System.out.println("ERROR HANDLER: " + errorHandler);

    try {
      setRDFSource(robustMode ? RDFReader.filter(source) : source);
      m_consumer = consumer;
      m_consumer.startModel();
      nodeFactory = m_consumer.getNodeFactory();

      fetchRDF();

      m_consumer.endModel();

    } catch (Exception any) {
      if(any instanceof SAXException)
        throw (SAXException)any;
      else
        throw new SAXException("Fatal error", any);
    }
  }

  private void fetchRDF () throws Exception {

    //  startConsumers ();

    //  try {
    // Create a new parser.
    Parser p = null;

    if (m_sXMLParser == null)
      p = ParserFactory.makeParser();
    else
      p = ParserFactory.makeParser(m_sXMLParser);

    // Register the handlers
    p.setEntityResolver(this);
    p.setDTDHandler (this);
    p.setDocumentHandler(this);
    p.setErrorHandler (errorHandler);

    //      StringReader sr = new StringReader (m_RDFsource.content());
    //      InputSource source = new InputSource (sr);

    //      source.setSystemId(m_RDFsource.url());
    //      setSource (m_RDFsource.url());
    if(source() == null)
      setSource( m_RDFsource.getSystemId() );

    // for unqualified
    Hashtable defNS = new Hashtable();
    if(source() != null)
      defNS.put(XMLNS, source() /*+ "#"*/); // ???
    m_namespaceStack.push(defNS);

    //      try {
    p.parse ( m_RDFsource );
    //      } catch (org.ginf.helpers.EndOfRDFException ok) {
    // We are done!
    //      }
    //      p.parse (m_RDFsource.url());
    // createBags (true);
    //        resolve ();
    //        processXML (root());
    //      root().linearize (0, System.out);

    //          } catch (SAXException e) {
    //      addError ("\n<br>Internal error "+e.getMessage());
    //      System.err.println("Internal error SAX "+e.getClass() + ", " + e + ", " + e.getMessage());
    //      if(e != null) {
    //        System.err.println("PRINTING STACK TRACE");
    //        e.printStackTrace(System.err);
    //        System.err.println("STACK TRACE DONE");
    //      }
    //  } catch (Exception e) {
    //      System.err.println("Internal error "+e.getClass() + ", " + e);
    //      e.printStackTrace(System.err);
    //  }

    //  endConsumers ();
  }

  public InputSource resolveEntity(String publicId, String systemId) throws SAXException {

    if(ignoreExternalEntities) {

      System.err.println("[Warning] ignoring external entity " + systemId);
      return new InputSource(new CharArrayReader(new char[0]));

    } else // default behavior
      return null;
  }

  public void notationDecl (String name, String publicId, String systemId) {
  }

  /**
   * Display unparsed entity declarations as they are reported.
   *
   * @see org.xml.sax.DTDHandler#unparsedEntityDecl
   */
  public void unparsedEntityDecl (String name,
                                  String publicId,
                                  String systemId,
                                  String notationName) {
  }

  Locator locator = new LocatorImpl();

  public void setDocumentLocator (Locator locator) {
    //      System.err.println("--- LOCATOR: " + locator);
    this.locator = locator;
  }

  public void startDocument () {
    //  m_sErrorMsg = "";
  }

  public void endDocument () throws SAXException {
  }

  public void doctype (String name, String publicID, String systemID) {
  }

  // SM
  private static final int smSKIPPING = 0;
  private static final int smRDF = 1;
  private static final int smDESCRIPTION = 2;
  private static final int smTOP_DESCRIPTION = 3;

  private int scanMode = smSKIPPING;
  private Element scanModeElement = null;
  // END SM

  /**
   * Called for each new element.
   * Build up the document tree using an element stack
   *
   * @exception SAXException Passed on since we don't handle it.
   */
  /*
    public void startElement (String name, AttributeList al) throws SAXException {
    try {
    protectedstartElement(name, al);
    } catch (Exception any) {
    any.printStackTrace(System.err);
    }
    }
  */

  public void startElement (String name, AttributeList al) throws SAXException {

    //      System.err.println("--> " + name + ", scan mode is: " + scanMode + ", el stack: " + m_elementStack.size() + ", ns stack: " + m_namespaceStack.size());

    Hashtable namespaces = new Hashtable ();
    // for unqualified
    //  namespaces.put("xmlns", source() + "#");

    /**
     * The following loop tries to identify special xmlns prefix
     * attributes and update the namespace stack accordingly.
     * While doing all this, it builds another AttributeList instance
     * which will hold the expanded names of the attributes
     * (I think this approach is only useful for RDF which uses
     * attributes as an abbreviated syntax for element names)
     */
    AttributesImpl newAL = new AttributesImpl ();

    int iLength = al.getLength ();
    if (iLength == 0) {
      // ohwell, no attributes
    } else for (int x = 0; x < iLength; x++) {
      String aName = al.getName (x);
      if (aName.equals (XMLNS)) {
        String aValue = al.getValue (aName);
        if (aValue != null && aValue.length() == 0 && source() != null)
          aValue = source();
        namespaces.put (aName, aValue);

                                // save all non-RDF schema addresses
        if (!hasString (s_vNStodo, aValue) &&
            !hasString (s_vNSdone, aValue) /* &&
            !aValue.startsWith (RDFMS) &&
            !aValue.startsWith (RDFSCHEMA) */) {
          s_vNStodo.addElement (aValue);
        }
      } else if (aName.startsWith (XMLNS_COLON)) {
        String aValue = al.getValue (aName);
        if (aValue != null && aValue.length() == 0 && source() != null)
          aValue = source();
        aName = aName.substring (6);
        namespaces.put (aName, aValue);

                                // save all non-RDF schema addresses
        if (!hasString (s_vNStodo, aValue) &&
            !hasString (s_vNSdone, aValue) /* &&
            !aValue.startsWith (RDFMS) &&
            !aValue.startsWith (RDFSCHEMA) */) {
          s_vNStodo.addElement (aValue);
        }
      }
    }

    /**
     * Recursively call myself to go through all the schemas
     */
    while (s_vNStodo.size() > 0) {
      String sURI = (String)s_vNStodo.elementAt(0);
      s_vNStodo.removeElementAt(0);
      s_vNSdone.addElement (sURI);
      //      System.err.println("NEW SCHEMA URI: " + sURI);

      if (m_bFetchSchemas)
	fetchSchema(sURI);

    }

    /**
     * Place new namespace declarations into the stack
     * (Yes, I could optimize this a bit, not it wastes space
     * if there are no xmlns definitions)
     */
    m_namespaceStack.push (namespaces);

    /**
     * Figure out the prefix part if it exists and
     * determine the namespace of the element accordingly
     */
    String sNamespace = null;
    String sElementName = null;
    Element newElement = null;
    int i = name.indexOf (':');
    String sPrefix2 = null;
    if (i > 0) {
      sPrefix2 = name.substring (0, i);
      sNamespace = namespace (sPrefix2);
      sElementName = name.substring (i+1);
    } else {
      sNamespace = namespace (XMLNS);
      sElementName = name;
    }

    // SM
    boolean setScanModeElement = false;

    switch(scanMode) {

    case smSKIPPING:
      {
                                // check for rdf:RDF
        if(RDFMS.equals(sNamespace)) {
          if(sElementName.equals(_RDF)) {
            scanMode = smRDF;
            setScanModeElement = true;
          } else if(sElementName.equals(DESCRIPTION)) {
            scanMode = smTOP_DESCRIPTION;
            setScanModeElement = true;
          }
        }//else {
                                //return;
                                // }
        break;
      }
    case smRDF:
      {
        scanMode = smDESCRIPTION;
        setScanModeElement = true;
        break;
      }
    }
    //  System.err.println("--- ELEMENT: " + sNamespace + " : " + sElementName + ", MODE=" + scanMode + ", flag=" + setScanModeElement + ", stack=" + m_elementStack.size() + ", RET: " + (scanModeElement != null ? scanModeElement.name() : "none"));

    // END SM

    /**
     * Finally look for attributes other than the special xmlns,
     * expand them, and place to the new AttributeListImpl
     */

    // speed up a bit...
    boolean parseLiteral = scanMode != smSKIPPING && parseLiteral();


    if(scanMode != smSKIPPING)

      for (int x = 0; x < iLength; x++) {
        String sAttributeNamespace = null;
        String aName = al.getName (x);
        if (!aName.startsWith (XMLNS)) {
          String aValue = al.getValue (aName);
          String aType = al.getType (aName);

          int iIndex = aName.indexOf (':');
          String sPrefix = null;
          if (iIndex > 0) {
            sPrefix = aName.substring (0, iIndex);
            sAttributeNamespace = namespace (sPrefix);
            aName = aName.substring (iIndex+1);

          } else {
	    // FIXED: no namespace defaulting for attributes!!!
	    // see XML namespaces spec; namespace remains null
	    /*
            if (sNamespace == null)
              sAttributeNamespace = namespace (XMLNS);
	      else
              sAttributeNamespace = sNamespace;
	    */
          }
          if (parseLiteral) {
            if (sPrefix == null) {
              sPrefix = "gen" + x; // x is a handy counter
            }
            newAL.addAttribute (sPrefix + ":", aName, null,
                                aType,
                                aValue);
            newAL.addAttribute (XMLNS_COLON, sPrefix, null,
                                aType,
                                sAttributeNamespace);
          } else {
            newAL.addAttribute (sAttributeNamespace, aName, null,
                                aType,
                                aValue);
          }
          /**
           * This call will try to see if the user is using
           * RDF look-alike elements from another namespace
           *
           * Note: you can remove the call if you wish
           */
          //            likeRDF (sAttributeNamespace, aName);
        }
      }

    /**
     * If we have parseType="Literal" set earlier, this element
     * needs some additional attributes to make it stand-alone
     * piece of XML
     */
    if (parseLiteral) {
      if (sPrefix2 == null) {
                                // default namespace coming in
        if (sNamespace != null) {
          newAL.addAttribute ("xmlns:", "gen", null,
                              "CDATA",
                              sNamespace);
        }
        newElement = new Element ("gen:", sElementName,
                                  newAL);
        newElement.prefix ("gen");
      } else {
        String sAttributeNamespace = namespace (sPrefix2);
        if (sAttributeNamespace != null)
          newAL.addAttribute ("xmlns:", sPrefix2, null,
                              "CDATA",
                              sAttributeNamespace);
        newElement = new Element (sPrefix2 + ":", sElementName,
                                  newAL);
      }
    } else {
      newElement = new Element (sNamespace, sElementName,
                                newAL);
      //            likeRDF (sNamespace, sElementName);
    }

    // SM
    if(setScanModeElement)
      scanModeElement = newElement;
    // END SM


    // needed for parseType below
    String sLiteralValue = null;

    if(scanMode != smSKIPPING) {

      checkAttributes (newElement);

      /**
       * Check parseType
       */
      sLiteralValue = newElement.getAttribute(RDFMS_parseType);
      if (sLiteralValue != null && !sLiteralValue.equals (RDFMS_parseType_Resource)) {
                                /**
                                 * This is the management of the element where
                                 * parseType="Literal" appears
                                 *
                                 * You should notice RDF V1.0 conforming implementations
                                 * must treat other values than Literal and Resource as
                                 * Literal. This is why the condition is !equals("Resource")
                                 */
        m_parseTypeStack.push (sLiteralValue);

        if (!m_elementStack.empty()) {
          Element e = (Element)m_elementStack.peek ();
          e.addChild (newElement);
        }

        m_elementStack.push (newElement);
        m_parseElementStack.push (newElement);
        m_sLiteral = "";
        return;
      }

      if (parseLiteral) {
                                /**
                                 * This is the management of any element nested within
                                 * a parseType="Literal" declaration
                                 */
        makeMarkupST (newElement);
        m_elementStack.push (newElement);
        return;
      }

    } // smSKIPPING

    /**
     * Update the containment hierarchy
     * with the stack.
     */
    if (!m_elementStack.empty() &&
                                // SM: IMPORTANT, prevent hooking up of 1st level descriptions to the root element
        (!streamMode || !setScanModeElement)
                                // END SM
        ) {
      Element e = (Element)m_elementStack.peek ();
      e.addChild (newElement);
      //          System.err.println("--- appending child " + newElement.name() + " to " + e.name());
    }

    /**
     * Place the new element into the stack
     */
    m_elementStack.push (newElement);

    if (sLiteralValue != null && sLiteralValue.equals (RDFMS_parseType_Resource)) {
      m_parseTypeStack.push (sLiteralValue);
      m_parseElementStack.push (newElement);
      m_sLiteral = "";

      /**
       * Since parseType="Resource" implies the following
       * production must match Description, let's create
       * an additional Description node here in the document tree.
       */
      Element desc = new Element (RDFMS_Description, new AttributeListImpl());

      if (!m_elementStack.empty()) {
        Element e = (Element)m_elementStack.peek ();
        e.addChild (desc);
      }

      m_elementStack.push (desc);
    }
  }

  /**
   * Helper function to determine if a String is already within a
   * Vector
   *
   * @param     v       Vector to be inspected
   * @param     s       String to be searched
   */
  private boolean hasString (Vector v, String s) {
    for (int x = 0; x < v.size(); x++) {
      String s2 = (String)v.elementAt(x);
      if (s.equals (s2)) {
        return true;
      }
    }
    return false;
  }

  /**
   * For each end of an element scope step back in the
   * element and namespace stack
   *
   * @exception SAXException Passed on since we don't handle it.
   */
  /*
    public void endElement (String name) throws SAXException {
    try {
    protectedendElement(name);
    } catch (Exception any) {
    any.printStackTrace(System.err);
    }
    }
  */

  public void endElement (String name) throws SAXException {

    //      System.err.println("<-- " + name);

    try {

      boolean bParseLiteral = parseLiteral();

      m_root = (Element)m_elementStack.pop ();

      //  if(isRDFroot(m_root))
      // we are done!!!
      //    throw new org.ginf.helpers.EndOfRDFException();

      m_namespaceStack.pop ();


      if(scanMode == smSKIPPING)
	return;


      if (bParseLiteral) {
	Element pe = (Element)m_parseElementStack.peek ();
	if (pe != m_root) {
	  makeMarkupET (m_root.prefix()+name);
	} else {
	  m_root.addChild (new Data (m_sLiteral, true));
	  m_sLiteral = "";
	  m_parseElementStack.pop();
	  m_parseTypeStack.pop ();
	}
      } else if (parseResource()) {
	/**
	 * If we are doing parseType="Resource"
	 * we need to explore whether the next element in
	 * the stack is the closing element in which case
	 * we remove it as well (remember, there's an
	 * extra Description element to be removed)
	 */
	if (!m_elementStack.empty()) {
	  Element pe = (Element)m_parseElementStack.peek ();
	  if (m_elementStack.peek() == pe) {
	    Element e = (Element)m_elementStack.pop ();
	    m_parseElementStack.pop();
	    m_parseTypeStack.pop ();
	  }
	}
      }

      // SM
      if(scanMode == smRDF) {
	scanMode = smSKIPPING;
	if(!streamMode) {
	  resolve();
	  processXML(m_root);
	}
	return;
      }
      if(scanModeElement != m_root)
	// we are deep inside
	return;

      switch(scanMode) {
      case smTOP_DESCRIPTION:
	{
	  preProcessXML(scanModeElement);
	  scanMode = smSKIPPING;
        break;
	}
      case smDESCRIPTION:
	{
	  preProcessXML(scanModeElement);
	  scanMode = smRDF;
	  break;
	}
      }
      // END SM

    } catch (ModelException exc) {
      throw new SAXException(exc);
    }
  }

  void preProcessXML(Element root) throws SAXException, ModelException {
    //      System.err.println("--- preprocessXML: " + root.name());
    if(streamMode)
      processXML(root);
  }
  /**
   * Return the root element pointer. This requires the parsing
   * has been already done.
   */
  public Element root () {
    return m_root;
  }

  public void characters (char ch[], int start, int length)
    throws SAXException {

    /**
     * Place all characters as Data instance to the containment
     * hierarchy with the help of the stack.
     */
    Element e = (Element)m_elementStack.peek ();
    String s = new String (ch, start, length);

    if (parseLiteral()) {
      makeMarkupChar (s);
      return;
    }

    /**
     * Determine whether the previous event was for
     * characters. If so, update the Data node contents.
     * A&amp;B would otherwise result in three
     * separate Data nodes in the parse tree
     */
    boolean bHasData = false;
    Data dataNode = null;
    Enumeration enumeration = e.children();
    while (enumeration.hasMoreElements()) {
      Element e2 = (Element)enumeration.nextElement();
      if (e2 instanceof Data) {
        bHasData = true;
        dataNode = (Data)e2;
        break;
      }
    }

    /**
     * Warning: this is not correct procedure according to XML spec.
     * All whitespace matters!
     */
    String sTrimmed = s.trim();
    //  if(preserveWhiteSpace())
    //    System.err.println("---S is <" + s + ">, hasData=" + bHasData);
    if (sTrimmed.length() > 0 || preserveWhiteSpace()) {
      if(!bHasData)
        e.addChild (new Data (s));
      else
        dataNode.set (dataNode.data() + s);
    } //else
    //System.err.println("--- EMPTY: \"" + s + "\"");
  }

  public void ignorableWhitespace (char ch[], int start, int length) {
  }

  public void processingInstruction (String target, String data) {
  }

  public static Parser createParser (String className) {
    Parser parser = null;

    try {
      // Get the named class.
      Class c = Class.forName(className);
      // Instantiate the parser.
      parser = (Parser)(c.newInstance());
    } catch (ClassNotFoundException e) {
      System.err.println("SAX parser class " + className +
                         "cannot be loaded.");
      System.exit(1);
    } catch (IllegalAccessException e) {
      System.err.println("SAX parser class " + className +
                         " does not have a zero-argument constructor.");
      System.exit(1);
    } catch (InstantiationException e) {
      System.err.println("SAX parser class " + className +
                         " cannot be instantiated.");
      System.exit(1);
    }

    // Check the the parser object
    // actually implements the Parser interface.
    if (!(parser instanceof org.xml.sax.Parser)) {
      System.err.println("Class " + className +
                         " does not implement org.xml.sax.Parser.");
      System.exit(1);
    }

    return parser;
  }

  /**
   * If a URL is relative, make it absolute against the current directory.
   *
   * @exception java.net.MalformedURLException
   private static String makeAbsoluteURL (String url)
   throws java.net.MalformedURLException {
   URL baseURL;

   String currentDirectory = System.getProperty("user.dir");
   String fileSep = System.getProperty("file.separator");
   String file = currentDirectory.replace(fileSep.charAt(0), '/') + '/';

   if (file.charAt(0) != '/') {
   file = "/" + file;
   }
   baseURL = new URL("file", null, file);

   return new URL(baseURL, url).toString();
   }
  */


  /**
   * Escape special characters for display.
   */
  private static String escapeCharacters(char ch[], int start, int length) {
    StringBuffer out = new StringBuffer();

    for (int i = start; i < start+length; i++) {
      if (ch[i] >= 0x20 && ch[i] < 0x7f) {
        out.append(ch[i]);
      } else {
        out.append("&#" + (int)ch[i] + ';');
      }
    }

    return out.toString();
  }

  /**
   * Given an XML document (well-formed HTML, for example),
   * look for a suitable element to start parsing from
   *
   * @exception SAXException Passed on since we don't handle it.
   */
  public void processXML (Element ele) throws SAXException, ModelException {

    //  if (isRDF(ele)) {
    if (isRDFroot (ele)) {
      processRDF(ele);
    } else if (isDescription (ele)) {
      processDescription (ele, false, m_bCreateBags, m_bCreateBags);
    } else {
      processTypedNode(ele);
      /*
        Enumeration e = ele.children();
        while (e.hasMoreElements()) {
        Element child = (Element)e.nextElement();
        processXML (child);
        }
      */
    }

  }

  public void fetchSchema(String sURI) {

    /*
                                 setSource (sURI);
                                 try {
                                 URL url = new URL (sURI);
                                 String sContentType = url.openConnection().getContentType();
                                 // DEBUG -> remove false later on
                                 if (false &&
                                 !sContentType.startsWith ("text/xml") &&
                                 !sContentType.startsWith ("text/html")) {
                                 addError ("The RDF schema at "+sURI+" is of wrong content type '"+sContentType+"'\n(should have been 'text/xml' or 'text/html')");
                                 } else {

                                 InputStream is = url.openStream ();
                                 InputSource source = new InputSource (is);

                                 // Create a new parser.
                                 Parser p = null;

                                 if (m_sXMLParser == null)
                                 p = ParserFactory.makeParser();
                                 else
                                 p = ParserFactory.makeParser(m_sXMLParser);

                                 // Register the handlers
                                 p.setEntityResolver(this);
                                 p.setDTDHandler (this);
                                 p.setDocumentHandler(this);
                                 p.setErrorHandler (this);

                                 p.parse(source);
                                 resolve ();
                                 processXML (root());
                                 }
                                 } catch (Exception ex) {
                                 addError ("Could not load RDF schema from "+sURI+". Problem: "+ex);
                                 }
    */
  }

  /**
   * Start processing an RDF/XML document instance from the
   * root element <i>rdf</i>.
   *
   * @exception SAXException Passed on since we don't handle it.
   */
  public void processRDF (Element rdf) throws SAXException, ModelException {
    Enumeration e = rdf.children();
    //  if (!e.hasMoreElements()) {
    //      addError ("Empty RDF element");
    //      return;
    //  }
    while (e.hasMoreElements()) {
      Element ele = (Element)e.nextElement();

      if (isDescription (ele)) {
        processDescription (ele, false, m_bCreateBags, m_bCreateBags);
      } else if (isContainer (ele)) {
        processContainer (ele);
      } else if (isTypedPredicate (ele)) {
        processTypedNode (ele);
      }
    }
  }

  /**
   * Manage the typedNode production in the RDF grammar.
   *
   * @exception SAXException Passed on since we don't handle it.
   */
  public String processTypedNode (Element typedNode) throws SAXException, ModelException {
    String sID              = typedNode.ID();
    String sBagID           = typedNode.bagID();
    String sAbout           = typedNode.about();

    Element target = typedNode.target();

    //  System.out.println("PROCESSING TYPED NODE: " + sAbout + ", " + sID + ", TARGET: " +
    //                     (target != null ? target.about() + ", " + target.ID() : ""));

    String sAboutEach       = typedNode.aboutEach();
    String sAboutEachPrefix = typedNode.aboutEachPrefix();

    if (typedNode.resource() != null) {
      addError ("'resource' attribute not allowed for a typedNode "+typedNode.name()+" - see <a href=\"http://www.w3.org/TR/REC-rdf-syntax/#typedNode\">[6.13]</a>");
    }

    /**
     * We are going to manage this typedNode using the processDescription
     * routine later on. Before that, place all properties encoded as
     * attributes to separate child nodes.
     */
    Enumeration e = typedNode.attributes ();
    while (e.hasMoreElements()) {
      QName sAttribute = (QName)e.nextElement();
      String sValue = typedNode.getAttribute (sAttribute);
      sValue = sValue.trim ();

      if (!RDFMS.equals(sAttribute.getNamespace()) &&
          !XMLSCHEMA.equals(sAttribute.getNamespace())) {
        if (sValue.length() > 0) {
          Element newPredicate = new Element (/*sAttribute.getName(),*/ sAttribute.getNamespace(), sAttribute.getLocalName(),
                                              new AttributeListImpl ());
          newPredicate.addAttribute (RDFMS_ID, (sAbout != null ? sAbout : sID));
          newPredicate.addAttribute (RDFMS_bagID, sBagID);
          Data newData = new Data (sValue);
          newPredicate.addChild (newData);
          typedNode.addChild (newPredicate);
          typedNode.removeAttribute (sAttribute);
        }
      }
    }

    String sObject = null;

    if(target != null)
      sObject = (target.bagID() != null ? target.bagID() : target.ID());
    else if (sAbout != null)
      sObject = sAbout;
    else if (sID != null)
      sObject = sID;
    else
      sObject = newReificationID();

    typedNode.ID (sObject, source());

    //  System.out.println("PROCESSING TYPED NODE AFTER: " + typedNode.about() + ", " + typedNode.ID());

    //  System.out.println("ATTACHING " + sObject + " to " + source() + "=" + typedNode.ID());

    // special case: should the typedNode have aboutEach attribute,
    // the type predicate should distribute to pointed
    // collection also -> create a child node to the typedNode
    Enumeration eTargets = typedNode.targets ();
    if (sAboutEach != null &&
        eTargets.hasMoreElements()) {
      Element newPredicate = new Element (RDFMS, "type",
                                          new AttributeListImpl());
      Data newData = new Data (typedNode.name());
      newPredicate.addChild (newData);
      typedNode.addChild (newPredicate);
    } else {
      addTriple (createResource(RDFMS, "type"),
                 createResource(typedNode.ID()),
                 createResource(typedNode.namespace(), typedNode.localName()));
    }


    // 1.13
    //  String sDesc = processDescription (typedNode, false, false, true);
    String sDesc = processDescription (typedNode, false, false, false);

    //  return typedNode.ID();
    return sObject;
  }

  /**
   * processDescription manages Description elements
   *
   * @param description The Description element itself
   * @param inPredicate Is this is a nested description
   * @param reify               Do we need to reify
   * @param createBag           Do we create a bag container
   *
   * @return            An ID for the description
   *
   * @exception SAXException Passed on since we don't handle it.
   */
  public String processDescription (Element description,
                                    boolean inPredicate,
                                    boolean reify,
                                    boolean createBag) throws SAXException, ModelException {
    /**
     * Return immediately if the description has already been managed
     */
    if (description.done())
      return description.ID();

    int iChildCount = 1;
    boolean     bOnce = true;

    /**
     * Determine first all relevant values
     */
    String sID              = description.ID();
    String sBagid           = description.bagID();
    String sAbout           = description.about();

    String sAboutEach       = description.aboutEach();
    String sAboutEachPrefix = description.aboutEachPrefix();

    Element target = description.target();

    //  System.out.println("PROCESSING DESC: " + sAbout + ", " + sID + ", TARGET: " +
    //                     (target != null ? target.about() + ", " + target.ID() : ""));

    boolean hasTarget = target != null; // ME faster than description.targets().hasMoreElements();

    boolean targetIsContainer = false;
    String sTargetAbout = null;
    String sTargetBagid = null;
    String sTargetID = null;

    /**
     * Determine what the target of the Description reference is
     */
    if (hasTarget) {
      sTargetAbout = target.about();
      sTargetID    = target.ID();
      sTargetBagid = target.bagID();

      /**
       * Target is collection if
       * 1. it is identified with bagID attribute
       * 2. it is identified with ID attribute and is a collection
       */
      if (sTargetBagid != null && sAbout != null) {
        targetIsContainer = (sAbout.substring(1).equals (sTargetBagid));
      } else {
        if (sTargetID != null &&
            sAbout != null &&
            sAbout.substring(1).equals (sTargetID) &&
            isContainer (target)) {
          targetIsContainer = true;
        }
      }
    }

    /**
     * Check if there are properties encoded using the abbreviated
     * syntax
     */
    expandAttributes (description, description, false);

    /**
     * Manage the aboutEach attribute here
     */
    if (sAboutEach != null && hasTarget) {
      if (isContainer(target)) {
        Enumeration e = target.children ();
        while (e.hasMoreElements()) {
          Element ele = (Element)e.nextElement ();
          if (isListItem (ele)) {
            String sResource = ele.resource();
            /**
             * Manage <li resource="..." /> case
             */
            if (sResource != null) {
              Element newDescription = null;
              // 1.13
              //                            if (sResource != null) {
              //                                newDescription = new Element (RDFMS + "Description",
              //                                                              new AttributeListImpl ());
              //                                newDescription.addAttribute (RDFMS + "about", sResource);
              //
              //                            }
              newDescription = new Element (RDFMS_Description,
                                            new AttributeListImpl ());
              newDescription.about (sResource);

              Enumeration e2 = description.children();
              while (e2.hasMoreElements()) {
                Element ele2 = (Element)e2.nextElement ();
                if (newDescription != null) {
                  newDescription.addChild (ele2);
                }
              }
              if (newDescription != null)
                processDescription (newDescription, false, false, false);
            } else {
              /**
               * Otherwise we have a structured value inside <li>
               */

              // loop through the children of <li>
              // (can be only one)
              Enumeration e2 = ele.children ();
              while (e2.hasMoreElements()) {
                Element ele2 = (Element)e2.nextElement ();

                // loop through the items in the
                // description with aboutEach
                // and add them to the target
                Element newNode = new Element (RDFMS_Description,
                                               new AttributeListImpl());
                Enumeration e3 = description.children();
                while (e3.hasMoreElements()) {
                  Element ele3 = (Element)e3.nextElement ();
                  newNode.addChild (ele3);
                }
                newNode.addTarget (ele2);

                processDescription (newNode,
                                    true, false, false);
              }
            }
          } else if (isTypedPredicate (ele)) {
            Element newNode = new Element (RDFMS_Description,
                                           new AttributeListImpl());
            Enumeration e2 = description.children();
            while (e2.hasMoreElements()) {
              Element ele2 = (Element)e2.nextElement ();
              newNode.addChild (ele2);
            }
            newNode.addTarget (ele);

            processDescription (newNode,
                                true, false, false);
          }
        }
      } else if (isDescription(target)) {
        processDescription (target,
                            false, reify, createBag);

        Enumeration e = target.children ();
        while (e.hasMoreElements()) {
          Element ele = (Element)e.nextElement ();
          Element newNode = new Element (RDFMS_Description,
                                         new AttributeListImpl());
          Enumeration e2 = description.children();
          while (e2.hasMoreElements()) {
            Element ele2 = (Element)e2.nextElement ();
            newNode.addChild (ele2);
          }
          newNode.addTarget (ele);

          processDescription (newNode,
                              true, false, false);
        }
      }
      return null;
    }

    /**
     * Manage the aboutEachPrefix attribute
     */
    if (sAboutEachPrefix != null) {
      if (hasTarget) {
        Enumeration e = description.targets();
        while (e.hasMoreElements()) {
          target = (Element)e.nextElement ();
          sTargetAbout = target.about();
          Element newDescription = new Element (RDFMS_Description,
                                                new AttributeListImpl ());
          newDescription.about (sTargetAbout);
          Enumeration e2 = description.children();
          while (e2.hasMoreElements()) {
            Element ele2 = (Element)e2.nextElement ();
            newDescription.addChild (ele2);
          }
          processDescription (newDescription,
                              false, false, false);
        }
      }
      return null;
    }

    /**
     * Enumerate through the children
     */
    Enumeration e = description.children();

    //->    // ADDED by PA
    int paCounter = 1;
    //->    // end ADDED by PAS

    while (e.hasMoreElements()) {
      Element n = (Element)e.nextElement();

      if (isDescription (n)) {
        addError ("Cannot nest a Description inside another Description");
                                //          } else if (isListItem (n)) {
                                //              addError ("Cannot nest a Listitem inside a Description");
                                //->        // ADDED by PA
      } else if (isListItem(n)) {
        processListItem(sID,n,paCounter);
        paCounter++;
                                //->        // end ADDED by PA
      } else if (isContainer (n)) {
        addError ("Cannot nest a container (Bag/Alt/Seq) inside a Description");
      } else if (isTypedPredicate(n)) {

        String sChildID = null;

        if (hasTarget && targetIsContainer) {
          sChildID = processPredicate (n, description,
                                       (target.bagID() != null ? target.bagID() : target.ID()),
                                       false);
          description.ID (sChildID, source());
          createBag = false;
        } else if (hasTarget) {
          sChildID = processPredicate (n, description,
                                       (target.bagID() != null ? target.bagID() : target.ID()),
                                       reify);
          description.ID (sChildID, source());
        } else if (!hasTarget && !inPredicate) {
          if (description.ID() == null)
            // 1.13
            description.ID (newReificationID());
          if (sAbout == null)
            if (sID != null)
              sAbout = sID;
            else
              sAbout = description.ID();

          //                                    System.out.println("PREDICATE: " + n + " of " + description.name());

          sChildID = processPredicate (n, description,
                                       sAbout,
                                       ( sBagid != null ? true : reify));
          //description.ID (sChildID, source());
        } else if (!hasTarget && inPredicate) {
          if (sAbout == null) {
            if (sID != null) {
              description.ID (sID, source());
              sAbout = sID;
            } else {
              if (description.ID() == null)
                // 1.13
                description.ID (newReificationID());
              sAbout = description.ID();
            }
          } else {
            description.ID (sAbout);
          }
          sChildID = processPredicate (n, description,
                                       sAbout,
                                       false);
        }

                                /**
                                 * Each Description block creates also a Bag node which
                                 * has links to all properties within the block IF
                                 * the m_bCreateBags variable is true
                                 */
        if (sBagid != null || (m_bCreateBags && createBag)) {
          String sNamespace = RDFMS;
          // do only once and only if there is a child
          if (bOnce && sChildID != null) {
            bOnce = false;
            if (description.bagID() == null)
              // 1.13
              description.bagID (newReificationID());
            if (description.ID() == null)
              description.ID (description.bagID(), source());

            addTriple (createResource(sNamespace, "type"),
                       createResource(description.bagID()),
                       createResource(sNamespace, "Bag"));
          }
          if (sChildID != null) {
            addTriple (createResource(sNamespace, "_" + iChildCount),
                       createResource(description.bagID()),
                       createResource(sChildID));
            iChildCount++;
          }
        }
      }
    }

    description.done (true);

    return description.ID();
  }

  /**
   * processPredicate handles all elements not defined as special
   * RDF elements. <tt>predicate</tt> has either <tt>resource()</tt> or a single child
   *
   * @param predicate   The predicate element itself
   * @param description Context for the predicate
   * @param sTarget     The target resource
   * @param reify       Should this predicate be reifyd
   *
   * @return the new ID which can be used to identify the predicate
   *
   * @exception SAXException Passed on since we don't handle it.
   */
  private String processPredicate (Element predicate,
                                   Element description,
                                   String sTarget,
                                   boolean reify) throws SAXException, ModelException {
    String sStatementID = predicate.ID();
    String sBagID       = predicate.bagID();
    String sResource    = predicate.resource();

    //  extractNamespace(sResource);

    /**
     * If a predicate has other attributes than rdf:ID, rdf:bagID,
     * or xmlns... -> generate new triples according to the spec.
     * (See end of Section 6)
     */


    // this new element may not be needed
    Element d = new Element (RDFMS_Description,
                             new AttributeListImpl());
    if (expandAttributes (d, predicate, true)) {


      // error checking
      if (predicate.children().hasMoreElements()) {
        addError (predicate.name()+" must be an empty element since it uses propAttr grammar production - see <a href=\"http://www.w3.org/TR/REC-rdf-syntax/#propertyElt\">[6.12]</a>");
        return null;
      }

      // determine the 'about' part for the new statements
      if (sStatementID != null) {
        d.addAttribute (RDFMS_about, sStatementID);
                                // make rdf:ID the value of the predicate
        predicate.addChild (new Data (sStatementID));
      } else if (sResource != null) {
        d.addAttribute (RDFMS_about, sResource);
      } else {
        sStatementID = newReificationID();
        d.addAttribute (RDFMS_about, sStatementID);
      }

      if (sBagID != null) {
        d.addAttribute (RDFMS_bagID, sBagID);
        d.bagID (sBagID);
      }

      processDescription (d, false, false, m_bCreateBags);
    }


    //    System.err.println("PRED ST: " + predicate);

						//    }

    //          for(Enumeration en = predicate.attributes(); en.hasMoreElements(); ) {
    //                  String attName = (String)en.nextElement();
    //                  if(!attName.equals(RDFMS + "resource"))
    //                          addError("[SM] Property element " + predicate.name() + " has invalid attribute " + attName + ". Only rdf:resource is allowed.");
    //          }

    //          System.out.println("PROCESS PREDICATE 2: " + predicate.name());

    /**
     * Tricky part: if the resource attribute is present for a predicate
     * AND there are no children, the value of the predicate is either
     * 1. the URI in the resource attribute OR
     * 2. the node ID of the resolved #resource attribute
     */
    if (sResource != null && !predicate.children().hasMoreElements()) {
      if (predicate.target() == null) {
        if (reify) {
          sStatementID = reify (createResource(predicate.namespace(), predicate.localName()),
                                createResource(sTarget),
                                createResource(sResource),
                                predicate.ID());
          predicate.ID (sStatementID, source());
        } else {
          addOrder ( predicate, 0, null,
		     createResource(predicate.namespace(), predicate.localName()),
                     createResource(sTarget),
                     createResource(sResource));
        }
      } else {
        if (reify) {
          sStatementID = reify (createResource(predicate.namespace(), predicate.localName()),
                                createResource(sTarget),
                                createResource(predicate.target().ID()),
                                predicate.ID());
          predicate.ID (sStatementID, source());
        } else {
          addOrder (predicate, 0, null,
		    createResource(predicate.namespace(), predicate.localName()),
		    createResource(sTarget),
		    createResource(predicate.target().ID()));
        }
      }
      return predicate.ID();
    }

    /**
     * Does this predicate make a reference somewhere using the
     * <i>sResource</i> attribute
     */
    if (sResource != null && predicate.target() != null) {
      sStatementID = processDescription (predicate.target(),
                                         true, false, false);
      if (reify) {
        sStatementID = reify (createResource(predicate.namespace(), predicate.localName()),
                              createResource(sTarget),
                              createResource(sStatementID),
                              predicate.ID());
        predicate.ID (sStatementID, source());
      } else {
        addOrder (predicate, 0, null,
		  createResource(predicate.namespace(), predicate.localName()),
		  createResource(sTarget),
		  createResource(sStatementID));
      }

      return sStatementID;
    }

    /**
     * Before looping through the children, let's check
     * if there are any. If not, the value of the predicate is
     * an anonymous node
     */
    // SM
    String sObject = newReificationID(); //d.ID() != null ? d.ID() : ;

    Enumeration e2 = predicate.children();
    if (!(e2.hasMoreElements())) {
      if (reify) {
        sStatementID = reify (createResource(predicate.namespace(), predicate.localName()),
                              createResource(sTarget),
                              createResource(sObject), // newReificationID()
                              predicate.ID());
      } else {
	//        System.out.println("OBJECT: " + sResource + " or " + sStatementID + " or " + description.ID() + " or " + d.ID());
        addOrder (predicate, 0, null,
		  createResource(predicate.namespace(), predicate.localName()),
		  createResource(sTarget),
		  createResource(sObject)); // newReificationID()
      }
    }
    Statement previousStatement = null;
    int order = 0;

    while (e2.hasMoreElements()) {
      Element n2 = (Element)e2.nextElement();

      // FIXME: we are not requiring this for the sake of experiment
      //          addError ("Only one node allowed inside a predicate (Extra node is "+n2.name()+") - see <a href=\"http://www.w3.org/TR/REC-rdf-syntax/#propertyElt\">[6.12]</a>");

      if (isDescription (n2)) {
        Element d2 = n2;

        sStatementID = processDescription (d2, true, false, false);

        d2.ID (sStatementID, source());

        if (reify) {
          sStatementID = reify (createResource(predicate.namespace(), predicate.localName()),
                                createResource(sTarget),
                                createResource(sStatementID),
                                predicate.ID());
        } else {
	  previousStatement = addOrder (predicate, ++order, previousStatement,
					createResource(predicate.namespace(), predicate.localName()),
					createResource(sTarget),
					createResource(sStatementID));
        }

      } else if (n2 instanceof Data) {
                                /**
                                 * We've got real data
                                 */
        String  sValue = ((Data)n2).data();
        boolean isXML = ((Data)n2).isXML();

                                /**
                                 * Only if the content is not empty PCDATA (whitespace that is),
                                 * print the triple
                                 */
                                //              sValue = sValue.trim();
                                //              if (sValue.length() > 0) {
        if (reify) {
          sStatementID = reify (createResource(predicate.namespace(), predicate.localName()),
                                createResource(sTarget),
                                createLiteral(sValue, isXML),
                                predicate.ID());
          predicate.ID (sStatementID, source());
        } else {
          previousStatement = addOrder (predicate, ++order, previousStatement,
					createResource(predicate.namespace(), predicate.localName()),
					createResource(sTarget),
					createLiteral(sValue, isXML));
        }
        //              }
      } else if (isContainer (n2)) {

        String sCollectionID = processContainer (n2);
        sStatementID = sCollectionID;

                                /**
                                 * Attach the collection to the current predicate
                                 */
        if (description.target() != null) {
          if (reify) {
            sStatementID = reify (createResource(predicate.namespace(), predicate.localName()),
                                  createResource(description.target().about()),
                                  createResource(sCollectionID),
                                  predicate.ID());
            predicate.ID (sStatementID, source());
          } else {
            addTriple (createResource(predicate.namespace(), predicate.localName()),
                       createResource(description.target().about()),
                       createResource(sCollectionID));
          }
        } else {
          if (reify) {
            sStatementID = reify (createResource(predicate.namespace(), predicate.localName()),
                                  createResource(sTarget),
                                  createResource(sCollectionID),
                                  predicate.ID());
            predicate.ID (sStatementID, source());
          } else {
            addTriple (createResource(predicate.namespace(), predicate.localName()),
                       createResource(sTarget),
                       createResource(sCollectionID));
          }
        }

      } else if (isTypedPredicate (n2)) {
	//	System.out.println("PROCESSING NODE: " + n2);
        sStatementID = processTypedNode (n2);
	previousStatement = addOrder(predicate, ++order, previousStatement,
				     createResource(predicate.namespace(), predicate.localName()),
				     createResource(sTarget),
				     createResource(sStatementID));
      }
    }

    return sStatementID;
  }

  // SM: check for rdf:order and rdf:backwardOrder
  // orderInt == 1 is ignored, it is added on the second call with previousStatement (orderInt == 2)
  // orderInt < 1 is completely ignored
  private Statement addOrder(Element predicate, int orderInt, Statement previousStatement, Resource p, Resource s, RDFNode o) throws SAXException, ModelException {

    if(!ENABLE_EXPERIMENTAL)
      return null;

    if(orderInt == 2) {
      // add order of the previous statement if any
      addTriple(nodeFactory.createStatement(previousStatement,
					    createResource(RDFX.order.getURI()),
					    createLiteral("1")));
    }

    Statement st = nodeFactory.createStatement(s, p, o);
    addTriple(st);

    String order = predicate.getAttribute(RDFX.order.getURI());
    String backwardOrder = predicate.getAttribute(RDFX.backwardOrder.getURI());

    if(previousStatement != null && order != null) {
      // two statements cannot have the same forward order
      addError("SM: two statements " + previousStatement + " and " + st + " cannot have the same forward order");
    }

    if(order == null && orderInt > 1)
      order = String.valueOf(orderInt);
    if(order != null) {
      addTriple(createResource(RDFX.order.getURI()), st, createLiteral(order));
    }
    if(backwardOrder != null) {
      addTriple(createResource(RDFX.backwardOrder.getURI()), st, createLiteral(backwardOrder));
    }
    return st;
  }


  private String processContainer (Element n) throws SAXException, ModelException {

    //      checkAttributes(n);

    String sID = n.ID();
    if (sID == null)
      sID = n.about();
    if(sID == null)
      sID = newReificationID();


    /**
     * Do the instantiation only once
     */
    if (!n.done()) {
      String sNamespace = RDFMS;
      if (isSequence (n)) {
        addTriple (createResource(RDFMS, "type"),
                   createResource(sID),
                   createResource(RDFMS_Seq));
      } else if (isAlternative (n)) {
        addTriple (createResource(RDFMS, "type"),
                   createResource(sID),
                   createResource(RDFMS_Alt));
      } else if (isBag (n)) {
        addTriple (createResource(RDFMS, "type"),
                   createResource(sID),
                   createResource(RDFMS_Bag));
      }
      n.done (true);
    }

    expandAttributes (n, n, false);

    Enumeration e = ((Element)n).children();

    if (!e.hasMoreElements() &&
        isAlternative (n)) {
      addError ("An RDF:Alt container must have at least one nested listitem");
    }

    int iCounter = 1;
    while (e.hasMoreElements()) {
      Element n2 = (Element)e.nextElement();
      if (isListItem (n2)) {
        processListItem (sID, n2, iCounter);
        iCounter++;
      } else {
        addError ("Cannot nest "+n2.name()+" inside a container (Bag/Alt/Seq)");
      }
    }

    return sID;
  }

  private void processListItem (String sID, Element listitem, int iCounter)
    throws SAXException, ModelException {
    /**
     * Two different cases for
     * 1. LI element without content (resource available)
     * 2. LI element with content (resource unavailable)
     */
    String sResource = listitem.resource();

    // SM: bug fix 2000-10-30
    Resource ord = listitem.name().endsWith("li") ?
      createResource(RDFMS, "_"+iCounter) :
      createResource(listitem.namespace(), listitem.localName());

    //    System.out.println("LIST ITEM: " + listitem.name() + " vs " + listitem.ID() + " vs " + ord);

    if (sResource != null) {
      addTriple (ord,
                 createResource(sID),
                 createResource(sResource));
      // validity checking
      if (listitem.children().hasMoreElements()) {
        addError ("Listitem with 'resource' attribute cannot have child nodes - see <a href=\"http://www.w3.org/TR/REC-rdf-syntax/#referencedItem\">[6.29]</a>");
      }
      listitem.ID (sResource, source());
    } else {
      Enumeration e = listitem.children();
      while (e.hasMoreElements()) {
        Element n = (Element)e.nextElement();
        if (n instanceof Data) {
          addTriple (ord,
                     createResource(sID),
                     createLiteral(((Data)n).data()));
        } else if (isDescription (n)) {
          String sNodeID = processDescription (n, false, true, false);
          addTriple (ord,
                     createResource(sID),
                     createResource(sNodeID));
          listitem.ID (sNodeID, source());
        } else if (isListItem (n)) {
          addError ("Cannot nest a listitem inside another listitem");
        } else if (isContainer (n)) {
          processContainer (n);
          addTriple (ord,
                     createResource(sID),
                     createResource(n.ID()));
        } else if (isTypedPredicate (n)) {
          String sNodeID = processTypedNode (n); //
          addTriple (ord,
                     createResource(sID),
                     createResource(sNodeID));
        }
      }
    }
  }

  String getCompatibilityAttribute(QName bad, QName good, Element e) {

    String sResource = e.getAttribute (bad);
    if(sResource != null) {
      // compatibility fix for buggy specs
      e.addAttribute(good, sResource);
      e.removeAttribute(bad);
    } else
      sResource = e.getAttribute (good);

    return sResource;
  }

  /**
   * checkAttributes goes through the attributes of element <i>e</i>
   * to see
   * 1. if there are symbolic references to other nodes in the data model.
   *    in which case they must be stored for later resolving with
   *    <i>resolveLater</i> method.
   * 2. if there is an identity attribute, it is registered using
   *    <i>registerResource</i> or <i>registerID</i> method.
   *
   * @see       resolveLater
   * @see       registerResource
   * @see       registerID
   */
  private void checkAttributes (Element e) throws SAXException {

    getCompatibilityAttribute (UNQUALIFIED_parseType, RDFMS_parseType, e);

    String sResource = getCompatibilityAttribute (UNQUALIFIED_resource, RDFMS_resource, e);

    if (sResource != null) {
      if (sResource.startsWith("#") && !streamMode) { // SM END SM
        resolveLater (e);
        e.resource (sResource);
      } else {
	//	System.out.println("RESOLVING: " + sResource + ", " + source());
        e.resource (sResource, source());
                                //              extractNamespace( e.resource() );
      }
    }

    String sAboutEach = e.getAttribute (RDFMS_aboutEach);

    if (sAboutEach != null &&
        sAboutEach.startsWith("#")) {
      resolveLater (e);
      e.aboutEach (sAboutEach);
    }

    String sAboutEachPrefix = e.getAttribute (RDFMS_aboutEachPrefix);
    if (sAboutEachPrefix != null) {
      resolveLater (e);
      e.aboutEachPrefix (sAboutEachPrefix);
    }

    String sAbout = getCompatibilityAttribute (UNQUALIFIED_about, RDFMS_about, e);

    if (sAbout != null) {
      if (sAbout.startsWith("#") && !streamMode) { // SM END SM
        resolveLater (e);
        e.about( sAbout );
                                //              System.out.println("RESOLVE LATER: " + sAbout);
      } else {
        registerResource (e);
        e.about (sAbout, source());
                                //              extractNamespace( e.about() );
      }
    }

    String sBagID = e.getAttribute (RDFMS_bagID);
    if (sBagID != null) {
      e.bagID (sBagID, source());
      sBagID = e.bagID();
      registerID (sBagID, e);
    }

    String sID = getCompatibilityAttribute (UNQUALIFIED_ID, RDFMS_ID, e);

    if (sID != null) {
      e.ID (sID, source());
      sID = e.ID();
      registerID (sID, e);
    }

    // SM
    if(streamMode && (sAboutEach != null || sAboutEachPrefix != null))
      addError("aboutEach and aboutEachPrefix are not supported in the stream mode");
    // END SM

    if (sID != null && sAbout != null) {
      addError ("A description block cannot use both 'ID' and 'about' attributes - see <a href=\"http://www.w3.org/TR/REC-rdf-syntax/#idAboutAttr\">[6.5]</a");
    }
  }

  /**
   * Take an element <i>ele</i> with its parent element <i>parent</i>
   * and evaluate all its attributes to see if they are non-RDF specific
   * and non-XML specific in which case they must become children of
   * the <i>ele</i> node.
   *
   * @exception SAXException Passed on since we don't handle it.
   */
  private boolean expandAttributes (Element parent, Element ele, boolean predicateNode)
    throws SAXException {
    boolean foundAbbreviation = false;

    // SM
    String id = null;
    if(predicateNode) {
      id = ele.getAttribute(RDFMS_resource);
      if(id == null)
	id = ele.getAttribute(UNQUALIFIED_resource);
    }
    if(id != null)
      parent.ID(id);
    // end SM

    Enumeration e = ele.attributes ();
    while (e.hasMoreElements()) {
      QName sAttribute = (QName)e.nextElement();
      String sValue = ele.getAttribute (sAttribute); /*.trim();*/

      //      System.out.println("---xml: " + sAttribute + "=" + sValue);

      if (XMLSCHEMA.equals(sAttribute.getNamespace())) {
        /* expands after parsing, that's why it is useless here... :(
           // because of concatenation without : inbetween
           if("xmlspace".equals(sAttribute))
           ele.setPreserveWhiteSpace( "preserve".equals(sValue) );
        */
        continue;
      }

      // exception: expand rdf:value
      // SO UGLY: Janne stores his own XML attributes like rdf:ID in the elements
      if (RDFMS.equals(sAttribute.getNamespace()) &&
          !sAttribute.getLocalName().startsWith("_") &&
          !sAttribute.getLocalName().endsWith ("value") &&
          !sAttribute.getLocalName().endsWith ("type") &&
          !sAttribute.getLocalName().endsWith ("object"))
        continue;

      // SM: ignore order
      if(ENABLE_EXPERIMENTAL && RDFX._Namespace.equals(sAttribute.getNamespace()))
	continue;

      /*
      if(predicateNode &&  // expanding predicate element
         !( sAttribute.equals(RDFMS_resource) ||
	    sAttribute.equals(UNQUALIFIED_resource)))
        addError("[SM] Property element " + ele.name() + " has invalid attribute '" + sAttribute + "'. Only rdf:resource is allowed.");
      */

      //            if (sValue.length() > 0) {
      foundAbbreviation = true;
      Element newElement = new Element (sAttribute.getNamespace(), sAttribute.getLocalName(),
                                        new AttributeListImpl());
      Data newData = new Data (sValue);
      newElement.addChild (newData);
      parent.addChild (newElement);
      //            }
    }
    return foundAbbreviation;
  }

  /**
   * reify creates one new node and four new triples
   * and returns the ID of the new node
   */
  private String reify (Resource predicate,
                        Resource subject,
                        RDFNode object,
                        String sNodeID) throws SAXException, ModelException {
    String sNamespace = RDFMS;
    if (sNodeID == null)
      sNodeID = newReificationID();

    /**
     * The original statement must remain in the data model
     */
    addTriple (predicate, subject, object);

    /**
     * Do not reify reifyd properties
     */
    if (predicate.equals (sNamespace+"subject") ||
        predicate.equals (sNamespace+"predicate") ||
        predicate.equals (sNamespace+"object") ||
        predicate.equals (sNamespace+"type")) {
      return null;
    }

    /**
     * Reify by creating 4 new triples
     */
    addTriple (createResource(sNamespace, "predicate"),
               createResource(sNodeID),
               predicate);

    addTriple (createResource(sNamespace, "subject"),
               createResource(sNodeID),
               createLiteral(( subject.toString().length() == 0 ? source() : subject.toString())));

    addTriple (createResource(sNamespace, "object"),
               createResource(sNodeID),
               object);

    addTriple (createResource(sNamespace, "type"),
               createResource(sNodeID),
               createResource(sNamespace, "Statement"));

    return sNodeID;
  }

  protected void addTriple(Statement s) throws SAXException, ModelException {
    m_consumer.addStatement(s);
  }

  /**
   * Create a new triple and add it to the <i>m_triples</i> Vector
   */
  public void addTriple (Resource predicate,
                         Resource subject,
                         RDFNode object) throws SAXException, ModelException {

    //          System.out.println("ADDING " + subject + " " + predicate + " " + object);
    //          new Exception("Tracing").printStackTrace(System.out);
    /**
     * If there is no subject (about=""), then use the URI/filename where
     * the RDF description came from
     */
    if (predicate == null) {
      addWarning ("Predicate null when subject="+subject+" and object="+object);
      return;
    }

    if (subject == null) {
      addWarning ("Subject null when predicate="+predicate+" and object="+object);
      return;
    }

    if (object == null) {
      addWarning ("Object null when predicate="+predicate+" and subject="+subject);
      return;
    }

    if (subject.toString() == null ||
        subject.toString().length() == 0) {
      if(source() == null)
	addError("XML document refers to the source URL. The source URL has not been set.");
      subject = createResource(source());
      //      subject = createResource(m_sSourceModified ? source().substring(0, source().length() - 1) : source());
      //      addError("SELF SUBJECT: " + subject + ", modified: " + m_sSourceModified);
    }

    //  Triple t = new Triple (predicate, subject, object);
    //  m_triples.addElement (t);

    /**
     * Notify RDFConsumer objects if any
     */
    /*
      for (int x = 0; x < m_consumers.size(); x++) {
      RDFConsumer consumer = (RDFConsumer)m_consumers.elementAt(x);
      consumer.assert (this, predicate, subject, object);
      }
    */
    addTriple(nodeFactory.createStatement(subject, predicate, object));
  }

  /**
   * Print all triples to the <i>ps</i> PrintStream
   public void printTriples (PrintStream ps) {
   for (int x = 0; x < m_triples.size(); x++) {
   Triple t = (Triple)m_triples.elementAt (x);
   ps.println ("triple(\""+t.predicate()+"\",\""+t.subject()+"\",\""+t.object()+"\").");
   }
   }
  */

  /**
   * Return all created triples in an Enumeration instance
   public Enumeration triples () {
   return m_triples.elements ();
   }
  */

  /**
   * Is the element a Description
   */
  public boolean isDescription (Element e) {
    return RDFMS_Description.equals(e.name());
  }

  /**
   * Is the element a ListItem
   */
  public boolean isListItem (Element e) {
    return isRDF(e) &&
      ( e.name().endsWith ("li") ||
        e.name().indexOf ("_") > -1);
  }

  /**
   * Is the element a Container
   *
   * @see       isSequence
   * @see       isAlternative
   * @see       isBag
   */
  public boolean isContainer (Element e) {
    return (isSequence (e) ||
            isAlternative (e) ||
            isBag (e));
  }

  /**
   * Is the element a Sequence
   */
  public boolean isSequence (Element e) {
    return RDFMS_Seq.equals(e.name());
  }

  /**
   * Is the element an Alternative
   */
  public boolean isAlternative (Element e) {
    return RDFMS_Alt.equals(e.name());
  }

  /**
   * Is the element a Bag
   */
  public boolean isBag (Element e) {
    return RDFMS_Bag.equals(e.name());
  }

  /**
   * This method matches all properties but those from RDF namespace
   */
  public boolean isTypedPredicate (Element e) {

    // SM: don't understand this

    String n = e.name();
    if(RDFMS_resource.equals(n))
      return false; // needed?

    /*
    if (isRDF(e)) {
      // list all RDF predicates known by the RDF specification
      if (e.name().endsWith ("predicate") ||
          e.name().endsWith ("subject") ||
          e.name().endsWith ("object") ||
          e.name().endsWith ("type") ||
          e.name().endsWith ("value") ||
          e.name().indexOf("_") >= 0 ||
          e.name().endsWith ("Property") ||
          e.name().endsWith ("Statement")) {
        return true;
      }
      return false;
    }
    */
    if (n.length() > 0)
      return true;
    else
      return false;
  }

  public boolean isRDFroot (Element e) {
    return RDFMS_RDF.equals(e.name());
  }

  /**
   * Check if the element <i>e</i> is from the namespace
   * of the RDF schema by comparing only the beginning of
   * the expanded element name with the canonical RDFMS
   * URI
   */
  public boolean isRDF (Element e) {
    if (e != null && e.name() != null)
      return e.name().startsWith (RDFMS);
    else
      return false;
  }

  public void setStreamMode(boolean b) {
    streamMode = b;
  }

  public void setRobustMode(boolean b) {
    robustMode = b;
  }

  public void ignoreExternalEntities(boolean b) {
    ignoreExternalEntities = b;
  }

  public void setGenerateUniqueResources(boolean b) {
    generateUniqueResources = b;
  }

  /**
   * Methods for node reference management
   */
  private Vector        m_vResources = new Vector ();
  private Vector        m_vResolveQueue = new Vector ();
  // SM: if streamMode is set, m_vResources, m_vResolveQueue and m_hIDtable are not used!
  private boolean     streamMode = true;
  private boolean     robustMode = false;
  private ErrorHandler errorHandler = null; // new ErrorStore(); // default error handler

  private boolean     generateUniqueResources = false;

  // END SM
  private Hashtable     m_hIDtable = new Hashtable ();
  private int           m_iReificationCounter = 0;

  /**
   * Add the element <i>e</i> to the <i>m_vResolveQueue</i>
   * to be resolved later.
   */
  public void resolveLater (Element e) {
    // SM
    if(!streamMode)
      // END SM
      m_vResolveQueue.addElement (e);
  }

  /**
   * Go through the <i>m_vResolveQueue</i> and assign
   * direct object reference for each symbolic reference
   */
  public void resolve () throws SAXException {

    // SM
    if(streamMode) {
      return;
    }
    // END SM

    for (int x = 0; x < m_vResolveQueue.size(); x++) {
      Element e = (Element)m_vResolveQueue.elementAt(x);

      String sAbout = e.about();
      //      System.out.println("RESOLVING ABOUT: " + sAbout);
      if (sAbout != null) {
        if (sAbout.startsWith ("#"))
          sAbout = sAbout.substring(1);
        Element e2 = (Element)lookforNode(sAbout);
        if (e2 != null) {
          //-> ADDED by PA
          e.resource(e2.ID());
          //-> end ADDED by PA
          e.addTarget (e2);
        } else {
          addError ("Unresolved internal reference to "+sAbout);
        }
      }

      String sResource = e.getAttribute (RDFMS_resource);
      if (sResource != null) {
        if (sResource.startsWith ("#"))
          sResource = sResource.substring(1);
        Element e2 = (Element)lookforNode(sResource);
        if (e2 != null) {
          //-> ADDED by PA
          e.resource(e2.ID());
          //-> end ADDED by PA
          e.addTarget (e2);
        }
      }


      String sAboutEach = e.getAttribute (RDFMS_aboutEach);
      if (sAboutEach != null) {
        Element e2 = (Element)lookforNode(sAboutEach.substring(1));
        if (e2 != null) {
          //-> ADDED by PA
          e.resource(e2.ID());
          //-> end ADDED by PA
          e.addTarget (e2);
        }
      }

      String sAboutEachPrefix = e.getAttribute (RDFMS_aboutEachPrefix);
      if (sAboutEachPrefix != null) {
        for (int y = 0; y < m_vResources.size(); y++) {
          Element ele = (Element)m_vResources.elementAt(y);
          String sA = ele.about();
          if (sA.startsWith (sAboutEachPrefix)) {
            e.addTarget (ele);
          }
        }
      }
    }
    m_vResolveQueue.removeAllElements();
  }

  /**
   * Look for a node by name <i>sID</i> from the Hashtable
   * <i>m_hIDtable</i> of all registered IDs.
   */
  public Element lookforNode (String sID) {

    // cannot be called in streamMode

    if (sID == null) {
      return null;
    } else {
      return (Element)m_hIDtable.get (source()+/*"#"+*/sID); // ???
    }
  }

  /**
   * Add an element <i>e</i> to the Hashtable <i>m_hIDtable</i>
   * which stores all nodes with an ID
   */
  public void registerID (String sID, Element e) throws SAXException {

    // SM
    if(streamMode)
      return;
    // END SM

    if (m_hIDtable.get (sID) != null)
      addError("Node ID '"+sID+"' redefined.");
    m_hIDtable.put (sID, e);
  }

  /**
   * Create a new reification ID by using a name part and an
   * incremental counter <i>m_iReificationCounter</i>.
   */
  public String newReificationID () {

    try {
      if(generateUniqueResources)
	return nodeFactory.createUniqueResource().getURI();
    } catch (ModelException any) {}

    m_iReificationCounter++;

    String reifStr;
    if(source() != null)
      reifStr = Element.makeAbsolute( new String ("genid" + m_iReificationCounter), source() );
    else
      reifStr = new String ("#genid" + m_iReificationCounter);
    //  System.out.println("REIFIED: " + reifStr + " (" + source() + ")");
    return reifStr;
  }

  /**
   * Add an element <i>e</i> to the Vector <i>m_vResources</i>
   * which stores all nodes with an URI
   */
  public void registerResource (Element e) {
    // SM
    if(!streamMode)
      // END SM
      m_vResources.addElement (e);
  }

  public void makeMarkupST (Element ele) {
    m_sLiteral += "<" + ele.name();

    Enumeration e = ele.attributes();
    while (e.hasMoreElements()) {
      QName sAttribute = (QName)e.nextElement();
      String sAttributeValue = (String)ele.getAttribute (sAttribute);
      m_sLiteral += " " + sAttribute + "='" + sAttributeValue + "'";
    }
    m_sLiteral += ">";
  }

  public void makeMarkupET (String name) {
    m_sLiteral += "</" + name + ">";
  }

  public void makeMarkupChar (String s) {
    m_sLiteral += s;
  }

  /**
   * This method adds a warning for each name (element & attribute)
   * which looks like it is from RDF but it is not.
   *
   * Note: this method is useful for interactive use but can be
   * omitted from embedded applications.
   */
  /*
    public void likeRDF (String sNamespace, String sElement) {
    if (!sNamespace.equals (RDFMS)) {
    if (sElement.equals ("RDF") ||
    sElement.equals ("Description") ||
    sElement.equals ("Bag") ||
    sElement.equals ("Alt") ||
    sElement.equals ("Seq") ||
    sElement.equals ("li") ||
    sElement.equals ("_1") ||
    sElement.equals ("ID") ||
    sElement.equals ("parseType") ||
    sElement.equals ("resource") ||
    sElement.equals ("about") ||
    sElement.equals ("value") ||
    sElement.equals ("subject") ||
    sElement.equals ("predicate") ||
    sElement.equals ("object") ||
    sElement.equals ("type")) {
    addWarning ("Name '"+sElement+"' looks like it is from RDF but it has namespace "+sNamespace+"\n");
    }
    }
    }
  */


  public Resource createResource(String str) throws ModelException {

    // check whether we can extract namespace
    Enumeration en = listNamespaces();
    //    System.out.println("NS CHECK for " + str);
    while(en.hasMoreElements()) {

      String ns = (String)en.nextElement();
      //      System.out.println("NS CHECK " + str + " against " + ns);
      if(str.startsWith(ns))
	return createResource(ns, str.substring(ns.length()));
    }

    // if none of the declared namespaces matched, guess it!
    // starting from the end of the string, look for any of "#","/","\",":"
    for(int i = str.length() - 1; i > 0; i--) {

      char c = str.charAt(i);
      switch(c) {
      case '#':
      case '\\':
      case '/':
      case ':':
	String localName = i < str.length() - 1 ? str.substring(i+1) : "";
	String ns = str.substring(0, i+1);
	return createResource(ns, localName);
      }
    }

    //    System.out.println("OPAQUE RESOURCE: " + str);
    return nodeFactory.createResource(GenericParser.create(str));
  }

  public Resource createResource(String namespace, String name) throws ModelException {

    Resource r = nodeFactory.createResource(GenericParser.create(namespace), GenericParser.create(name));
    //    System.out.println("RESOURCE " + r.getNamespace() + " --- " + r.getLocalName());
    return r;
  }

  public Literal createLiteral(String str, boolean isXML)  throws ModelException {

    return nodeFactory.createLiteral(GenericParser.create(str));
  }

  private Literal createLiteral(String str)  throws ModelException {

    return nodeFactory.createLiteral(GenericParser.create(str));
  }

  static void bailOut() {
    System.err.println("Usage: java -Dorg.xml.sax.parser=<classname> org.w3c.rdf.SiRPAC " +
                       //                        "[-fetch_schemas | -f] [-stream | -s] [-robust | -r] [ URI | filename ]");
                       "[-robust | -r] [-orthodox -o] [ URI | filename ]");
    System.err.println ("This is revision " + REVISION);
    System.exit(1);
  }

  /**
   * main method for running SiRPAC as an application
   */
  public static void main (String args[]) throws Exception {

    SiRPAC parser = new SiRPAC ();
    String urlStr = null;

    boolean robust = false;
    boolean experimenal = true;

    for(int i = 0; i < args.length; i++) {
      String a = args[i];
      //      if(a.startsWith("-f"))
      //        parser.fetchSchemas (true);
      //      else if(a.startsWith("-s"))
      //        parser.setStreamMode(true);
      if(a.startsWith("-r"))
        parser.setRobustMode(true);
      else if(a.startsWith("-o"))
	parser.useExperimentalFeatures(false);
      else {
        urlStr = args[i];
        if(i+1 < args.length)
          bailOut();
      }
    }

    InputSource source  = GenericParser.getInputSource(urlStr);
    RDFConsumer consumer = new DumpConsumer();
    ErrorStore handler = new ErrorStore();

    parser.setErrorHandler(handler);
    try {
      parser.parse(source, consumer);
    } catch (SAXException se) {
      System.err.println("Error during parsing: " + se.getMessage());
      se.getException().printStackTrace(System.err);
    }

    String sErrors = handler.errors ();
    if (sErrors != null && sErrors.length() > 0)
      System.err.println ("Errors during parsing:\n"+sErrors);

  }
}

