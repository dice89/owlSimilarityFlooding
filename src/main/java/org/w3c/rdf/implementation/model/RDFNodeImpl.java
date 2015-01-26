
package org.w3c.rdf.implementation.model;

import org.w3c.rdf.model.*;
import org.w3c.rdf.util.*;

public abstract class RDFNodeImpl implements RDFNode {

//   int hash;

  public String	toString () {
    return getLabel();
  }

  public abstract String getLabel ();

  public int hashCode() {

    return RDFUtil.incrementalHashCode(getLabel());

//     if(hash == 0) {
//       hash = getLabel().hashCode();
//     }
//     return hash;
  }

  public boolean equals (Object that) {

    try {
      return getLabel().equals( ((RDFNode)that).getLabel() );
    } catch (ModelException ex) {
      return false;
    }
  }
}
