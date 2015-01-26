package org.w3c.rdf.digest;

import java.security.*;
import java.io.*;
import java.math.BigInteger;

/**
 * This class is a container of static functions for manipulating cryptographic digests.
 *
 * @see org.w3c.rdf.digest.Digestable
 * @see org.w3c.rdf.digest.Digest
 */

public class DigestUtil {

  public static Digest computeDigest(String alg, String str) throws NoSuchAlgorithmException {

    try {
      byte[] b = str.getBytes("UTF8");
      return computeDigest(alg, b);
    } catch (UnsupportedEncodingException exc) {
      throw new RuntimeException("DigestImpl: weird internal error: UTF-8 is not supported");
    }
  }

  public static Digest computeDigest(String alg, byte[] b) throws NoSuchAlgorithmException {

    MessageDigest md = MessageDigest.getInstance(alg);
    byte[] digest = md.digest(b);
    try {
      return createFromBytes(alg, digest);
    } catch (DigestException de) {
      // cannot happen
      throw new RuntimeException("Bogus implementation of digest algorithm " + alg + ": " + de.getMessage());
    }
  }

  public static Digest createFromBytes(String alg, byte[] digest) throws DigestException {

    if(Digest.MD5.equals(alg)) {
      if(digest.length != 16)
				throw new DigestException("MD5 digest must be 16 bytes long");
      return new MD5Digest(digest);
    }

    if(Digest.SHA1.equals(alg)) {
      if(digest.length != 20)
				throw new DigestException("SHA-1 digest must be 20 bytes long");
      return new SHA1Digest(digest);
    }

    return new GenericDigest(alg, digest);
  }

  public static int getHashCode(Digest d) throws DigestException {

    return digestBytes2HashCode(d.getDigestBytes());
  }

  public static int digestBytes2HashCode(byte[] digest) {
    
    return
			((int) digest[0] & 0xff) |
      (((int) digest[1] & 0xff) << 8) |
      (((int) digest[2] & 0xff) << 16) |
      (((int) digest[3] & 0xff) << 24);
  }

  public static void xor(byte[] d1, byte[] d2) {
    
    int l = d1.length;
    for(int i = 0; i < l; i++)
      d1[i] ^= d2[i];
  }

  public static void xor(byte[] d1, byte[] d2, int shift) {
    
    int l = d1.length;
    for(int i = 0; i < l; i++)
      d1[(i + shift) % l] ^= d2[i];
  }
	
	/*

	// The circular left shift by N bits
  public static byte[] leftShift(byte[] src, int n) {

    int l = src.length;

		// make n positive
		while(n < 0) {
			n += l*8;
		}

		byte[] dest = new byte[src.length];

		int nBytes = n/8;
		int nBits = n%8;

		if (nBits == 0) {
			for (int i=0; i < l; i++)
				dest[(i + nBytes) % l] = src[i];
		} else {
			for (int i = 1; i <= l; i++) { // starts with 1 to make sure i-1 is non-negative
				dest[(i + nBytes) % l] = (byte) ((src[i % l] << nBits) | ((src[i-1]&0xff) >> (8-nBits)));
			}
		}

		return dest;
  }

  public static byte[] add(byte[] d1, byte[] d2) {
    
		byte[] r = new BigInteger(d1).add(new BigInteger(d2)).toByteArray();
		if(r.length > d1.length) {
			byte[] c = new byte[d1.length];
			System.arraycopy(r, 0, c, 0, c.length);
			return c;
		} else
  		return r;
  }

	public static byte[] sha_f_1(byte[] B, byte[] C, byte[] D) {

		byte[] F = new byte[B.length];

		for(int i=0; i < B.length; i++) {
			F[i] = (byte) ((B[i] & C[i]) | ((~B[i]) & D[i]));
		}
		return F;
	}

	public static byte[] sha_f_2(byte[] B, byte[] C, byte[] D) {

		byte[] F = new byte[B.length];

		for(int i=0; i < B.length; i++) {
			F[i] = (byte) (B[i] ^ C[i] ^ D[i]);
		}
		return F;
	}

	public static byte[] sha_f_3(byte[] B, byte[] C, byte[] D) {

		byte[] F = new byte[B.length];

		for(int i=0; i < B.length; i++) {
			F[i] = (byte) ((B[i] & C[i]) | (B[i] & D[i]) | (C[i] & D[i]));
		}
		return F;
	}

  public static void and(byte[] d1, byte[] d2) {
    
    int l = d1.length;
    for(int i = 0; i < l; i++)
      d1[i] &= d2[i];
  }

  public static void or(byte[] d1, byte[] d2) {
    
    int l = d1.length;
    for(int i = 0; i < l; i++)
      d1[i] |= d2[i];
  }
	
  public static void not(byte[] d1) {
    
    int l = d1.length;
    for(int i = 0; i < l; i++)
      d1[i] = (byte)~d1[i];
  }
	*/

