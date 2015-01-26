package edu.stanford.db.xml.util;

import java.util.*;

public class Element {

  public static final String EMPTY_STR = "";
  public static final int EMPTY = 0;
  public static final int CDATA = 1;
  public static final int ELEMENTS = 2;
  public static final int MIXED = CDATA | ELEMENTS;

  public static final Enumeration EMPTY_ENUMERATION = new Enumeration() {

    public boolean hasMoreElements() {
      return false;
    }
    public Object nextElement() {
      throw new NoSuchElementException("ElementEnumerator");
    }
  };

  protected int type = EMPTY;
  protected Element parent;
  /**
   * we consider a single child at a time
   */
  protected Element child;
  protected QName name;
  protected int numInParent;
  /**
   * If element is CDATA
   */
  protected String value = EMPTY_STR;
  protected Map attrs;

  public Element() {
  }

//   public void setName(String name) {
//     this.name = new QName(name);
//   }

  public void setName(QName name) {
    this.name = name;
  }

  public QName getName() {
    return name;
  }

  public void setNumInParent(int n) {
    this.numInParent = n;
  }

  public int getNumInParent() {
    return numInParent;
  }

  public int getType() {
    return type;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
    type |= CDATA;
  }

  public boolean hasValue() {

    return (type & CDATA) != 0;
  }

  public Element getParent() {
    return parent;
  }

  public void setParent(Element parent) {
    this.parent = parent;
  }

  public void setChild(Element child) {
    this.child = child;
    type |= ELEMENTS;
  }

  public Element getChild() {
    return child;
  }

  /*
  public Enumeration getProperAttributes() {

    if(attrs == null)
      return EMPTY_ENUMERATION;
    return new ProperEnumeration(attrs);
  }
  */

  public Map getAttributes() {

    if(attrs == null)
      attrs = new HashMap();

    return attrs;
  }

//   public Enumeration getAttributes() {

//     if(attrs == null)
//       return EMPTY_ENUMERATION;
//     else
//       return attrs.keys();
//   }

  public void setAttribute(QName name, String value) {

    Map at = getAttributes();
    at.put(name, value);
  }

  public String getAttribute(QName name) {

    if(attrs == null)
      return null;
    else
      return (String)attrs.get(name);
  }


  /**
   * returns attribute names that are not xmlns...
  class ProperEnumeration {

    Object current = null;

    public ProperEnumeration(Enumeration en) {
      this.en = en;
    }

    public boolean hasMoreElements() {
      return findNext() != null;
    }

    Object findNext() {

      while(current == null && en.hasMoreElements()) {

	String aName = (String)en.nextElement();
	if(!aName.startsWith("xmlns"))
	  current = aName;
      }
      return current;
    }

    public Object nextElement() {

      Object res = findNext();
      current = null;
      return res;
    }
  }
   */

}

