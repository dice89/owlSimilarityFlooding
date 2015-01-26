package edu.stanford.db.rdf.model.i;

import org.w3c.rdf.model.*;
import org.w3c.rdf.util.RDFUtil;
import edu.stanford.db.xml.util.QName;

import org.w3c.rdf.io.*;
import java.io.*;

public class ResourceImpl extends RDFNodeImpl implements Resource, Serializable {

  /**
   * Normally, payload is a QName.
   * For blank nodes, payload is initially a reference to Registry.
   * If getURI() is ever called on this object,
   * reference to Registry is replaced by a fixed QName.
   */
  protected Object payload;
  int hash;

  public ResourceImpl(/*int nodeID,*/ QName qname) throws ModelException {

    //    super(nodeID);
    if(qname.getLocalName() == null)
      throw new ModelException("Local name cannot be null");

    this.payload = qname;
  }

  /**
   * Used in FindIndex to create dummy node
   */
  protected ResourceImpl(String dummy) {

    this.payload = new QName(dummy, null);
  }

  protected ResourceImpl() {
  }

  /*
  public ResourceImpl(int nodeID, String namespace, String localName) {

    super(nodeID);
    this.namespace = namespace;
    this.localName = localName;
  }
  */

  protected QName getQName() {

    return (QName)payload;
  }

  public String getNamespace() {

    return getQName().getNamespace();
  }

  public String getLocalName() {

    return getQName().getLocalName();
  }

  public String getURI() {

    //    System.err.println("HERE");
    if(getQName() == null)
      return "<null>";
    else
      return (getNamespace() == null ? getLocalName() : getNamespace() + getLocalName());
  }

  public int hashCode() {

    if(hash == 0) {
      
      if(getQName() != null) {
	
	String namespace = getQName().getNamespace();
	String localName = getQName().getLocalName();
	
	if(namespace == null)
	  hash = RDFUtil.incrementalHashCode(localName);
	else
	  hash = RDFUtil.incrementalHashCode(RDFUtil.incrementalHashCode(namespace), localName);
	
      } else
	hash = -1; // no qname at all
    }
    return hash;
  }

  public String getLabel() {

    return getURI();
  }

  public boolean equals(Object that) {
    
    //    System.err.println("RImpl: comparing " + this + " to " + that);

    if(this == that)
      return true;
    
    if((that instanceof ResourceImpl) && !(that instanceof BlankResourceImpl))
      return false;
    
    if(that instanceof Statement)
      return false;

    if(!(that instanceof Resource))
      return false;

    try {
      return RDFUtil.equalResources(this, (Resource)that);
    } catch (ModelException any) {}

    return false;
  }

  Object writeReplace() throws ObjectStreamException {

    return new SerializableResource(this);
  }

}