  public static boolean equal(Digest d1, Digest d2) {

    if(d1 == d2)
      return true;

    try {
      if(d1 instanceof AbstractDigest && d2 instanceof AbstractDigest)
      	return equal(((AbstractDigest)d1).digest, ((AbstractDigest)d2).digest);
      else // general case
	if(d1.getDigestAlgorithm().equals(d2.getDigestAlgorithm()))
	  return equal(d1.getDigestBytes(), d2.getDigestBytes());
    } catch (DigestException ex) {}
    return false;
  }

  /**
   * Tests the equality of digests
   */
  public static boolean equal(byte[] d1, byte[] d2) {

    // just to be sure
    if(d1.length != d2.length)
      return false;

    for(int i = 0; i < d1.length; i++)
      if(d1[i] != d2[i])
	return false;

    return true;
  }

  /**
   * @returns hexadecimal representation of the Digest
   */  
  public static String toHexString (Digest d) throws DigestException {
    return toHexString(d.getDigestBytes());
  }

  /**
   * @returns hexadecimal representation of the byte array
   */  
  public static String toHexString (byte buf[]) {

    StringBuffer sb = new StringBuffer(2*buf.length) ;
    for (int i = 0 ; i < buf.length; i++) {
      int h = (buf[i] & 0xf0) >> 4 ;
      int l = (buf[i] & 0x0f) ;
      sb.append ((char)((h>9) ? 'a'+h-10 : '0'+h)) ;
      sb.append ((char)((l>9) ? 'a'+l-10 : '0'+l)) ;
    }

    return sb.toString() ;
  }

	/*
	public static void main(String args[]) {

		BigInteger b = new BigInteger("20");
		byte[] d = b.toByteArray();
		byte[] d2 = leftShift(d, 7);
		d2 = leftShift(d2, 3);
		System.out.println("Result 1: " + new BigInteger(d2));
		byte[] d3 = leftShift(d2, -10);
		System.out.println("Result 1: " + new BigInteger(d3));
	}
	*/
}

// AUXILIARY CLASSES

abstract class AbstractDigest implements Digest {

	byte[] digest;

	protected AbstractDigest(byte[] b) {

		digest = new byte[b.length];
		System.arraycopy(b, 0, digest, 0, b.length);
	}

	public byte[] getDigestBytes() {

	  //return digest;
	  byte[] result = new byte[digest.length];
	  System.arraycopy(digest, 0, result, 0, digest.length);
	  return result;
	}

	public int hashCode() {

		return DigestUtil.digestBytes2HashCode(digest);
	}

	public boolean equals (Object that) {

		if(!(that instanceof Digest))
			return false;

		return DigestUtil.equal(this, (Digest)that);
	}

	public String toString() {
		return DigestUtil.toHexString(digest);
	}
}

class GenericDigest extends AbstractDigest {

	String algorithm;

	protected GenericDigest(String alg, byte[] b) {
		super(b);
		algorithm = alg;
	}

	public String getDigestAlgorithm() {
		return algorithm;
	}
}

// Specific digests: save bytes for algorithm type

final class SHA1Digest extends AbstractDigest {

	SHA1Digest(byte[] b) {
		super(b);
	}

	public String getDigestAlgorithm() {
		return SHA1;
	}
}

final class MD5Digest extends AbstractDigest {

	protected MD5Digest(byte[] b) {
		super(b);
	}

	public String getDigestAlgorithm() {
		return MD5;
	}
}



