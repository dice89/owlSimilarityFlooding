package edu.stanford.db.rdf.model.i;

import org.w3c.rdf.model.*;
import org.w3c.rdf.util.*;
//import com.lastmileservices.rdf.util.*;
import edu.stanford.db.rdf.util.*;

public abstract class RDFNodeImpl implements RDFNode, LongIDAware {

  //  protected int nodeID;
  //  private int _hashCode;

  /*
  protected RDFNodeImpl(int nodeID) {

    this.nodeID = nodeID;
  }
  */

  protected RDFNodeImpl() {
  }

  public String	toString () {
    return getLabel();
  }

  public abstract String getLabel();

  public long getLongID() {

    //    return nodeID;
    return super.hashCode(); // of Object
  }

  public int hashCode() {

    return RDFUtil.incrementalHashCode(getLabel());
  }

  /** That's the beauty of it... */
  public boolean equals(Object that) 
  {
    return this == that;
  }
  
}
