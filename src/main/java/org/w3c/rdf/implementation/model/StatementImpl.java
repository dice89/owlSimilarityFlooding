/**
 * Copyright © World Wide Web Consortium, (Massachusetts Institute of
 * Technology, Institut National de Recherche en Informatique et en
 * Automatique, Keio University).
 *
 * All Rights Reserved.
 *
 * @author	Sergey Melnik <melnik@db.stanford.edu>
 */

package org.w3c.rdf.implementation.model;

import org.w3c.rdf.model.*;
import org.w3c.rdf.util.*;
import org.w3c.rdf.digest.*;
import java.io.*;

public final class StatementImpl implements Statement {

  Resource	pred;
  Resource	subj;
  RDFNode	obj;

  /**
   * The parameters to constructor are instances of classes
   * and not just strings
   */
  public StatementImpl (Resource subj, Resource pred, RDFNode obj) {
    this.pred = pred;
    this.subj = subj;
    this.obj = obj;
  }

  public Resource predicate () {
    return pred;
  }

  public Resource subject () {
    return subj;
  }

  public RDFNode object () {
    return obj;
  }

  /**
   * @return four most significant bytes of the digest
   */

  public int hashCode()
  {
    return RDFUtil.statementHashCode(subj, pred, obj);
  }

  public String getURI() throws ModelException {

    try {
      return RDFDigestUtil.statementDigestToURI(RDFDigestUtil.getStatementDigest(this));
    } catch (DigestException exc) {
      throw new ModelException(exc.toString());
    }
  }

  public String getLabel() throws ModelException {
    return getURI();
  }

  public String getNamespace() {

    return null;
  }

  public String getLocalName() throws ModelException {

    return getURI();
  }

  public String node2string(RDFNode n) {

    try {
      if(n instanceof Literal)
	return "literal(\"" + n.getLabel() + "\")";
      else if(n instanceof Statement)
	return n.toString();
      else // resource
	return "\"" + n.getLabel() + "\"";
    } catch (ModelException exc) {
      return "<EXC: " + exc + ">";
    }
  }

  public String toString() {
    
    return "triple(" + node2string(subj) + ", " + node2string(pred) + ", " + node2string(obj) + ")";
  }


  public boolean equals (Object that) {
    
    if (this == that) {
      return true;
    }
    if (that == null || !(that instanceof Statement)) {
      return false;
    }

    Statement t = (Statement)that;
    
    try {
      return
	subj.equals(t.subject()) &&
	pred.equals(t.predicate()) &&
	obj.equals(t.object());
    } catch (ModelException ex) {
      return false;
    }
  }

  public static void main(String[] args) throws Exception {

    if(args.length == 0) {
      System.err.println("StatementImpl <subject> <predicate> <object> [literal]");
      System.exit(1);
    }

    RDFNode object;
    if(args.length > 3)
      object =  new LiteralImpl(args[2]);
    else
      object = new ResourceImpl(args[2]);

    Statement t = new StatementImpl(new ResourceImpl(args[0]),
			      new ResourceImpl(args[1]),
			      object);

    System.err.println("Statement URI is: " + t.getURI() + ", hashCode: " + t.hashCode());
  }
}
