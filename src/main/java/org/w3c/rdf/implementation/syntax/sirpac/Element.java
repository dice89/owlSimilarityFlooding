/**
 * A simple Element class for storing the element name, attributes
 * and children.
 *
 * $Log: Element.java,v $
 * Revision 1.3  2001/09/17 03:39:16  stefan
 * *** empty log message ***
 *
 * Revision 1.13  1999/05/04 15:23:39  lehors
 * commit after CVS crash
 *
 * Revision 1.10  1999/04/26 14:51:26  jsaarela
 * URI resolution improved.
 *
 * Revision 1.9  1998/12/15 17:00:44  jsaarela
 * New distribution release V1.7 on 15-Dec-98.
 *
 * Revision 1.8  1998/10/09 17:26:50  jsaarela
 * Parser conformance to RDF Model&Syntax dated 19981008
 *
 * Revision 1.7  1998/09/08 15:54:01  jsaarela
 * Distribution release V1.4 - aboutEachPrefix added, namespace management
 * improved.
 *
 * Revision 1.6  1998/08/28 10:03:15  jsaarela
 * Distribution release 1.3 on 28-Aug-98.
 *
 * Revision 1.5  1998/08/12 07:54:55  jsaarela
 * Namespace management now corresponds with the W3C Working
 * Draft dated 2-Aug-98.
 *
 * Revision 1.4  1998/07/30 13:39:32  jsaarela
 * multiple internal references fixed,
 * properties without children fixed.
 *
 * Revision 1.3  1998/07/27 12:20:54  jsaarela
 * 1st distribution version logged in.
 *
 *
 * @author Janne Saarela
 */
package org.w3c.rdf.implementation.syntax.sirpac;

import java.net.URL;
import java.util.*;
import java.io.*;
import org.xml.sax.AttributeList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import edu.stanford.db.xml.util.*;

public class Element
{
  String m_sNamespace = null;
    private String	m_sName = null;
  private Hashtable	m_attributes = new Hashtable(); // QName -> String
    private Vector	m_children = new Vector();
    private String	m_sResource = null;
    private String	m_sID = null;
    private String	m_sBagID = null;
    private String	m_sAbout = null;
    private String	m_sAboutEach = null;
    private String	m_sAboutEachPrefix = null;
    private Vector	m_vTargets = new Vector ();
    private boolean	m_bDone = false;
    private String	m_sPrefix = null;
  /*
  boolean preserveWhiteSpace  = false; // unset
  
  void setPreserveWhiteSpace(boolean b) {
    System.out.println("---PRESERVE: " + b + " " + this + ", " + name());
    preserveWhiteSpace = b;
  }

  boolean preserveWhiteSpace() {
    return preserveWhiteSpace;
  }
  */  

  public Element (String sName, AttributeList al) {

    this(null, sName, al);
  }

  /** sName is a URI */
  public Element (String sNamespace, String sName, AttributeList al) /*throws SAXException*/ {

    m_sNamespace = sNamespace;
    m_sName = sName;

	if (al != null) {
	    int iLength = al.getLength ();
	    if (al == null) {
		// System.out.println("[Attributes not available]");
	    } else {
		for (int x = 0; x < iLength; x++) {
		    String aName = al.getName (x);
		    String aValue = al.getValue (x);

		    m_attributes.put (new QName(aName), aValue);
		}
	    }
	}
    }

  public Element (String sNamespace, String sName, Attributes al) /*throws SAXException*/ {

    m_sNamespace = sNamespace;
    m_sName = sName;

    if (al != null) {
      int iLength = al.getLength ();
      if (al == null) {
	// System.out.println("[Attributes not available]");
      } else {
	for (int x = 0; x < iLength; x++) {
	  
	  m_attributes.put (new QName(al.getURI(x), al.getLocalName(x)), al.getValue(x));
	}
      }
    }
  }

    public String name() {
      return m_sNamespace != null ? m_sNamespace + m_sName : m_sName;
    }

  public String localName() {

    return m_sName;
  }

  public String namespace() {

    return m_sNamespace;
  }

    public void prefix (String sPrefix) {
	m_sPrefix = sPrefix;
    }

    public String prefix () {
	if (m_sPrefix != null)
	    return m_sPrefix + ":";
	else
	    return "";
    }

    public void addChild (Element e) {
	m_children.addElement (e);
    }

    public Enumeration children () {
	return m_children.elements();
    }

  /** @returns enumeration of QNames as keys */
    public Enumeration attributes () {
	return m_attributes.keys();
    }

    public void addAttribute (QName name, String sValue) {
	if (name == null)
	  m_attributes.put (name, sValue);
    }

    public void removeAttribute (QName sName) {
	m_attributes.remove (sName);
    }

    public String getAttribute (QName sName) {
	return (String)m_attributes.get (sName);
    }

    public String getAttribute (String qName) {
	return (String)m_attributes.get (new QName(qName));
    }

  //    public String getAttribute (String sNamespace, String sName) {
  //	return (String)m_attributes.get (sNamespace+sName);
  //    }

    public void addTarget (Element e) {
	m_vTargets.addElement (e);
    }

    public Enumeration targets () {
	return m_vTargets.elements();
    }

    public Element target () {
	if (m_vTargets.size() == 0)
	    return null;
	else
	    return (Element)m_vTargets.elementAt(0);
    }

