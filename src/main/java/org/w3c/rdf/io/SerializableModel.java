package org.w3c.rdf.io;

import org.w3c.rdf.model.*;
import org.w3c.rdf.syntax.*;
import org.w3c.rdf.util.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import org.xml.sax.*;

/**
 * This class is used for serialization of RDF models.  Arbitraty
 * serialization formats are supported. Serialized model can be
 * optionally gzipped.
 * <p>
 * To make a custom model implementation serializable, simply add the
 * following lines
 * <blockquote><pre>
 * import org.w3c.rdf.model.*;
 * import org.w3c.rdf.io.*;
 * import java.io.*;
 *
 * public class MyModel implements Model, Serializable {
 * ...
 *   Object writeReplace() throws ObjectStreamException {
 *     return new SerializableModel(this);
 *   }
 * </pre></blockquote>
 *
 * Custom parser and ModelFactory can be set for a model read from stream as follows:
 * <blockquote><pre>
 * SerializableModel sm = (SerializableModel)m;
 * sm.setRDFSyntaxFactory(mySyntaxFactory);
 * sm.setModelFactory(myModelFactory);
 * sm.size(); // any public accessor
 *
 * <p>
 *
 * On-the-wire format:
 * <ol>
 *   <li>String: modelURI
 *   <li>String: format type, e.g. "RDF/XML-19990222"
 *   <li>boolean: gzipped
 *   <li>byte array contained (gzipped) model serialization in UTF8 encoding
 * </ol>
 *
 * <!--Special case: if format type is "NATIVE", it is followed by the count of triples (int) + Statement objects.-->
 *
 * @see org.w3c.rdf.syntax.RDFSyntaxFactory
 */
public class SerializableModel implements Model, Serializable {

  public static final byte GZIP_NONE = 0;
  public static final byte GZIP_FORCE = 1;
  public static final byte GZIP_AUTO = 2;

  //  public static final String NATIVE = "NATIVE";

  static RDFSyntaxFactory defaultLookup = new edu.stanford.db.rdf.syntax.RDFSyntaxFactoryImpl();
  static ModelFactory defaultModelFactory = new RDFFactoryImpl();

  static String defaultTargetFormat = edu.stanford.db.rdf.syntax.RDFSyntaxFactoryImpl.TripleXML_20010107;
  //  static String defaultTargetFormat = RDFSyntaxFactory.RDF_XML_19990222;
  static byte defaultTargetGZipped = GZIP_AUTO;
  static boolean directResolve = true;

  // these three variables are discarded (nulled) as soon as the model
  // has been parsed.
  private transient byte gzipped; // whether actual s_model is gzipped | whether to zip for writing
  private transient String format; // actual wire format on stream | format used for writing

  // (possibly zipped) string content of model
  // need to cache, since user can overwrite type of model to return
  private transient String s_uri; // uri of the model (for backward compatibility)
  //  private transient StringBuffer s_model;
  private transient byte[] s_model;

  //  RDFFactory factory; // preferences for creating model from stream, and for writing to stream

  // either m or the above variables are non null
  private transient Model m; // unfolded

  private transient RDFSyntaxFactory lookup; // lookup, precedes defaultLookup
  private transient ModelFactory modelFactory;

  /**
   * Called by Java (de)serialization
   */
  protected SerializableModel() {
  }

  public SerializableModel(Model m) {

    this(m, defaultTargetFormat, defaultTargetGZipped);
  }

  public SerializableModel(Model m, String targetFormat, byte targetGZipped) {

    this.m = m;
    this.format = targetFormat;
    this.gzipped = targetGZipped;
    this.lookup = defaultLookup;
    this.modelFactory = defaultModelFactory;

//     if(gzipped)
//       throw new RuntimeException("SerializableModel: gzipped on-the-wire format not yet supported!");
  }

  public static void setDirectResolve(boolean d) {

    directResolve = d;
  }

  protected Object readResolve() throws ObjectStreamException {

    if(directResolve) {
      try {
	return getInternalModel();
      } catch (Exception any) {
	//	throw new ObjectStreamException("SerializableResource: could not resolve object. Reason: " + any);
      }
    }
    return this;
  }

  boolean isFolded() {

    return m == null;
  }

  void unfold() throws ModelException, SAXException, IOException {

    unfold(lookup, modelFactory);
  }

  void silentUnfold() throws ModelException {

    try {
      unfold(lookup, modelFactory);
    } catch (ModelException me) {
      throw me;
    } catch (Exception any) {

      throw new ModelException("SerializableModel: could not unfold serialized model. Reason: " + any);
    }
  }

  void unfold(RDFSyntaxFactory lookup, ModelFactory mf) throws ModelException, SAXException, IOException {

    if(m != null)
      return;

    synchronized(this) {

      m = mf.createModel();
      m.setSourceURI(s_uri);

      RDFParser parser = lookup.createParser(format);
      InputStream in = new ByteArrayInputStream(s_model);
      //      System.err.println("READING GZIPPED: " + gzipped);
      if(gzipped > GZIP_NONE)
	in = new GZIPInputStream(in);
      parser.parse( new InputSource(new InputStreamReader(in)), new ModelConsumer(m) );
      // clean up
      s_uri = null;
      s_model = null;
      gzipped = defaultTargetGZipped;
    }
  }

