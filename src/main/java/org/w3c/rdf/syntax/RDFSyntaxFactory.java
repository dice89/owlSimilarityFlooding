package org.w3c.rdf.syntax;

public interface RDFSyntaxFactory {

  String RDF_XML_19990222 = "RDF/XML-19990222";

  /**
   * Creates a new RDF parser for a given format.
   * Returns <tt>null</tt> if formatID is unknown.
   */
  public RDFParser createParser(String formatID);

  /**
   * Creates a new RDF serializer for a given format.
   * Returns <tt>null</tt> if formatID is unknown.
   */
  public RDFSerializer createSerializer(String formatID);
}
