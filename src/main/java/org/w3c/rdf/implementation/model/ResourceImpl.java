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

import org.w3c.rdf.io.*;
import java.io.*;

public class ResourceImpl extends RDFNodeImpl implements Resource, Serializable {

  String namespace;
  String localName;

  protected ResourceImpl() {
  }

  public ResourceImpl (String uri) {

    this.namespace = null;
    this.localName = uri;
  }

  public ResourceImpl (String namespace, String localName) {

    this.namespace = namespace;
    this.localName = localName;
  }

  public String getURI() {

    return (namespace == null ? localName : namespace + localName);
  }

  public String getNamespace() {

    return namespace;
  }

  public String getLocalName() {

    return localName;
  }

  public String getLabel() {

    return getURI();
  }

  public int hashCode() {

    if(namespace == null)
      return RDFUtil.incrementalHashCode(localName);
    else
      return RDFUtil.incrementalHashCode(RDFUtil.incrementalHashCode(namespace), localName);
  }

  public boolean equals (Object that) {

    if (this == that) {
      return true;
    }
    if (that == null || !(that instanceof Resource) || (that instanceof Statement)) {
      return false;
    }

    /*
    Resource t = (Resource)that;

    // resources are equal iff this.getURI() == that.getURI()
    // the case distinction below is for optimization only to avoid unnecessary string concatenation
    
    try {
      if(namespace == null) {
	if(t.getNamespace() == null)
	  return localName.equals(t.getLocalName());
	else // maybe "that" did not detect names
	  return localName.equals(t.getURI());
      } else {
	if(t.getNamespace() != null)
	  return localName.equals(t.getLocalName()) && namespace.equals(t.getNamespace());
	else // maybe "this" did not detect names
	  return getURI().equals(t.getURI());
      }
    } catch (ModelException any) {
    }
    */

    try {
      return RDFUtil.equalResources(this, (Resource)that);
    } catch (ModelException any) {}

    return false;
  }

  Object writeReplace() throws ObjectStreamException {

    return new SerializableResource(this);
  }
}
