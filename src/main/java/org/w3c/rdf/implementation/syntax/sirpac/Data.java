
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

