/**
 * Copyright © Sergey Melnik (Stanford University, Database Group)
 *
 * All Rights Reserved.
 */

package org.w3c.rdf.implementation.syntax.sirpac;

import org.w3c.rdf.model.*;
import org.w3c.rdf.syntax.*;
import org.w3c.rdf.vocabulary.rdf_syntax_19990222.RDF;
import org.w3c.rdf.vocabulary.rdf_schema_19990303.RDFS;
import edu.stanford.db.rdf.vocabulary.order_20000527.RDFX;
import org.w3c.rdf.util.RDFUtil;
//import org.w3c.tools.sorter.*;

import java.util.*;
import java.io.*;
import java.net.URL;

/**
 * A default implementation of the RDFMS interface.
 * For parsing it uses a modified version of
 * SiRPAC parser written by Janne Saarela at W3C.
 *
 * @author Sergey Melnik <melnik@db.stanford.edu>
 */

public class SiRS implements Comparator, RDFSerializer {

  Model model;
  Hashtable namespaces = new Hashtable();
  Map overridenNamespaceMap = null;
  String rdfNsAbb = null;

  // SM:experimental
  boolean ENABLE_EXPERIMENTAL = true;
  boolean useAbbrevSyntax = true; // by default try to use abbreviated syntax

  public SiRS() {
  }

  /**
   * Allows overriding the way namespaces abbreviations are generated.
   * Maps namespaces to abbreviations. If a mapping is missing, a default one will be used.
   */
  public SiRS(Map namespaceMap) {

    this.overridenNamespaceMap = namespaceMap;
  }

  public void useExperimentalFeatures(boolean b) {

    ENABLE_EXPERIMENTAL = b;
  }

  // more efficient: avoids concatenation of ns and localname
  int compare(Resource r1, Resource r2) throws ModelException {

    // move reified stuff to the back
    boolean s1 = r1 instanceof Statement;
    boolean s2 = r2 instanceof Statement;

    if(!s1 && s2)
      return -1;
    else if(s1 && !s2)
      return 1;

    //    return r1.getLabel().compareTo(r2.getLabel());

    String ns1 = r1.getNamespace();
    String ns2 = r2.getNamespace();

    if(ns1 == null && ns2 != null)
      return -1;
    else if(ns2 == null && ns1 != null)
      return 1;
    else if((ns1 == null && ns2 == null) ||
	    ns1.equals(ns2))
      return r1.getLocalName().compareTo(r2.getLocalName());
    else
      return ns1.compareTo(ns2);

  }

  public int compare(Object o1, Object o2) {

    //    System.err.println("=== comparing " + o1 + " and " + o2);

    try {
      //      int t = ((Integer)handle).intValue();
      Statement t1 = (Statement)o1;
      Statement t2 = (Statement)o2;
      
      int res = compare(t1.subject(), t2.subject());
      
      if(res != 0)
	return res;
      
      /*
	// move rdf:type upwards
	boolean r1 = RDF_TYPE.equals( t1.predicate() );
	boolean r2 = RDF_TYPE.equals( t2.predicate() );
	
	if(r1 && !r2)
	return -1;
	if(r2 && !r1)
	return 1;
      */
      
      // sort abbreviatable objects before long objects
      RDFNode n1 = t1.object();
      RDFNode n2 = t2.object();
      
      // which is a resource?
      boolean r1 = n1 instanceof Resource;
      boolean r2 = n2 instanceof Resource;
      
      // push resources down
      if(r1 && r2)
	return compare((Resource)n1, (Resource)n2); // .getLabel().compareTo( n2.getLabel() );
      if(r2)
	return -1;
      else if(r1)
	return 1;
      
      // both are literals
      // which can be abbreviated?
      r1 = canAbbrev( n1.getLabel() );
      r2 = canAbbrev( n2.getLabel() );
      
      if(r1 && !r2)
	return -1;
      else if(r2 && !r1)
	return 1;
      
      // in case of literals, first sort them out
      // according to their names (important for postponed elements)
      //      res = t1.predicate().getLabel().compareTo( t2.predicate().getLabel() );
      res = compare(t1.predicate(), t2.predicate());
      if(res != 0)
	return res;
      else // compare their values
	return n1.getLabel().compareTo( n2.getLabel() );

    } catch (ModelException exc) {}

    return 0; // do not sort
  }

