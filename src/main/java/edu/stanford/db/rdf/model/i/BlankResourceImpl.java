package edu.stanford.db.rdf.model.i;

import org.w3c.rdf.model.*;
import org.w3c.rdf.util.RDFUtil;
import edu.stanford.db.xml.util.QName;

import org.w3c.rdf.io.*;
import java.io.*;

public class BlankResourceImpl extends ResourceImpl {

  int uniqueID;

  /**
   * Used in Registry to create blank nodes
   */
  protected BlankResourceImpl(Registry registry, int uniqueID) {

    this.payload = registry;
    this.uniqueID = uniqueID;
  }

  protected QName getQName() {

    if(payload instanceof Registry) {

      //      System.err.println("BEFORE ASSIGNING NAME: " + hashCode() + ", " + getNamespace() + "," + getLocalName());

      Registry r = (Registry)payload;
      payload = r.assignUniqueQName(this);

      //      System.err.println("AFTER ASSIGNING NAME: " + hashCode() + ", " + getNamespace() + "," + getLocalName());
    }
    return (QName)payload;
  }

  public String getLocalName() {

    if(payload instanceof Registry)
      return ((Registry)payload).create(String.valueOf(uniqueID));
    else
      return getQName().getLocalName();
  }

  public String getNamespace() {

    if(payload instanceof Registry)
      return ((Registry)payload).getBlankNS();
    else
      return getQName().getNamespace();
  }

  public int hashCode() {

    if(hash == 0) {

      if(payload instanceof Registry) {
	hash = ((Registry)payload).getHashCodeOfBlankResource(uniqueID);
      } else
	hash = super.hashCode(); // will be set there anyway...
    }
    return hash;
  }

  public boolean equals(Object that) {

    //    System.err.println("Blank: comparing " + this + " to " + that);
    
    if(this == that)
      return true;
    
    if(that instanceof BlankResourceImpl)
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

}
