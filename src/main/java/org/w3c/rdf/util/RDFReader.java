/**
 * Copyright © Sergey Melnik (Stanford University, Database Group)
 *
 * All Rights Reserved.
 */

package org.w3c.rdf.util;

import org.xml.sax.*;

import java.io.*;
import java.net.*;

/**
 * A rough approximation for extracting RDF out of an arbitrary character stream.
 *
 * @author Sergey Melnik <melnik@db.stanford.edu>
 */

public class RDFReader extends FilterReader {

  StringBuffer nsbuf = new StringBuffer(); // buffer for reading namespaces
  int nsbufPos;

  StringBuffer buf = new StringBuffer(); // buffer for caching
  int bufPos;

  boolean done;
  String delimiter;
  int delimiterPos;

  static final int INITIAL = 0;
  static final int PASS = 1;
  static final int LT_EXPECTED = 2;
  static final int IN_RDF = 3;
  int state = INITIAL;

  public RDFReader(Reader in) {

    super(in);
    if(!in.markSupported())
      in = new BufferedReader(in);
  }
  
  public void close() throws IOException {
    // A SAX driver may attempt to close the stream
    // after parsing. We do not allow this!
    //    in.close();
  }

  public boolean ready() throws IOException {

    if(done)
      return false;

    if(bufPos < buf.length())
      return true;

    int c = read();

    if(!done) {
      setCache(c);
      return true;
    } else
      return false;
  }

  boolean isNameChar(char c) {

    return Character.isLetterOrDigit(c) | c == '.' | c == '-' | c == '_' | c == ':';
  }

  /**
   * return the consumed part of the string
  String consume(String str) {

    StringBuffer b = new StringBuffer();
    int i = 0;
    int c = 0;
    while(i < str.length() && ((c = readIt()) == str.charAt(i))) {
      b.append(c);
      i++;
    }
    return b.toString();
  }
   */

  void setCache(String str) {

    buf.setLength(0);
    bufPos = 0;
    buf.append(str);
  }

  void setCache(int c) {

    buf.setLength(0);
    bufPos = 0;
    buf.append((char)c);
  }

  public int read() throws IOException {

    if(done)
      return -1;

    // read buf empty
    if(bufPos < buf.length()) {
      return buf.charAt(bufPos++);
    }

    if(state == INITIAL) {
      in.mark(3);
      char c1 = (char)in.read();
      char c2 = (char)in.read();
      char c3 = (char)in.read();
      if(c1 == '<' && c2 == (int)'?' && c3 == (int)'x') {
	state = PASS;
	//	System.out.println("PASSING!");
      } else
	state = LT_EXPECTED;
      in.reset();
    }

    if(state == PASS)
      return in.read();

    while(!done && state == LT_EXPECTED) {

      char c = readIt();

      if(c == '<') {

	nsbuf.setLength(0);

	while(isNameChar(c = readIt())) {
	  nsbuf.append(c);
	}
	String s = nsbuf.toString();

	//	System.err.println("Tag found: " + s);

	if(s.endsWith("RDF")) {
	  // we found it!
	  setCache(s + c);
	  state = IN_RDF;
	  delimiter = "</" + s + ">";
	  delimiterPos = 0;
	  //	  System.err.println("Delimiter is: " + delimiter);
	  // go ahead and return the first character
	  return '<';
	}
      }
    }

    if(!done && state == IN_RDF) {

      char c = readIt();

      //      if(delimiterPos > 0)
      //	System.err.print("[" + delimiterPos + ":" + delimiter.charAt(delimiterPos) + " " + c + "]");

      if(delimiterPos < delimiter.length())
	if(c == delimiter.charAt(delimiterPos))
	  delimiterPos++;
	else
	  delimiterPos = 0;

      if(delimiterPos == delimiter.length()) {
	// we're done! restart the process
	state = LT_EXPECTED;
      }
      
      return c;
    }

    // else done
    return -1;
  }

  char readIt() throws IOException {
    int c = in.read();
    if(c == -1)
      done = true;
    return (char)c;
  }

  /**
   * Tell whether this stream supports the mark() operation.
   */
  public boolean markSupported() {
    return false;
  }

  /**
   * Read characters into a portion of an array, one by one.
   *
   * @exception  IOException  If an I/O error occurs
   */
  public int read(char /*byte*/ cbuf[], int off, int len) throws IOException {
    
    int i = 0;
    while (len > 0) {
      int c = read();
      if(c >= 0)
	cbuf[off + i++] = (char)/*(byte)*/c;
      else
	return i > 0 ? i : -1;
      len--;
    }
    return i;
  }

  public static InputSource filter(InputSource old) {

    // clone old source
    InputSource src = new InputSource();
    src.setSystemId(old.getSystemId());
    src.setPublicId(old.getPublicId());
    src.setEncoding(old.getEncoding());
    
    InputStream inStream = old.getByteStream();
    Reader in = inStream != null ? new InputStreamReader(inStream) : old.getCharacterStream();

    if(!(in instanceof RDFReader)) {
      
      if(!in.markSupported())
	in = new BufferedReader(in);
      in = new RDFReader(in);
    }

    src.setCharacterStream(in);
    return src;
  }

  public static void main(String[] args) throws Exception {

    if(args.length == 0) {
      System.err.println("Usage: org.w3c.rdf.RDFReader <URI>");
      System.exit(1);
    }

    Reader in = new RDFReader(new BufferedReader(new InputStreamReader(new URL(args[0]).openStream())));

    int c;
    while ((c = in.read()) != -1)
      System.out.print((char)c);
  }
  
}

