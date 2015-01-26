package edu.stanford.db.xml.util;


public class QName {

  String namespace;
  String localName;
  
  public QName(String ns, String ln) {
    namespace = ns;
    localName = ln;
  }
  
  public QName(String ln) {
    namespace = null;
    localName = ln;
  }
  
  public int hashCode() {

    if(namespace == null)
      return incrementalHashCode(localName);
    else
      return incrementalHashCode(incrementalHashCode(namespace), localName);
  }

  public String getName() {

    return namespace == null ? localName : namespace + localName;
  }

  public String toString() {

    return "QNAME[" + (namespace == null ? "" : namespace + "," ) + localName + "]";
  }
  
  public String getNamespace() {

    return namespace;
  }

  public String getLocalName() {
    
    return localName;
  }

  public boolean equals (Object that) {
    
    if (this == that) {
      return true;
    }
    if (that == null) {
      return false;
    }

    if(that instanceof QName) {

      QName t = (QName)that;
      
      // resources are equal iff this.getURI() == that.getURI()
      // the case distinction below is for optimization only to avoid unnecessary string concatenation

      if(localName == t.localName && namespace == t.namespace)
	return true;

      return getName().equals(t.getName());
      /*
      
      boolean b;
      
      if(namespace == null) {
	if(t.getNamespace() == null)
	  b = localName.equals(t.getLocalName());
	else // maybe "that" did not detect names
	  b = localName.equals(t.getName());
      } else {
	if(t.getNamespace() != null)
	  b = localName.equals(t.getLocalName()) && namespace.equals(t.getNamespace());
	else // maybe "this" did not detect names
	  b = getName().equals(t.getName());
      }

      return b;
      */

    } else
      return getName().equals(String.valueOf(that));
  }


  public static int incrementalHashCode(int hash, char c) {

    return 31*hash + c;
  }

  public static int incrementalHashCode(int hash, String segment) {

    int len = segment.length();

    for (int i = 0; i < len; i++)
      hash = 31*hash + segment.charAt(i);

    return hash;
  }

  public static int incrementalHashCode(String segment) {

    return incrementalHashCode(0, segment);
  }
  
}
