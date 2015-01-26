package edu.stanford.db.rdf.syntax;

import org.w3c.rdf.syntax.*;

public class RDFSyntaxFactoryImpl implements RDFSyntaxFactory {

  public static final String TripleXML_20010107 = "TripleXML_20010107";

  /**
   * Creates a new RDF parser. Returns null if could not create one.
   */
  public RDFParser createParser(String formatID) {

    if(RDF_XML_19990222.equals(formatID))
      return new org.w3c.rdf.implementation.syntax.sirpac.SiRPAC();
    if(TripleXML_20010107.equals(formatID))
      return new edu.stanford.db.rdf.syntax.generic.TripleParser();

    return null;
  }

  /**
   * Creates a new RDF serializer
   */
  public RDFSerializer createSerializer(String formatID) {

    if(RDF_XML_19990222.equals(formatID))
      return new org.w3c.rdf.implementation.syntax.sirpac.SiRS();
    if(TripleXML_20010107.equals(formatID))
      return new edu.stanford.db.rdf.syntax.generic.TripleSerializer();

    return null;
  }

}
