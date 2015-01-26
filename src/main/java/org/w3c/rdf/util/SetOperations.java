package org.w3c.rdf.util;

import org.w3c.rdf.model.*;
import java.util.*;

/**
 * An implementation of set operations on models.
 * Every of these static methods first attempts to use the native implementation provided by the model if any.
 */

public class SetOperations {

  public static boolean equals(Model m1, Model m2) throws ModelException {

    return contains(m1, m2) && contains(m2, m1);
  }

  public static boolean contains(Model m1, Model m2) throws ModelException {

    synchronized(m2) {

      for(Enumeration en = m2.elements(); en.hasMoreElements();)
	if(!m1.contains( (Statement)en.nextElement() ))
	  return false;
    }
    return true;
  }

  public static void copy(Model src, Model dest) throws ModelException {

    subtract(dest, dest);
    unite(dest, src);
  }


  public static Model union(Model m1, Model m2) throws ModelException {

    Model res = m1.duplicate();
    unite(res, m2);
    return res;
  }

  public static void unite(Model m1, Model m2) throws ModelException {

    if(m1 instanceof SetModel)
      ((SetModel)m1).unite(m2);
    else {
      synchronized(m2) {

	for(Enumeration en = m2.elements(); en.hasMoreElements();)
	  m1.add( (Statement)en.nextElement() );
      }
    }
  }

  public static Model difference(Model m1, Model m2) throws ModelException {

    Model res = m1.duplicate();
    subtract(res, m2);
    return res;
  }

  public static void subtract(Model m1, Model m2) throws ModelException {

    if(m1 instanceof SetModel)
      ((SetModel)m1).subtract(m2);
    else {
      synchronized(m2) {

	for(Enumeration en = m2.elements(); en.hasMoreElements();)
	  m1.remove( (Statement)en.nextElement() );
      }
    }
  }

  public static Model intersection(Model m1, Model m2) throws ModelException {

    Model res = m1.duplicate();
    intersect(res, m2);
    return res;
  }

  public static void intersect(Model m1, Model m2) throws ModelException {

    if(m1 instanceof SetModel)
      ((SetModel)m1).intersect(m2);
    else {
      synchronized(m2) {

	Model tmp = m1.duplicate();

	for(Enumeration en = tmp.elements(); en.hasMoreElements();) {
	  Statement t = (Statement)en.nextElement();
	  if(!m2.contains(t))
	    m1.remove(t);
	}
      }
    }
  }

}