  final char ABB_LONG = (char)0;
  final char ABB_CDATA = (char)1;

  final char ANYQUOTE = (char)0;
  final int MAX_ABBLENGTH = 60;

  /**
   * @return Quote sigh that can be used to abbreviate this string 
   * as an attribute. (char)0 if is CDATA or too long.
   */
  char abbrevQuote( String s ) {

    char quote = ANYQUOTE; // any
    boolean hasBreaks = false;
    boolean whiteSpaceOnly = true;

    for(int i=0; i < s.length(); i++) {
      char c = s.charAt(i);
      if(c == '<' /*|| c == '>'*/ || c == '&')
	return ABB_CDATA;
      else if(c == '\n')
	hasBreaks = true;

      if(c == '"' || c == '\'') {
	if(quote == ANYQUOTE)
	  quote = (c == '"') ? '\'' : '"';
	else if (c == quote)
 	  return ABB_CDATA;
      }

      if(!Character.isWhitespace(c))
	whiteSpaceOnly = false;
    }

    if(whiteSpaceOnly && hasBreaks)
      return ABB_CDATA;

    if(hasBreaks || s.length() > MAX_ABBLENGTH) // optically nice value
      return whiteSpaceOnly ? ABB_CDATA : ABB_LONG;

    return quote == ANYQUOTE ? '"' : quote;

  }

  boolean canAbbrev(RDFNode n) throws ModelException {
    // FIXME: don't abbreviate if order is supported
    return useAbbrevSyntax && (n instanceof Literal) && canAbbrev(n.getLabel());
  }

  boolean canAbbrev(String s) {

    // just to speed up for long strings
    if(s.length() > MAX_ABBLENGTH)
      return false;

    char c = abbrevQuote(s);
    return c == '"' || c == '\'';
  }

  Hashtable getNamespaces(Iterator it) throws ModelException {

    Hashtable h = new Hashtable();

    while(it.hasNext()) {
      Statement t = (Statement)it.next();
      /*
      getNamespace(h, t.subject().getURI());
      RDFNode n = t.object();
      if(n instanceof Resource)
	getNamespace(h, ((Resource)n).getURI());
	*/
      // collect types
      boolean o_done = false, s_done = false;

      if(RDF.type.equals(t.predicate())) { // RDFMS.type
	getNamespace(h, (Resource)t.object());
	o_done = true;
      } else if(RDFS.subClassOf.equals(t.predicate()) || RDFS.subPropertyOf.equals(t.predicate())) {
	getNamespace(h, t.subject());
	getNamespace(h, (Resource)t.object());
	o_done = s_done = true;
      } else if (ENABLE_EXPERIMENTAL && RDFX.order.equals(t.predicate())) {
	useAbbrevSyntax = false;
      }

      getNamespace(h, t.predicate());

      // weak check: only if explicit namespace available
      if(!s_done && t.subject().getNamespace() != null)
	getNamespace(h, t.subject());

      if(!o_done && t.object() instanceof Resource && ((Resource)t.object()).getNamespace() != null)
	getNamespace(h, (Resource)t.object());
    }
    return h;
  }

  // not really, but a good conservative approximation
  static boolean validXMLTag(String str) {
    for(int i = 0; i < str.length(); i++) {
      char c = str.charAt(i);
      if(!(Character.isDigit(c) || Character.isLetter(c) || c=='.' || c=='_' || c=='-'))
	return false;
    }
    return true;
  }