    public void resource (String sResource) {
	m_sResource = sResource;
    }

    public void resource (String sResource, String sContext) {
	m_sResource = makeAbsolute (sResource, sContext);
    }

    public String resource () {
	return m_sResource;
    }

    public void ID (String sID) {
	m_sID = sID;
    }

    public void ID (String sID, String sContext) {
	m_sID = makeAbsolute (sID, sContext);
    }

    public String ID () {
	return m_sID;
    }

    public void bagID (String sBagID) {
	m_sBagID = sBagID;
    }


    public void bagID (String sBagID, String sContext) {
	m_sBagID = makeAbsolute (sBagID, sContext);
    }

    public String bagID () {
	return m_sBagID;
    }

    public void about (String sAbout) {
	m_sAbout = sAbout;
    }

    public void about (String sAbout, String sContext) {
	m_sAbout = makeAbsolute (sAbout, sContext);
    }

    public String about () {
	return m_sAbout;
    }

    public void aboutEach (String sAboutEach) {
	m_sAboutEach = sAboutEach;
    }

    public void aboutEach (String sAboutEach, String sContext) {
	m_sAboutEach = makeAbsolute (sAboutEach, sContext);
    }

    public String aboutEach () {
	return m_sAboutEach;
    }

    public void aboutEachPrefix (String sAboutEachPrefix) {
	m_sAboutEachPrefix = sAboutEachPrefix;
    }

    public void aboutEachPrefix (String sAboutEachPrefix, String sContext) {
	m_sAboutEachPrefix = makeAbsolute (sAboutEachPrefix, sContext);
    }

    public String aboutEachPrefix () {
	return m_sAboutEachPrefix;
    }

    public void linearize (int indent, PrintStream ps) {
	for (int x = 0; x < indent; x++) {
	    ps.print (" ");
	}
	System.out.print ("Element "+name()+" (");

	Enumeration eKeys = m_attributes.keys ();
	while (eKeys.hasMoreElements()) {
	    QName sName = (QName)eKeys.nextElement ();
	    String sValue = (String)m_attributes.get (sName);
	    System.out.print (" "+sName+"="+sValue);
	}
	System.out.print (")\n");

	Enumeration e = children();
	while (e.hasMoreElements()) {
	    Element ele = (Element)e.nextElement();
	    ele.linearize (indent + 2, ps);
	}
    }

    public boolean done () {
	return m_bDone;
    }

    public void done (boolean b) {
	m_bDone = b;
    }

    /**
     * Private methods for this class only
     */

    public static String 	makeAbsolute (String sURI, String context) {

      if(sURI != null && sURI.indexOf(':') > 0)
	return sURI; // already absolute

	String sResult = new String ();
	if (sURI != null &&
	    context != null) {

	  if(sURI.startsWith("#") &&
	     context.endsWith("#"))
	    return context + sURI.substring(1);

	    /**
	     * If sURI has .. then it indicates relative URI which
	     * we must make absolute
	     */
	    if (sURI.startsWith ("..")) {
		try {
		    URL absoluteURL = new URL (new URL(context), sURI);
		    sResult = absoluteURL.toString();
		} catch(Exception e) {
		    System.err.println("RDF Resource - cannot combine " + 
				       context + " with " +sURI);
		}
	    } else {
		/**
		 * If sURI is an absolute URI, don't bother
		 * with it
		 */
		try {
		    URL absoluteURL = new URL (sURI);
		    sResult = sURI; // absoluteURL.toString();
		    //		    sResult = absoluteURL.toString();
		    //		    System.out.println("ABS1: " + sResult + " from " + sURI + ", " + context);

		} catch(Exception e) {
		    /**
		     * Well, the sURI wasn't absolute either,
		     * try making absolute with the context
		     */

		  // Cases: "dir/foo.rdf#" + "Top/Arts" should not produce dir/Top/Arts
		  //        "ex.rdf#prg1" + "#query2" should produce "ex.rdf#query2"
		  //		    if (sURI.indexOf ('/') > -1) {
		    if (sURI.startsWith ("#")) {
			try {
			    URL absoluteURL = new URL (new URL(context), sURI);
			    sResult = absoluteURL.toString();
			} catch (Exception e2) {
			  sResult = context +  sURI; // ???
			}
		    } else
		      sResult = context + /*"#" +*/ sURI; // ???
		    
		  //System.out.println("ABS2: " + sResult + " from " + sURI + ", " + context + ": " + e.getMessage());
		}
	    }
	    return sResult;
	} else {
	  //System.out.println("ABS3: " + sURI + " from " + sURI + ", " + context);
	    return sURI;
	}
    }

  // SM
  public String toString() {
    String res = "[Element " + name() + "(";
    for(Enumeration en = attributes(); en.hasMoreElements();) {
      QName aName = (QName)en.nextElement();
      String aValue = getAttribute(aName);
      res += aName + "=" + aValue + " ";
    }
    res += ")<";
    Enumeration en = children();
    while(en.hasMoreElements()) {
      res += en.nextElement().toString();
      if(en.hasMoreElements())
	res += ",";
    }
    res += ">]";
    return res;
  }

  public static void main(String[] args) throws Exception {

    Element e = new Element("", null);
    System.err.println( e.makeAbsolute(args[0], args[1]) );
  }

}

