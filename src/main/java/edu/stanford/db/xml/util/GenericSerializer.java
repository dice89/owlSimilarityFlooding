package edu.stanford.db.xml.util;

import java.io.*;

public class GenericSerializer {

  public static final char ABB_LONG = (char)0;
  public static final char ABB_CDATA = (char)1;
  public static final char ANYQUOTE = (char)0;

  public static void writeEscaped(String str, Writer w) throws IOException {

    for(int i=0; i < str.length(); i++) {
      char c = str.charAt(i);
      if(c == '&')
	w.write("&amp;");
      else
	w.write(c);
    }
  }

  /**
   * ]]> cannot be there!
   * FIXME: how to we encode binary data? It is a function
   * of some more abstract layer?
   */
  public static void writeCDATA(String str, Writer w) throws IOException {

    int start = 0, i = 0;
    do {
      i = str.indexOf("]]>", start);
      if(i >= 0) {
	w.write(str.substring(start, i+2));
	w.write("]]><![CDATA[");
	start = i + 2;
      } else
	w.write(str.substring(start));
    } while (i >= 0 && start < str.length());
  }
  /**
   * < and &
   */
  public static void writeAttValue(String str, Writer w) throws IOException {

    for(int i=0; i < str.length(); i++) {
      char c = str.charAt(i);
      if(c == '<')
	w.write("&lt;");
      else if(c == '&')
	w.write("&amp;");
      else
	w.write(c);
    }
  }

  public static boolean isCDATA(String s) {

    return abbrevQuote(s) == ABB_CDATA;
  }

  public static char abbrevQuote( String s ) {

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

    if(hasBreaks /*|| s.length() > MAX_ABBLENGTH*/) // optically nice value
      return whiteSpaceOnly ? ABB_CDATA : ABB_LONG;

    return quote == ANYQUOTE ? '"' : quote;
  }
}