  void getNamespace(Hashtable h, Resource r) throws ModelException {
 
    String ns = RDFUtil.getNamespace(r);
    //    if(ns != null && ns.length() > 0) {
    // space is ok; make sure we test it in rdf:about, rdf:resource and rdf:ID
    //    System.out.println("NS: " + ns + " for " + r);

    if(h.containsKey(ns))
      return;
    // else check for proper prefixes and remove them if necessary
    /*
    Enumeration en = ((Hashtable)h.clone()).keys();
    while(en.hasMoreElements()) {
      String nsPref = (String)en.nextElement();
      if(ns.startsWith(nsPref)) {// remove nsPref from hashtable
	h.remove(nsPref);
      } else if(nsPref.startsWith(ns))
	return; // do not insert proper prefixes
    }
    */
    h.put(ns, r.getURI());
    //    System.out.println("INSERTING: " + ns + " for " + r);
    //    }
  }


  final String INDENT_ABB = "\t ";
  final String INDENT_LONG = "\t";


  public void serialize(Model m, Writer w) throws IOException, ModelException {

    if(m == null)
      return;

    model = m;

    if(overridenNamespaceMap != null)
      rdfNsAbb = (String)overridenNamespaceMap.get(RDF._Namespace);
    if(rdfNsAbb == null)
      rdfNsAbb = "rdf";

    PrintWriter out = new PrintWriter(w);
    out.println("<?xml version='1.0' encoding='ISO-8859-1'?>");

    if(m.isEmpty()) {
      // write empty model
      out.println("<" + rdfNsAbb + ":RDF xmlns:" + rdfNsAbb + "=\"" + RDF._Namespace + "\"/>");
      out.flush();
      return;
    }

    //    String modelURI = m.getSourceURI();

    // triples that have been written out
    Hashtable done = new Hashtable();

    //    Vector tv = RDFUtil.getStatementVector(m);
    Statement[] tv_arr = RDFUtil.getStatementArray(m);
    // sort order: GROUP BY resource, abbreviatable
    Arrays.sort(tv_arr, this);

    //    System.err.println("SORTING DONE!");

    List tv = Arrays.asList(tv_arr);

    //    System.err.println("=== SORTED: " + tv);

    out.println("<!DOCTYPE " + rdfNsAbb + ":RDF [");

    // collect namespaces

    Hashtable ns = getNamespaces(tv.iterator());
    int counter = 0;
    String indent = INDENT_ABB;
    namespaces.clear();
    // just to be sure
    ns.put(RDF._Namespace, rdfNsAbb);

    for(Enumeration en = ns.keys(); en.hasMoreElements();) {

      // assign a shortcut to a namespace

      String s = (String)en.nextElement();

      //      System.out.println("URL: " + modelURI + ", comp " + s);

      String shortcut = null;

      if(overridenNamespaceMap != null) {
	shortcut = (String)overridenNamespaceMap.get(s);
      }

      while(shortcut == null) {

	if(RDF._Namespace.equals(s))
	  shortcut = rdfNsAbb;
	else if(RDFS._Namespace.equals(s))
	  shortcut = "rdfs";
	//      else if(modelURI != null && modelURI.equals(s))
	//	shortcut = "this";
	else {
	  shortcut = counter <= (int)'z' - (int)'a' ? String.valueOf((char)((int)'a' + counter++)) : "n" + (counter++ - (int)'z' + (int)'a');
	  // make sure it's not used in overridenNamespaceMap
	  if(overridenNamespaceMap != null && overridenNamespaceMap.containsValue(shortcut))
	    shortcut = null; // retry
	}
      }

      namespaces.put(s, shortcut);
      out.print(indent + "<!ENTITY " + shortcut + " '" + s + "'>");
			//      out.print(indent + "xmlns:" + shortcut + "=\"" + s + "\"");

      indent = "\n" + INDENT_ABB;
    }
    out.println("\n]>");


		// print out the declaration itself
    out.print("<" + rdfNsAbb + ":RDF ");

    indent = "";
    for(Enumeration en = namespaces.elements(); en.hasMoreElements();) {

      String shortcut = (String)en.nextElement();
			out.print(indent + "xmlns:" + shortcut + "=\"&" + shortcut + ";\"");
      indent = "\n" + INDENT_ABB;
    }
    out.println(">");



    // contains triples having the same subject
    Vector group = new Vector();

    Statement tCurrent = null, tNext = null;
    StringBuffer buf = new StringBuffer();

    // loop over all triples
    for(int i=0; i <= tv.size(); i++) { // loop one more time than needed

      tCurrent = tNext;
      tNext = null;

      if(i < tv.size()) {
	tNext = (Statement)tv.get(i);
      }

      //      System.err.println("=== looking at " + tNext);

      // if different resource starts
      if(tCurrent == null || tNext == null || !tCurrent.subject().equals(tNext.subject()) ) {

	if(i > 0) {
	  // process group of triples with equals subjects, contains at least 1 element

	  //	  System.err.println("@" + i + " STARTING GROUP: " + tCurrent.subject() + ", " + group.size());

	  // Problem 1: find the first "type" and remove it from the group
	  Statement type = null;
	  String qualName = null; // qualified name of this resource (if any)

	  boolean hasType = false;

	  //	  System.err.println("--- GROUP: " + group);

	  for(int j=0; j < group.size(); j++) {
	    Statement tEl = (Statement)group.elementAt(j);
	    Resource predicate = tEl.predicate();
	    if( predicate.equals(RDF.type) ) {
	      String tagName = RDFUtil.getLocalName((Resource)tEl.object());
	      if(!hasType && validXMLTag(tagName)) {
		type = tEl;
		qualName = namespaces.get( RDFUtil.getNamespace((Resource)type.object()) ) + ":" + tagName;
		group.removeElementAt(j--);
		hasType = true;
		//	      break; // don't because need to test order
	      }
	    }
	    // experimental support for ordering
	    if(ENABLE_EXPERIMENTAL && (tEl.subject() instanceof Statement) &&
	       (RDFX.order.equals(predicate) || RDFX.backwardOrder.equals(predicate))) {
	      group.removeElementAt(j--);
	    }
	  }

	  // Problem 2: find properties that can be abbreviated and have equal names
	  // Move all of them to the "bottom"
	  int abbNum = 0; // number of abbreviatable
	  int endOfGroup = group.size();

	  // order: don't write empty descriptions
	  if(type == null && endOfGroup == 0)
	    continue;

	  // filter out lists of abbreviatable attributes and move them to the bottom

	  Statement moveToBottom = null; // we both have the statement, and know the predicate!
	  boolean movedAlready = false;

	  for(int j=0; j < endOfGroup; j++) {
	    
	    Statement tEl = (Statement)group.elementAt(j);
	    
	    //	    System.err.println("TESTING PRED: " + abbNum + ", " + endOfGroup + ", " + tEl.predicate());
	  
	    if(!canAbbrev(tEl.object())) {
	      //	      abbNum = j;
	      break;

	    } else {

	      if(moveToBottom != null && tEl.predicate().equals( moveToBottom.predicate() )) {

		// make sure the whole group is moved to the bottom
		
		/*
		group.removeElementAt(j);
		group.addElement(tEl);
		endOfGroup--;
		j--;
		*/

		group.removeElementAt(j);

		if(!movedAlready) {
		  group.removeElementAt(j-1);
		  group.addElement(moveToBottom);
		  --endOfGroup;
		  --j;
		  // undo previous abbreviation
		  if(abbNum > 0) // really need to check?
		    abbNum--;
		  movedAlready = true;
		}

		group.addElement(tEl);
		--endOfGroup;
		--j;
		
	      } else {

		abbNum++;
		movedAlready = false;
		moveToBottom = tEl;
	      }
	    }
	  }

	  // prepare variables
	  Resource subj = tCurrent.subject();
	  boolean shortDescription = abbNum == group.size();

	  // now: "type" contains type of the described resource (+ qualName)
	  //      abbNum contains the number of abbreviatable resources
	  //      subj   contains the subject of the triple
	  //      shortDescription ( /> )

	  //	  System.err.println("ABBNUM: " + abbNum);
	  
	  // start description
	  if(type != null)
	    out.print("<" + qualName);
	  else
	    out.print("<" + rdfNsAbb + ":Description");
	  out.print(IDorAbout(subj.getLabel()));

	  // process abbreviated literals
	  //	  indent = " ";
	  for(int j=0; j < abbNum; j++) {
	    
	    Statement tEl = (Statement)group.elementAt(j);
	    Resource pred = tEl.predicate();
	    RDFNode obj = tEl.object();
	    // subj, pred, obj
	    
	    String p_ns = (String) namespaces.get( RDFUtil.getNamespace(pred) );
	    String p_name = RDFUtil.getLocalName(pred);
	    char quote = abbrevQuote( obj.getLabel() );
	    
	    out.print(indent + p_ns + ":" + p_name + "=" + quote + obj.getLabel() + quote);
	    //	    indent = "\n" + INDENT_ABB;
	  }

	  if(shortDescription)
	    // we are done
	    out.println("/>");
	  else {

	    out.println('>');
	    // process resources and long literals (can also be postponed)

	    for(int j=abbNum; j < group.size(); j++) {
		
	      Statement tEl = (Statement)group.elementAt(j);
	      Resource pred = tEl.predicate();
	      RDFNode obj = tEl.object();

	      String orderPrefix = (String)namespaces.get( RDFUtil.getNamespace(RDFX.order) );

	      // experimental support for ordering
	      String orderAttr = "";
	      if(ENABLE_EXPERIMENTAL) {
		String forwardOrder = RDFUtil.getObjectLiteral(model, tEl, RDFX.order);
		String backwardOrder = RDFUtil.getObjectLiteral(model, tEl, RDFX.backwardOrder);
		if(forwardOrder != null)
		  orderAttr += " " + orderPrefix + ":order=\"" + forwardOrder + "\"";
		if(backwardOrder != null)
		  orderAttr += " " + orderPrefix + ":backwardOrder=\"" + backwardOrder + "\"";
	      }

	      String p_ns = (String) namespaces.get( RDFUtil.getNamespace(pred) );
	      String p_name = RDFUtil.getLocalName(pred);

	      if(obj instanceof Resource) {
		// resource
		out.println(INDENT_LONG + "<" + p_ns + ":" + p_name + " " + rdfNsAbb + ":resource=\"" +
			    //			    shortcutPrefix( (RDFUtil.isQualified(obj.getLabel()) ? resourceID( obj.getLabel() ) : "#" + obj.getLabel()) ) +
			    shortcutPrefix( obj.getLabel() ) +
			    "\"" + orderAttr + "/>");
	      } else {
		
		char quote = abbrevQuote( obj.getLabel() );
		
		out.print(INDENT_LONG + "<" + p_ns + ":" + p_name);
		if(quote == ABB_CDATA)
		  out.print(" xml:space='preserve'");
		out.print(orderAttr + ">");

		if(quote == ABB_CDATA) {
		  out.print("<![CDATA[");
		  // write out CDATA
		  escapeCDATA(out, obj.getLabel());
		} else
		  out.print(obj.getLabel());
		
		if(quote == ABB_CDATA)
		  out.print("]]>");
		out.println("</" + p_ns + ":" + p_name + ">");
	      }
	    }

	    // end description
	    if(type != null)
	      out.println("</" + qualName + ">");
	    else
	      out.println("</" + rdfNsAbb + ":Description>");
	  }
	  
	  // end processing of the group
	  group.setSize(0);
	}

      }

      if(tNext != null) // continue building group
	group.addElement( tNext );

    } // loop over all triples

    out.println("</" + rdfNsAbb + ":RDF>");
    out.flush();
    /*
    for(int i=0; i < tv.size(); i++) {
      Statement t = (Statement)tv.elementAt(i);
      w.write ("triple(\""+t.predicate()+"\",\""+t.subject()+"\",\""+t.object()+"\").\n");
    }
    */
  }

