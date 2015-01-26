package edu.stanford.db.rdf.model.i;

import org.w3c.rdf.model.*;
import org.w3c.rdf.digest.*;
import org.w3c.rdf.util.*;

public class StatementImpl implements Statement {

  static boolean DEBUG = false;

  Resource s, p;
  RDFNode o;

  public StatementImpl(Resource s, Resource p, RDFNode o) throws ModelException {
    
    this.s = s;
    this.p = p;
    this.o = o;

    if(s == null || p == null || o == null)
      throw new ModelException("Cannot create statement for " + s + ", " + p + ", " + o);
  }

  protected StatementImpl() {
  }

  public Resource subject() {
    return s;
  }

  public Resource predicate() {
    return p;
  }

  public RDFNode object() {
    return o;
  }

  public String node2string(RDFNode n) {

    try {
      if(n instanceof Literal)
	return "literal(\"" + n.getLabel() + "\")";
      else if(n instanceof Statement)
	return n.toString();
      else if(n instanceof Resource)
	return "\"" + n.getLabel() + "\"";
      else
	return String.valueOf(n);
    } catch (ModelException exc) {
      return "<EXC: " + exc + ">";
    }
  }

  public String toString() {
    
    return "triple(" + node2string(s) + ", " + node2string(p) + ", " + node2string(o) + ")";
  }

  public String getNamespace() {

    return null;
  }

  public String getLocalName() throws ModelException {

    return getLabel();
  }

  public String getLabel() throws ModelException {
    try {
      return RDFDigestUtil.statementDigestToURI(RDFDigestUtil.computeStatementDigest(this));
    } catch (DigestException de) {
      throw new ModelException("Cannot compute statement URI: " + de);
    }
  }

  public String getURI() throws ModelException {
    return getLabel();
  }

  public int hashCode()
  {
    return RDFUtil.statementHashCode(s, p, o);
  }


  public boolean equals(Object that) {
    
//      if(this == that)
//        return true;

    if(DEBUG)
      System.err.println("Testing equality of " + this + " vs " + that);
    
    if (!(that instanceof Statement))
      return false;
    
    try {
      Statement st = (Statement)that;
      return (s.equals(st.subject()) &&
	      p.equals(st.predicate()) &&
	      o.equals(st.object()));
    } catch (ModelException any) {
      return false;
    }
  }
}
