package edu.stanford.db.rdf.model.i;

import org.w3c.rdf.vocabulary.rdf_syntax_19990222.RDF;
import org.w3c.rdf.model.*;
import org.w3c.rdf.util.*;
import java.util.*;
import java.lang.ref.*;
import edu.stanford.db.xml.util.*;
//import com.lastmileservices.rdf.util.Cache;
import edu.stanford.db.rdf.util.Cache;
import org.w3c.rdf.implementation.model.NodeFactoryImpl;

/**
 * Registry keeps exactly one copy of each resource in memory.
 * Blank resources are not cached and may have duplicate non-blank versions.
 */

public class Registry implements NodeFactory /*, Runnable*/ {

  // Resource -> Resource
  Cache rmap = new Cache(0, false);
  // String -> Literal
  Cache lmap = new Cache(0, false);
  // String -> String
  Cache smap = new Cache(0, false);

  int lastNodeID;
  String uniqueNS;
  int uniqueNSIncrementalHashCode;

  //  boolean dense;

  static Registry defaultRegistry;

  public Registry() {
    this(false);
  }

  public Registry(boolean hasInverse) {

    // Resource -> Resource
    this.rmap = new Cache(0, hasInverse);
    // String -> Literal
    this.lmap = new Cache(0, hasInverse);
    // String -> String
    this.smap = new Cache(0, false); // String is not LongIDAware!
  }

  public RDFNode get(long id) {

    Object r = rmap.get(id);
    if(r == null)
      r = lmap.get(id);
    if(r instanceof RDFNode)
      return (RDFNode)r;
    else 
      return null;
  }


  public static void setDefaultRegistry(Registry r) {

    defaultRegistry = r;
    //    System.err.println("Setting default registry to " + r);
  }

  public static Registry getDefaultRegistry() {

    if(defaultRegistry == null)
      defaultRegistry = new Registry();
    //    System.err.println("Returning default registry: " + defaultRegistry);
    return defaultRegistry;
  }

  public String toString() {

    return "[Registry: " + System.identityHashCode(this) + ", rmap=" + rmap + ", lmap=" + lmap + "]";
  }

  public void setUniquePrefix(String str) throws ModelException {

    if(uniqueNS != null)
      throw new ModelException("UniqueNS has already been initialized!");
    uniqueNS = str;
    //    System.err.println("Set unique to " + str + " in " + this);
  }

  /*
  public synchronized setDense(boolean b) {

    this.dense = b;
  }
  */

  // synchronized not needed since uniqueness of a string instance is not critical
  public String create(String str) {

    if(str == null)
      return null;

    String s = (String)smap.get(str);
    if(s == null) {
      s = str;
      smap.put(s, s);
    }
    return s;
  }

  public Resource createResource(String str) throws ModelException  {

    str = create(str);
    return createResource(new QName(str));
  }

  public Resource createResource(String ns, String str) throws ModelException  {

    ns = create(ns);
    str = create(str);
    return createResource(new QName(ns, str));
  }

  /**
   * Creates a resource out of a string
   */
  public synchronized Resource createResource(QName str) throws ModelException {

    // check whether we have it in the registry
    Resource r = (Resource)rmap.get(str);
    //    System.err.println("Creating resource: " + str + ", in rmap=" + r);

    if(r == null) {
      r = new ResourceImpl(/*getUnusedNodeID(),*/ str);
      rmap.put(str, r);
    }
    return r;
  }

  /**
   * Creates a literal out of a string
   */
  public synchronized Literal createLiteral(String str) {

    // check whether we have it in the registry
    Literal r = (Literal)lmap.get(str);
    if(r == null) {
      r = new LiteralImpl(/*getUnusedNodeID(),*/ str);
      lmap.put(str, r);
    }
    return r;
  }

  public int getUnusedNodeID() {

    return lastNodeID++;
  }