  /**
   * ]]> cannot be there!
   * FIXME: how to we encode binary data? It is a function
   * of some more abstract layer?
   */
  void escapeCDATA(PrintWriter out, String str) {

    int start = 0, i = 0;
    do {
      i = str.indexOf("]]>", start);
      if(i >= 0) {
	out.print(str.substring(start, i+2));
	out.print("]]><![CDATA[");
	start = i + 2;
      } else
	out.print(str.substring(start));
    } while (i >= 0 && start < str.length());
  }
  /**
   * < and &
   */
  void escapeAttValue(StringBuffer buf, String str) {

    for(int i=0; i < str.length(); i++) {
      char c = str.charAt(i);
      if(c == '<')
	buf.append("&lt;");
      else if(c == '&')
	buf.append("&amp;");
      else
	buf.append(c);
    }
  }

  String escapeAttValue(String s) {

    for(int i=0; i < s.length(); i++) {
      char c = s.charAt(i);
      if(c == '<' || c == '>' || c == '&') {
	StringBuffer buf = new StringBuffer();
	escapeAttValue(buf, s);
	return buf.toString();
      }
    }
    return s;
  }

  String IDorAbout(String s) throws ModelException {

    //    System.err.println("ID or ABOUT: " + model.getSourceURI() + ", " + s + ", " + RDFUtil.guessNamespace(s));

    //    if((model.getSourceURI() != null && s.startsWith(model.getSourceURI())) ||
    //       (model.getSourceURI() == null && RDFUtil.guessNamespace(s).length() == 0) ) {
    //      return " rdf:ID=\"" + RDFUtil.guessName(s) + "\"";
    //    } else
      return " " + rdfNsAbb + ":about=\"" + shortcutPrefix(s) + "\"";
  }

