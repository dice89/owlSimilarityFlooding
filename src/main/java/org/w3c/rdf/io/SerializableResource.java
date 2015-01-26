package org.w3c.rdf.io;

import org.w3c.rdf.model.*;
import java.io.*;

/**
 * This class is NOT used by SerializableModel.
 */
public class SerializableResource implements Serializable, Resource {

  static NodeFactory defaultNodeFactory = edu.stanford.db.rdf.model.i.Registry.getDefaultRegistry();
  static boolean directResolve = true;

  Resource r;

  String s_namespace;
  String s_localName;

  NodeFactory f;

  public SerializableResource(Resource r) {

    this.r = r;
  }

  public static void setDefaultNodeFactory(NodeFactory f) {

    defaultNodeFactory = f;
  }

  public void setNodeFactory(NodeFactory f) {

    this.f = f;
  }

  public NodeFactory getNodeFactory() {

    return f;
  }

  public static void setDirectResolve(boolean d) {

    directResolve = d;
  }

  protected Object readResolve() throws ObjectStreamException {

    if(directResolve) {
      try {
	return getInternalResource();
      } catch (Exception any) {
	//	throw new ObjectStreamException("SerializableResource: could not resolve object. Reason: " + any);
      }
    }
    return this;
  }

  private void writeObject(java.io.ObjectOutputStream out) throws IOException {

    try {

      out.writeObject(r.getNamespace());
      out.writeObject(r.getLocalName());

    } catch (Exception any) {

      throw new IOException("SerializableResource: could not write object. Reason: " + any);
    }
  }

  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
    
    try {
      s_namespace = (String)in.readObject();
      s_localName = (String)in.readObject();
      f = defaultNodeFactory;

    } catch (Exception any) {

      throw new IOException("SerializableResource: could not read object. Reason: " + any);
    }
  }

  public Resource getInternalResource() throws ModelException {

    return getInternalResource(f);
  }

  public Resource getInternalResource(NodeFactory f) throws ModelException {

    if(r != null)
      return r;

    synchronized(this) {

      r = f.createResource(s_namespace, s_localName);
      // clean up
      s_namespace = s_localName = null;
    }

    return r;
  }

  public String getURI() throws ModelException {

    getInternalResource();
    return r.getURI();
  }

  public String getNamespace() throws ModelException {

    getInternalResource();
    return r.getNamespace();
  }

  public String getLocalName() throws ModelException {

    getInternalResource();
    return r.getLocalName();
  }

  public String getLabel() throws ModelException {

    getInternalResource();
    return r.getLabel();
  }

}
