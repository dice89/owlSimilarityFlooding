package org.w3c.rdf.digest;

/**
 * A cryptographic digest
 *
 * @see org.w3c.rdf.digest.Digestable
 * @see org.w3c.rdf.digest.DigestUtil
 */

public interface Digest {

  public String MD5 = "MD5";
  public String SHA1 = "SHA-1";

  public String getDigestAlgorithm() throws DigestException;
  //  public int getDigestLength();
  public byte[] getDigestBytes() throws DigestException;
}