	String shortcutPrefix(String s) {

		for(Enumeration en = namespaces.keys(); en.hasMoreElements();) {
			String prefix = (String)en.nextElement();
			if(prefix.length() > 0)
			  if(s.startsWith(prefix))
			    return "&" + namespaces.get(prefix) + ";" + escapeAttValue(s.substring(prefix.length()));
		}
		return escapeAttValue(s);
	}

  /*
  String resourceID(String s) throws ModelException {

    if((model.getSourceURI() != null && s.startsWith(model.getSourceURI())) ||
       (model.getSourceURI() == null && RDFUtil.guessNamespace(s).length() == 0) ) {
      return "#" + RDFUtil.guessName(s);
    } else
      return s;
  }
  */

  /**
   * main method for running DefaultRDFMS as an application
   */
  /*
  public static void main(String args[]) throws Exception {

    if (args.length == 0) {
      System.err.println("Usage: java -Dorg.xml.sax.parser=<classname> org.ginf.helpers.RDFMSImpl [ URI | filename ]");
      //	    System.err.println ("This is revision "+REVISION);
      System.exit(1);
    }

    URL url = null;
    try {
      url = new URL (args[0]);
    } catch (Exception e) {
      url = new URL ("file", null, args[0]);
    }

    RDFFactory factory = new RDFFactoryImpl();

    //    factory.registry().addKnown( "http://purl.org/metadata/dublin_core#" );
    RDFMS p = factory.createRDFMS();

    //    InputSource source = new InputSource( new InputFilter ( url.openStream() ) );
    //    InputSource source = new InputSource( new DoNotCloseFilter ( url.openStream() ) );
    InputSource source = new InputSource(  url.openStream() );
    source.setSystemId( url.toString() );
    //    System.out.println("SYS ID: " + url.toString() );
    //source.setSystemId( "http://www.w3.org/1999/02/22-rdf-syntax-ns" );

    Model m = p.parse( source );

    RDFUtil.printStatements(m, System.out);
    p.serialize( m, new OutputStreamWriter(System.out));

    m = factory.createSchemaModel( m );

    //    System.err.println("Full schema info:");
    //    DefaultModel.printStatements(registry.get(), System.err);

    System.err.println("VALIDATION:");
    ((SchemaModel)m).validate();
    
  }
  */
}