  /**
   * Creates a triple
   */
  public Statement createStatement(Resource subject, Resource predicate, RDFNode object) throws ModelException {
		
    // remap resources if necessary

    /*

    //    System.err.println("Subject: " + subject);
    if(subject instanceof Statement) {
      if(!(subject instanceof StatementImpl)) {
	Statement st = (Statement)subject;
	subject = createStatement(st.subject(), st.predicate(), st.object());
      }
    } else if(!(subject instanceof ResourceImpl))
      subject = createResource(subject.getNamespace(), subject.getLocalName());

    if(!(predicate instanceof ResourceImpl))
      predicate = createResource(predicate.getNamespace(), predicate.getLocalName());

    if(object instanceof Statement) {
      if(!(object instanceof StatementImpl)) {
	Statement st = (Statement)object;
	object = createStatement(st.subject(), st.predicate(), st.object());
      }
    } else if(object instanceof Resource) {
      if(!(object instanceof ResourceImpl)) {
	Resource r = (Resource)object;
	object = createResource(r.getNamespace(), r.getLocalName());
      }
    } else if(object instanceof Literal) {
      if(!(object instanceof LiteralImpl))
	object = createLiteral(object.getLabel());
    }

    */

    return (subject != null && predicate != null && object != null) ?
      new StatementImpl(subject, predicate, object) : null;
  }

  public synchronized void ensureCanCreateUnique() throws ModelException {

    if(uniqueNS == null) {
      uniqueNS = new NodeFactoryImpl().createUniqueResource().getLabel() + "-";
      uniqueNS = create(uniqueNS);
      uniqueNSIncrementalHashCode = RDFUtil.incrementalHashCode(uniqueNS);
    }
  }

  public int getHashCodeOfBlankResource(int uniqueID) {

    return RDFUtil.incrementalHashCode( uniqueNSIncrementalHashCode,
					Integer.toString(uniqueID) );
  }

  /**
   * Creates a resource with a unique ID
   */
  public synchronized Resource createUniqueResource() throws ModelException {

    ensureCanCreateUnique();

    int id = getUnusedNodeID();
    BlankResourceImpl r = new BlankResourceImpl(this, id);
    return r;
  }

  /**
   * Creates a unique QName.
   * This method is called by BlankResourceImpl, which saves the assigned QName locally.
   *
   * ensureCanCreateUnique() must have been called in createUniqueResource already
   */
  protected synchronized QName assignUniqueQName(BlankResourceImpl node) {

    try {
      if(uniqueNS == null) {
	uniqueNS = new NodeFactoryImpl().createUniqueResource().getLabel() + "-";
      uniqueNS = create(uniqueNS);
      }
      int id = node.uniqueID; // getUnusedNodeID();
      String idStr = create(String.valueOf(id));
      QName qname = new QName(uniqueNS, idStr);

      rmap.put(qname, node);

//       System.err.println("=== ASSIGNING UNIQUE NAME: " + qname);
//       if(true)
//  	throw new RuntimeException();

      return qname;

    } catch(ModelException unlikely) {
      return null;
    }
  }

  /**
   * Called by BlankResource. uniqueNS is always set by that time.
   */
  protected String getBlankNS() {

    return uniqueNS;
  }

  /**
   * Creates an ordinal property (rdf:li, rdf:_N)
   */
  public Resource createOrdinal(int i)  throws ModelException {
    
    if(i < 1)
      throw new IllegalArgumentException("Attempt to construct invalid ordinal resource");
    else
      return createResource(RDF._Namespace, "_" + i);
  }

  /**
   * Creates a literal from a boolean
   */
  public Literal createLiteral(boolean b) throws ModelException {

    return createLiteral(String.valueOf(b));
  }

  /**
   * Creates a literal from a byte
   */
  public Literal createLiteral(byte b) throws ModelException {

    return createLiteral(String.valueOf(b));
  }


  /**
   * Creates a literal from a char
   */
  public Literal createLiteral(char b) throws ModelException {

    return createLiteral(String.valueOf(b));
  }


  /**
   * Creates a literal from a short
   */
  public Literal createLiteral(short b) throws ModelException {

    return createLiteral(String.valueOf(b));
  }


  /**
   * Creates a literal from an int
   */
  public Literal createLiteral(int b) throws ModelException {

    return createLiteral(String.valueOf(b));
  }


  /**
   * Creates a literal from a long
   */
  public Literal createLiteral(long b) throws ModelException{

    return createLiteral(String.valueOf(b));
  }


  /**
   * Creates a literal from a float
   */
  public Literal createLiteral(float b) throws ModelException{

    return createLiteral(String.valueOf(b));
  }


  /**
   * Creates a literal from a double
   */
  public Literal createLiteral(double b) throws ModelException{

    return createLiteral(String.valueOf(b));
  }

}
