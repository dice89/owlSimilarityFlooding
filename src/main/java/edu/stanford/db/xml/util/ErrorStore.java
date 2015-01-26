package edu.stanford.db.xml.util;

import org.xml.sax.*;

/**
 * An implementation of the <code>ErrorHandler</code>
 */

public class ErrorStore implements ErrorHandler {

  private String		m_sErrorMsg = new String ();
  private String		m_sWarningMsg = new String ();

  public ErrorStore() {
  }

    String getExceptionMsg (SAXParseException exception)
    {
	return exception.getMessage() +
	    " (" +
	  //	    exception.getSystemId() +
	    "line " +
	    exception.getLineNumber() +
	    ", column " +
	    exception.getColumnNumber() +
	    ")";
    }

    /**
     * Report all warnings, and continue parsing.
     *
     * @see org.xml.sax.ErrorHandler#warning
     */
    public void warning (SAXParseException exception)
    {
	m_sWarningMsg += getExceptionMsg(exception);
    }


    /**
     * Report all recoverable errors, and try to continue parsing.
     *
     * @see org.xml.sax.ErrorHandler#error
     */
    public void error (SAXParseException exception)
    {
      m_sErrorMsg += "Recoverable Error: " + getExceptionMsg(exception);
    }


    /**
     * Report all fatal errors, and try to continue parsing.
     *
     * <p>Note: results are no longer reliable once a fatal error has
     * been reported.</p>
     *
     * @see org.xml.sax.ErrorHandler#fatalError
     */
    public void fatalError (SAXParseException exception) throws SAXException
    {
      throw new SAXException("Fatal Error: " + getExceptionMsg(exception), exception);
    }

    public String errors () {
	return m_sErrorMsg;
    }

    public String warnings () {
	return m_sWarningMsg;
    }


}

