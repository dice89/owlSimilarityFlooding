package com.interdataworking.mm.alg;

import java.util.*;

// TODO: use syllable matcher as in TEX

public class StringComponentIterator implements Iterator {

  String s;
  int p;
  int state = OTHER;

  static final int LC_LETTER = 1;
  static final int UC_LETTER = 2;
  static final int LETTER = LC_LETTER | UC_LETTER;
  static final int DIGIT = 4;
  static final int OTHER = 0;

  int whitespaceStart = 0;
  int whitespaceEnd = 0;

  public int getCharType(char c) {

    if(Character.isDigit(c))
      return DIGIT;
    else if(Character.isLetter(c)) {
      if(Character.isLowerCase(c))
	return LC_LETTER;
      else
	return UC_LETTER;
    } else
      return OTHER;
  }

  public StringComponentIterator(String s) {

    this.s = s;
    skip();
  }

  void skip() {

    whitespaceStart = p;

    while(p < s.length() && !Character.isLetter(s.charAt(p)))
      p++;

    whitespaceEnd = p;
  }

  public boolean hasNext() {

    return p < s.length();
  }

  public static String matchCase(String sample, String text) {

    StringBuffer res = new StringBuffer(text.length());
    appendMatchCase(res, sample, text);
    return res.toString();
  }

  public static void appendMatchCase(StringBuffer res, String sample, String text) {

    boolean upper = false;

    int len = Math.min(sample.length(), text.length());

    int i=0;

    while(i < len) {

      char c = sample.charAt(i);

      if(Character.isUpperCase(c)) {
	res.append(Character.toUpperCase(text.charAt(i)));
	upper = true;
      } else {
	res.append(Character.toLowerCase(text.charAt(i)));
	upper = false;
      }
      i++;
    }

    while(i < text.length()) {
      res.append(upper ? Character.toUpperCase(text.charAt(i)) : Character.toLowerCase(text.charAt(i)));
      i++;
    }
  }

  public int getPosition() {

    return p;
  }

  public String getWhitespace() {

    return whitespaceStart < s.length() ? s.substring(whitespaceStart, whitespaceEnd) : "";
  }

  public static String canonicalUL(String s) {

    StringBuffer r = new StringBuffer(s.length());

    char lastChar = (char)0;

    for(int i=0; i < s.length(); i++) {

      char c = s.charAt(i);

      if(Character.isLetter(c)) {

	if(Character.isLetter(lastChar))
	  r.append(Character.toLowerCase(c));
	else
	  r.append(Character.toUpperCase(c));

      }
      lastChar = c;
    }

    return r.toString();
  }

  // abcDef, AbcDef, Abc_Def, Abc-Def, Abc Def, DEofGB
  //
  // word break: 
  //
  // one or more lower, then upper
  // more than one upper, then lower
  //
  // whitespace: non-letter

  public Object next() {

    int i = p;

    int count = 0; // num of last seen case
    int lastType = OTHER;

    boolean stop = false;

    while(p < s.length() && !stop) {

      stop = false;

      char c = s.charAt(p);
      int type = getCharType(c);

      switch(type) {

      case UC_LETTER:
	if(lastType == LC_LETTER)
	  stop = true;
	break;

      case LC_LETTER:
	if(lastType == UC_LETTER && count > 1) {
	  p--;
	  stop = true;
	}
	break;

      default:
	stop = true;
      }

      if(type == lastType)
	count++;
      else {
	count = 1;
      }

      lastType = type;

      if(!stop)
	p++;
    }

    String res = s.substring(i, p);
    skip();
    return res;
  }

  public void remove() {
  }

  public static void main(String[] args) throws Exception {

    Iterator it = new StringComponentIterator(args[0]);
    while(it.hasNext()) {
      String c = (String)it.next();
      System.err.println("Next component: " + c);
    }
  }
}
