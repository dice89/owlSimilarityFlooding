package org.w3c.rdf.digest;

import org.w3c.rdf.model.*;
import java.security.*;
import java.util.*;

/**
 * An implementation of the algorithms for computing digests of triples and models.
 */

public class RDFDigestUtil {

  //private static String algorithm = Digest.MD5;
  private static String algorithm = Digest.SHA1;

  public static String getDigestAlgorithm() {
    return algorithm;
  }
  
  public static void setDigestAlgorithm(String alg) {
    algorithm = alg;
  }

  public static String modelDigestToURI(Digest d) throws DigestException {

    return "urn:rdf:" + algorithm + "-" + DigestUtil.toHexString(d);
  }

  public static String statementDigestToURI(Digest d) throws DigestException {

    return "urn:rdf:" + algorithm + "-" + DigestUtil.toHexString(d);
  }

  /**
   * @returns the digest of an RDF node
   */
  public static Digest computeNodeDigest(RDFNode n) throws ModelException {
    
    try {
      return DigestUtil.computeDigest(algorithm, n.getLabel());
    } catch (NoSuchAlgorithmException ex) {
      throw new RuntimeException("RDFDigestUtil: no implementation for " + algorithm + " on your Java plattform");
    }
  }

  /**
   * @returns the standard digest of an RDF triple
   */
  public static Digest computeStatementDigest(Statement t)  throws DigestException, ModelException {

    byte[] s = getNodeDigest(t.subject()).getDigestBytes();
    byte[] p = getNodeDigest(t.predicate()).getDigestBytes();
    byte[] o = getNodeDigest(t.object()).getDigestBytes();

		int l = s.length;
		byte[] b = new byte[l * 3];
		System.arraycopy(s, 0, b, 0, l);
		System.arraycopy(p, 0, b, l, l);

		if(t.object() instanceof Resource)
			System.arraycopy(o, 0, b, l*2, l);
		else { // rotate by one byte
			for(int i=0; i < l; i++)
				b[l * 2 + ( (i+1) % l )] = o[i];
		}

    try {
      return DigestUtil.computeDigest(algorithm, b);
    } catch (NoSuchAlgorithmException ex) {
      throw new RuntimeException("RDFDigestUtil: no implementation for " + algorithm + " on your Java plattform");
    }

  }

  /**
   * @returns the standard digest of an model
   */
  public static Digest computeModelDigest(Model m) throws DigestException, ModelException {

    Enumeration en = m.elements();

    if(!en.hasMoreElements())
      return null;

    MessageDigest md = null;
    try {
     md = MessageDigest.getInstance(algorithm);
    } catch (NoSuchAlgorithmException ex) {
      throw new RuntimeException("RDFDigestUtil: no implementation for " + algorithm + " on your Java plattform");
    }

    // put all digests into an array and sort it
    Digest[] da = new Digest[m.size()];

    int l = 0;
    while(en.hasMoreElements()) {

      Statement t = (Statement)en.nextElement();
      Digest d = getStatementDigest(t);
      da[l++] = d;
    }

    // sort the array of digests
    Arrays.sort(da, new Comparator() {

	public int compare(Object o1, Object o2) {

	  try {
	    byte[] d1 = ((Digest)o1).getDigestBytes();
	    byte[] d2 = ((Digest)o2).getDigestBytes();
	    
	    // just to be sure
	    if(d1.length != d2.length)
	      return d1.length - d2.length;
	    
	    for(int i = 0; i < d1.length; i++)
	      if(d1[i] != d2[i])
		return d1[i] - d2[i];

	  } catch(DigestException any) {
	  }
	  
	  return 0;
	}
      });

    for(int i=0; i < l; i++)
      md.update(da[i].getDigestBytes());

    try {
      return DigestUtil.createFromBytes(algorithm, md.digest());
    } catch (DigestException de) { // cannot happen
      throw new InternalError(de.toString());
    }

    /*
    byte[] digest = null;

    while(en.hasMoreElements()) {

      Statement t = (Statement)en.nextElement();
      Digest d = getStatementDigest(t);
      if(digest == null)
	digest = d.getDigestBytes();
      else
	DigestUtil.xor(digest, d.getDigestBytes());

    }

    */
  }

  public static Digest getNodeDigest(RDFNode n) throws DigestException, ModelException {

    return n instanceof Digestable ? ((Digestable)n).getDigest() : computeNodeDigest(n);
  }

  public static Digest getStatementDigest(Statement t)  throws DigestException, ModelException {

    return t instanceof Digestable ? ((Digestable)t).getDigest() : computeStatementDigest(t);
  }

  public static Digest getModelDigest(Model m) throws DigestException, ModelException {

    return m instanceof Digestable ? ((Digestable)m).getDigest() : computeModelDigest(m);
  }


}

