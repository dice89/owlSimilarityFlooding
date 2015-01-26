package org.w3c.rdf.implementation.model;

import java.security.*;
import java.util.*;

import org.w3c.rdf.model.*;
import org.w3c.rdf.digest.*;
import org.w3c.rdf.vocabulary.rdf_syntax_19990222.RDF;


public class NodeFactoryImpl implements NodeFactory {

  static final int RANDOM_SEED_LEN = 4; // bytes

  private static Random rnd;
  private static Object rndLock = new String();

  private static final int MAX_ORD = 30; // maximal number of cached ordinals
  private static Resource[] ords;
  static {
    ords = new Resource[MAX_ORD + 1]; // 0 is wasted
    for(int i=1; i <= MAX_ORD; i++)
      ords[i] = new ResourceImpl(RDF._Namespace, "_" + i);

    // run a new thread to initialize secure random

    Thread th = new Thread() {
	public void run() {
	  //	  System.err.println("STARTED GENERATING RANDOM");
	  // hope 8 bytes for seed are enough...
	  rnd = new SecureRandom(SecureRandom.getSeed(RANDOM_SEED_LEN));
	  //	  System.err.println("FINISHED GENERATING RANDOM");
	  synchronized(rndLock) {
	    rndLock.notify();
	  }
	}
      };
    th.setPriority(Thread.MIN_PRIORITY);
    th.start();
  }

  /**
   * Creates a resource from a URI
   */
  public Resource createResource(String str) {

    return str != null ? new ResourceImpl(str) : null;
  }

  /**
   * Creates a resource from namespace and local name
   */
  public Resource createResource(String namespace, String localName) {

    return localName != null ? new ResourceImpl(namespace, localName) : null;
  }

  /**
   * Creates a literal out of a string
   */
  public Literal createLiteral(String str) {

    return str != null ? new LiteralImpl(str) : null;
  }

  /**
   * Creates a triple
   */
  public Statement createStatement(Resource subject, Resource predicate, RDFNode object) {
		
    return (subject != null && predicate != null && object != null) ?
			new StatementImpl(subject, predicate, object) : null;
  }

	/**
	 * Creates a resource with a unique ID
	 */
  public Resource createUniqueResource() {

    if(rnd == null) {
      // wait for the generator thread to complete
      try {
	synchronized(rndLock) {
	  //	  System.err.println("WAITING ON LOCK");
	  rndLock.wait();
	  //	  System.err.println("LOCK RELEASED");
	}
      } catch (InterruptedException any) {}
      // rnd must be done by now
      //      rnd = new SecureRandom(SecureRandom.getSeed(RANDOM_SEED_LEN));
    }
    byte[] b = new byte[16];
    rnd.nextBytes(b);
    return new ResourceImpl("urn:rdf:" + DigestUtil.toHexString(b));
  }

  /**
   * Creates an ordinal property (rdf:li, rdf:_N)
   */
  public Resource createOrdinal(int i) {
    
    if(i < 1)
      throw new IllegalArgumentException("Attempt to construct invalid ordinal resource");
    if(i <= MAX_ORD)
      return ords[i];
    else
      return new ResourceImpl(RDF._Namespace + "_" + i);
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
