package org.w3c.rdf.digest;

/**
 * An object that can produce a digest for itself implements this interface.
 *
 * @see org.w3c.rdf.digest.Digestable
 * @see org.w3c.rdf.digest.DigestUtil
 */

public interface Digestable {

  /**
   * @return a Digest
   */
  public Digest getDigest() throws DigestException;
}

