package org.w3c.rdf.io;

import org.w3c.rdf.model.*;
import java.io.*;

/**
 * This class is NOT used by SerializableModel.
 */
public class SerializableLiteral implements Serializable, Literal {

  static NodeFactory defaultNodeFactory = edu.stanford.db.rdf.model.i.Registry.getDefaultRegistry();
  static boolean directResolve = true;

  Literal r;

  String s_label;

  NodeFactory f;

  public SerializableLiteral(Literal r) {

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
	return getInternalLiteral();
      } catch (Exception any) {
	//	throw new ObjectStreamException("SerializableLiteral: could not resolve object. Reason: " + any);
      }
    }
    return this;
  }

  private void writeObject(java.io.ObjectOutputStream out) throws IOException {

    try {

      out.writeObject(r.getLabel());

    } catch (Exception any) {

      throw new IOException("SerializableLiteral: could not write object. Reason: " + any);
    }
  }

  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
    
    try {
      s_label = (String)in.readObject();
      f = defaultNodeFactory;

    } catch (Exception any) {

      throw new IOException("SerializableLiteral: could not read object. Reason: " + any);
    }
  }

  public Literal getInternalLiteral() throws ModelException {

    return getInternalLiteral(f);
  }

  public Literal getInternalLiteral(NodeFactory f) throws ModelException {

    if(r != null)
      return r;

    synchronized(this) {

      r = f.createLiteral(s_label);
      // clean up
      s_label = null;
    }

    return r;
  }

  public String getLabel() throws ModelException {

    getInternalLiteral();
    return r.getLabel();
  }

}