  public static void setDefaultTargetFormat(String format) {

    defaultTargetFormat = format;
  }

  public static void setDefaultGZipped(byte gzipped) {

    defaultTargetGZipped = gzipped;
  }

  public static void setDefaultRDFSyntaxFactory(RDFSyntaxFactory l) {

    defaultLookup = l;
  }

  public void setRDFSyntaxFactory(RDFSyntaxFactory l) {

    lookup = l;
  }

  public RDFSyntaxFactory getRDFSyntaxFactory() {

    return lookup;
  }

  public void setModelFactory(ModelFactory l) {

    modelFactory = l;
  }

  public ModelFactory getModelFactory() {

    return modelFactory;
  }

  public void setDefaultModelFactory(ModelFactory mf) {

    defaultModelFactory = mf;
  }

  public Model getInternalModel() throws ModelException, SAXException, IOException {

    return getInternalModel(lookup, modelFactory);
  }

  public Model getInternalModel(RDFSyntaxFactory lookup, ModelFactory mf) throws ModelException, SAXException, IOException {

    unfold(lookup, mf);
    return m;
  }

  private void writeObject(java.io.ObjectOutputStream out) throws IOException {

    try {

      synchronized(this) {

	boolean zipIt = (gzipped == GZIP_FORCE) || (gzipped == GZIP_AUTO && m.size() >= 100);

	if(!isFolded()) {

	  ByteArrayOutputStream b = new ByteArrayOutputStream();
	  RDFSerializer serializer = lookup.createSerializer(format);
	  OutputStream os = zipIt ? (OutputStream)new GZIPOutputStream(b) : (OutputStream)b;
	  serializer.serialize(m, new OutputStreamWriter(os, "UTF8"));
	  os.close();
	  s_model = b.toByteArray(); // new StringBuffer(b.toString());
	  s_uri = m.getSourceURI();

	} // otherwise still serialized, just write it out

	out.writeObject(s_uri);
	out.writeObject(format);
	out.writeBoolean(zipIt);
	out.writeObject(s_model);
      }
    } catch (IOException io) {

      throw io;
      
    } catch (Exception any) {

      throw new IOException("SerializableModel: could not write object. Reason: " + any);
    }
  }

  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {

    s_uri = (String)in.readObject();
    format = (String)in.readObject();

    /*
    if(NATIVE.equals(format)) {

      m = defaultModelFactory.createModel();
      NodeFactory f = m.getNodeFactory();

      int count = in.readInt();
      while(count-- > 0) {

	
      }

    } else {
    */

    boolean zipIt = in.readBoolean();
    gzipped = zipIt ? GZIP_FORCE : GZIP_NONE;
    
    //    System.err.println("READ HEADER: " + s_uri + ", " + gzipped);
    
    //    s_model = (StringBuffer)in.readObject();
    s_model = (byte[])in.readObject();
    
    m = null; // just to make sure...
    //    }

    lookup = defaultLookup;
    modelFactory = defaultModelFactory;
  }

  // MODEL INTERFACE

  public void setSourceURI(String uri) throws ModelException {

    silentUnfold();
    m.setSourceURI(uri);
  }

  public String getSourceURI() throws ModelException {

    silentUnfold();
    return m.getSourceURI();
  }

  public int size() throws ModelException {

    silentUnfold();
    return m.size();
  }

  public boolean isEmpty() throws ModelException {

    silentUnfold();
    return m.isEmpty();
  }

  public Enumeration elements() throws ModelException {

    silentUnfold();
    return m.elements();
  }

  public boolean contains(Statement t) throws ModelException {

    silentUnfold();
    return m.contains(t);
  }

  public void add(Statement t) throws ModelException {

    silentUnfold();
    m.add(t);
  }

  public void remove(Statement t) throws ModelException {

    silentUnfold();
    m.remove(t);
  }

  public boolean isMutable() throws ModelException {

    silentUnfold();
    return m.isMutable();
  }

  public Model find( Resource subject, Resource predicate, RDFNode object ) throws ModelException {

    silentUnfold();
    return m.find(subject, predicate, object);
  }

  public Model duplicate() throws ModelException {

    silentUnfold();
    return m.duplicate();
  }

  public Model create() throws ModelException {

    silentUnfold();
    return m.create();
  }

  public NodeFactory getNodeFactory() throws ModelException {

    silentUnfold();
    return m.getNodeFactory();
  }

  public String getURI() throws ModelException {

    silentUnfold();
    return m.getURI();
  }

  public String getNamespace() throws ModelException {

    silentUnfold();
    return m.getNamespace();
  }

  public String getLocalName() throws ModelException {

    silentUnfold();
    return m.getLocalName();
  }

  public String getLabel() throws ModelException {

    silentUnfold();
    return m.getLabel();
  }
}
