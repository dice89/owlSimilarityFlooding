/**
 * A simple Data class for storing character content.
 *
 * $Log: Data.java,v $
 * Revision 1.3  2001/09/17 03:39:16  stefan
 * *** empty log message ***
 *
 * Revision 1.13  1999/05/04 15:23:00  lehors
 * commit after CVS crash
 *
 * Revision 1.6  1999/05/04 14:52:43  jsaarela
 * Literal value now tells if it is well-formed XML.
 * Improved entity management in Data nodes.
 *
 * Revision 1.5  1998/10/09 17:26:55  jsaarela
 * Parser conformance to RDF Model&Syntax dated 19981008
 *
 * Revision 1.4  1998/09/08 15:54:00  jsaarela
 * Distribution release V1.4 - aboutEachPrefix added, namespace management
 * improved.
 *
 * Revision 1.3  1998/08/12 07:54:14  jsaarela
 * Namespace management now corresponds with the W3C Working
 * Draft dated 2-Aug-98.
 *
 * Revision 1.2  1998/07/27 12:20:48  jsaarela
 * 1st distribution version logged in.
 *
 *
 * @author Janne Saarela
 */
package org.w3c.rdf.implementation.syntax.sirpac;

import java.io.PrintStream;
import org.xml.sax.SAXException;

public class Data extends Element {
    private String	m_sContent = null;
    private boolean	m_bXML = false;

    public Data (String sContent) /*throws SAXException*/ {
	super ("[DATA: "+sContent+"]", null);
	m_sContent = sContent;
    }
    
    public Data (String sContent, boolean bXML) /*throws SAXException*/ {
	super ("[DATA: "+sContent+"]", null);
	m_sContent = sContent;
	m_bXML = bXML;
    }
    
    public String	data () {
	return m_sContent;
    }

    public void		set (String sData) {
	m_sContent = sData;
    }

    public boolean	isXML () {
	return m_bXML;
    }

    public void		linearize (int indent, PrintStream ps) {
	for (int x = 0; x < indent; x++) {
	    ps.print (" ");
	}
	System.out.println ("Content: "+m_sContent);
    }
}

