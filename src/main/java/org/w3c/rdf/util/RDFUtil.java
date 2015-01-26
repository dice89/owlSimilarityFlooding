/**
 * Copyright © Sergey Melnik (Stanford University, Database Group)
 *
 * All Rights Reserved.
 */

package org.w3c.rdf.util;

import org.w3c.rdf.model.*;
import org.w3c.rdf.syntax.*;
import org.w3c.rdf.vocabulary.rdf_syntax_19990222.*;
import org.w3c.rdf.vocabulary.rdf_schema_19990303.*;

import org.w3c.rdf.implementation.model.ResourceImpl;
import org.w3c.rdf.util.SetOperations;
//import org.w3c.tools.crypt.*;
import org.xml.sax.*;

import java.util.*;
import java.io.*;
import java.net.*;

/**
 * Useful utility methods.
 *
 * @author Sergey Melnik <melnik@db.stanford.edu>
 */

public class RDFUtil {

  /**
   * Creates a new unique unnamed resource.
   */
  public static Resource noname(Model m) throws ModelException {

    return m.getNodeFactory().createUniqueResource();
  }


  /*
    public static Resource noname(String namespace) {
    return new Resource(namespace + "#" + Integer.toHexString( rnd.nextInt() ));
    }
  */

  /**
   * Converts an ordinal property to an integer.
   */
  public static int getOrd(Resource r)  throws ModelException {

    if(r == null)
      return -1;

    String uri = r.toString();
    if(!isRDF(uri))
      return -1;

    int pos = getNamespaceEnd(uri);
    if(pos > 0 && pos + 1 < uri.length()) {
      try {
        int n = Integer.parseInt(uri.substring(pos + 1));
        if(n >= 1)
          return n;
      } catch (Exception any) {}
    }
    return -1;
  }

  /**
   * Tests if the URI is qualified, i.e. has a namespace prefix.
   */
  public static boolean isQualified(String s) {
    
    int l = s.length()-1;
    do {
      char c = s.charAt(l);
      if(c == '#' || c == ':')
        return true;
      l--;
    } while (l >= 0);
    return false;
  }

  /**
   * Extracts the namespace prefix out of a URI.
   */
  public static String guessNamespace(String uri) {

    int l = getNamespaceEnd(uri);
    return l > 1 ? uri.substring(0, l /*-1*/) : "";
  }

  /**
   * Delivers the name out of the URI (without the namespace prefix).
   */
  public static String guessName(String uri) {

    return uri.substring(getNamespaceEnd(uri));
  }

  public static Resource createGuessedResource(NodeFactory f, String uri) throws ModelException {

    int l = getNamespaceEnd(uri);
    String ns = l > 1 ? uri.substring(0, l) : null;
    String name = uri.substring(l);

    return f.createResource(ns, name);
  }

  /**
   * Extracts the namespace prefix out of a URI.
   */
  public static String getNamespace(Resource r) throws ModelException {

    String ns = r.getNamespace();
    return ns != null ? ns : guessNamespace(r.getURI());
  }

  /**
   * Delivers the name out of the URI (without the namespace prefix).
   */
  public static String getLocalName(Resource r) throws ModelException {

    if(r.getNamespace() == null)
      return guessName(r.getURI());
    else
      return r.getLocalName();
  }

  /**
   * Tests if the URI belongs to the RDF syntax/model namespace.
   */
  public static boolean isRDF(String uri) {
    return uri != null && uri.startsWith(RDF._Namespace);
  }

  /**
   * Tests if the resource belongs to the RDF syntax/model namespace.
   */
  public static boolean isRDF(Resource r) throws ModelException {
    return isRDF(r.getURI());
  }

  /**
   * Position of the namespace end
   */
  static int getNamespaceEnd(String uri) {

    int l = uri.length()-1;
    do {
      char c = uri.charAt(l);
      if(c == '#' || c == ':' || c == '/')
        break;
      l--;
    } while (l >= 0);
    l++;
    return l;
  }

  /**
   * Returns the first triple of the model
   */
  public static Statement get1(Model m) throws ModelException {
    if(m == null || m.isEmpty())
      return null;
    //    if(m.size() > 1)
    //      throw new RuntimeException("Model contains more than one triple");
    return (Statement)m.elements().nextElement();
  }

  public static void add(Model m, Resource subject, Resource predicate, RDFNode object) throws ModelException {

    m.add(m.getNodeFactory().createStatement(subject, predicate, object));
  }

  /**
   * returns true if old triples from r were removed
   */
  public static boolean setUniqueObject(Model r, Resource subject, Resource predicate, RDFNode object) throws ModelException {

    Model old = r.find(subject, predicate, null);
    SetOperations.subtract(r, old);
    Statement stmt = get1(old);

    if(subject == null && stmt != null)
      subject = stmt.subject();
    if(predicate == null && stmt != null)
      predicate = stmt.predicate();

    r.add(r.getNodeFactory().createStatement(subject, predicate, object));
    return !old.isEmpty();
  }

  /**
   * returns the literal value of the node reachable from subject via predicate
   */
  public static String getObjectLiteral(Model r, Resource subject, Resource predicate) throws ModelException {

    RDFNode obj = getObject(r, subject, predicate);
    if(obj instanceof Literal)
      return obj.toString();
    else
      return null;
  }

  public static Resource getObjectResource(Model r, Resource subject, Resource predicate) throws ModelException {

    RDFNode obj = getObject(r, subject, predicate);
    if(obj instanceof Resource)
      return (Resource)obj;
    else
      return null;
  }

  public static boolean isInstanceOf(Model r, Resource i, Resource cls) throws ModelException {
    return !r.find(i, RDF.type, cls).isEmpty();
  }

  public static RDFNode getObject(Model r, Resource subject, Resource predicate) throws ModelException {

    Model m = r.find(subject, predicate, null);
    if(m == null || m.size() == 0)
      return null;
    //    if(m.size() > 1)
    //      throw new RuntimeException("Model contains more than one triple");
    // FIXME: we do not check whether it is a resource or literal
    return ((Statement)m.elements().nextElement()).object();
  }

  public static List getObjects(Model m, Resource subject, Resource predicate) throws ModelException {

    List result = new ArrayList();

    if(m == null || m.size() == 0)
      return result;

    for(Enumeration en = m.find(subject, predicate, null).elements(); en.hasMoreElements();) {

      Statement st = (Statement)en.nextElement();
      result.add(st.object());
    }
    return result;
  }

  public static Resource getSubject(Model r, Resource predicate, RDFNode object) throws ModelException {

    Model m = r.find(null, predicate, object);
    if(m == null || m.size() == 0)
      return null;
    //    if(m.size() > 1)
    //      throw new RuntimeException("Model contains more than one triple");
    return ((Statement)m.elements().nextElement()).subject();
  }

  /**
   * Prints the triples of a model to the given PrintStream.
   */
  public static void printStatements(Model m, PrintStream ps) throws ModelException {

    for(Enumeration en = m.elements(); en.hasMoreElements();) {

      Statement t = (Statement)en.nextElement();
      ps.println (t); // "triple(\""+t.subject()+"\",\""+t.predicate()+"\",\""+t.object()+"\").");
    }
  }

  /**
   * Dumps the model in a serialized form.
   */
  public static void dumpModel(Model m, PrintStream ps, RDFSerializer s) throws ModelException, IOException, SerializationException {
    OutputStreamWriter w = new OutputStreamWriter(ps);
    s.serialize( m, w );
  }

  /**
   * Dumps the model in a serialized form in a string
   */
  public static String dumpModel(Model m, RDFSerializer s) throws ModelException, IOException, SerializationException {

    StringWriter w = new StringWriter();
    s.serialize( m, w );
    return w.toString();
  }

  /**
   * Collects the triples of a model in a vector.
   */
  public static Vector getStatementVector(Model m) throws ModelException {

    Vector v = new Vector();
    for(Enumeration en = m.elements(); en.hasMoreElements();) {

      Statement t = (Statement)en.nextElement();
      v.addElement(t);
    }
    return v;
  }

  /**
   * Collects the triples of a model into an array.
   */
  public static Statement[] getStatementArray(Model m) throws ModelException {

    Statement[] v = new Statement[m.size()];
    int i=0;
    for(Enumeration en = m.elements(); en.hasMoreElements();) {
      
      Statement t = (Statement)en.nextElement();
      v[i++] = t;
    }
    return v;
  }

  /**
   * Removes all triples which have something to do with the given namespace
   */
  public static Model removeNamespace( String ns, Model m ) throws ModelException {

    Model res = m.duplicate();
    for(Enumeration en = m.duplicate().elements(); en.hasMoreElements();) {

      Statement t = (Statement)en.nextElement();
      if(t.subject().toString().startsWith( ns ) ||
         t.predicate().toString().startsWith( ns ) ||
         t.object().toString().startsWith( ns )) {
        //      System.err.println("REMOVING TRIPLE: " + t);
        res.remove(t);
      }
    }
    return res;
  }

  /**
   * returns a subgraph of "m" containing
   * "r" and all nodes reachable from "r" via directed edges.
   * These edges are also included in the resulting model.
   */
  public static Model getReachable(Resource r, Model m) throws ModelException {

    Model result = m.create();
    getReachable(r, m, result);
    return result;
  }

  // FIXME: use digest instead of size
  static void getReachable(Resource r, Model m, Model result) throws ModelException {

    int oldSize = result.size();
    Model directlyReachable = m.find(r, null, null);
    SetOperations.unite(result, directlyReachable);
    if(result.size() == oldSize)
      return;
    for(Enumeration en = directlyReachable.elements(); en.hasMoreElements();) {
      Statement t = (Statement)en.nextElement();
      if(t.object() instanceof Resource)
        getReachable((Resource)t.object(), m, result);
    }
  }

  public static void parse(String fileNameOrURL, RDFParser parser, Model model) throws IOException, SAXException, MalformedURLException, ModelException {

    URL url = new URL (normalizeURI(fileNameOrURL));

    // maybe this model is loaded as schema...
    //    Model model = factory.registry().get(url.toString());
    //    if(model != null)
    //      return model;

    // Prepare input source
    model.setSourceURI( url.toString() );
    InputStream in = url.openStream();
    InputSource source = new InputSource(in);
    source.setSystemId( url.toString() );

    parser.parse( source, new ModelConsumer(model) );
    in.close();
  }

  public static String normalizeURI(String uri) {

    // normalise uri
    URL url = null;
    try {
      url = new URL (uri);
    } catch (Exception e) {
      try {
        if(uri.indexOf(':') == -1)
          url = new URL ("file", null, uri);
      } catch (Exception e2) {}
    }
    return url != null ? url.toString() : uri;
  }

  public static Hashtable getResources(Model m) throws ModelException {

    Hashtable t = new Hashtable();
    for(Enumeration en = m.elements(); en.hasMoreElements();) {
      Statement s = (Statement)en.nextElement();
      t.put(s.subject(), s.subject());
      if(s.object() instanceof Resource)
        t.put(s.object(), s.object());
    }
    return t;
  }

  public static Hashtable getNodes(Model m) throws ModelException {

    Hashtable t = new Hashtable();
    for(Enumeration en = m.elements(); en.hasMoreElements();) {
      Statement s = (Statement)en.nextElement();
      t.put(s.subject(), s.subject());
      t.put(s.object(), s.object());
    }
    return t;
  }

  public static void saveModel(Model m, String fileName, RDFSerializer s) throws FileNotFoundException, IOException, ModelException, SerializationException {

    //    RandomAccessFile raf = new RandomAccessFile(fileName, "rw");
    //  raf.close();
    //    FileWriter writer = new FileWriter(raf.getFD());
    FileWriter writer = new FileWriter(fileName);
    s.serialize(m, writer);
    writer.close();
  }

  /** tries to determine the file name from getSourceURI */
  public static void saveModel(Model m, RDFSerializer s) throws FileNotFoundException, IOException, ModelException, SerializationException {

    // URI to filename
    URL url = null;
    try {
      url = new URL(m.getSourceURI());
    } catch (Exception any) {
      throw new ModelException("RDFUtil: cannot determine model file name: " + m.getSourceURI());
    }
    if("file".equals(url.getProtocol()))
      saveModel(m, url.getFile().replace('/', File.separatorChar), s);
    else
      throw new ModelException("RDFUtil: cannot save to non-file model URI: " + m.getSourceURI());
  }


  public static void collectNamespaces(Resource r, Collection target) throws ModelException {

    String ns = r.getNamespace();
    if(ns != null)
      target.add(ns);
  }

  // FIXME: what if reification is an endless loop?
  public static void collectNamespaces(Statement st, Collection target) throws ModelException {

    if(st.subject() instanceof Statement)
      collectNamespaces((Statement)st.subject(), target);
    else
      collectNamespaces(st.subject(), target);

    collectNamespaces(st.predicate(), target);
    
    if(st.object() instanceof Statement)
      collectNamespaces((Statement)st.object(), target);
    else if(st.object() instanceof Resource)
      collectNamespaces((Resource)st.object(), target);
  }

  public static void collectNamespaces(Model m, Collection target) throws ModelException {

    for(Enumeration en = m.elements(); en.hasMoreElements(); ) {

      Statement st = (Statement)en.nextElement();
      collectNamespaces(st, target);
    }
  }

  public static void collectLiterals(Model m, Collection target) throws ModelException {

    for(Enumeration en = m.elements(); en.hasMoreElements(); ) {

      Statement st = (Statement)en.nextElement();
      if(st.object() instanceof Literal)
	target.add(st.object());
    }
  }

  public static void collectPredicates(Model m, Collection target) throws ModelException {

    for(Enumeration en = m.elements(); en.hasMoreElements(); ) {

      Statement st = (Statement)en.nextElement();
      target.add(st.predicate());
    }
  }

  public static void collectResources(Model m, Collection target) throws ModelException {

    for(Enumeration en = m.elements(); en.hasMoreElements(); ) {

      Statement st = (Statement)en.nextElement();
      if(!(st.object() instanceof Literal) && !(st.object() instanceof Statement))
	target.add(st.object());
      target.add(st.subject());
      target.add(st.predicate());
    }
  }

  public static void collectResourcesSO(Model m, Collection target) throws ModelException {

    for(Enumeration en = m.elements(); en.hasMoreElements(); ) {

      Statement st = (Statement)en.nextElement();
      if(!(st.object() instanceof Literal) && !(st.object() instanceof Statement))
	target.add(st.object());
      target.add(st.subject());
    }
  }

  /**
   * Fills <tt>m</tt> with statements from <tt>s</tt> and
   * returns it.
   */
  public static Model toModel(Set s, Model m) throws ModelException {

    Iterator it = s.iterator();
    while(it.hasNext()) {
      Object o = it.next();
      if(o instanceof Statement)
	m.add((Statement)o);
    }
    return m;
  }

  public static Set toSet(Model m) throws ModelException {

    Set s = new HashSet();
    Enumeration en = m.elements();
    while(en.hasMoreElements())
      s.add(en.nextElement());
    return s;
  }

  /**
   * @return a new model in which all occurrences of the old resources
   * are replaced by the new ones. Returns number replacements done.
   */
  public static int replaceResources(Model m, Map o2n) throws ModelException {

    NodeFactory f = m.getNodeFactory();
    Enumeration en = m.elements();

    Model toRemove = m.create();
    Model toAdd = m.create();

    while(en.hasMoreElements()) {

      Statement st = (Statement)en.nextElement();
      Statement st_n = replaceResources(st, f, o2n);

      if(st_n != st) { // yes, pointer comparison
	toAdd.add(st_n);
	toRemove.add(st);
      }
    }

    SetOperations.subtract(m, toRemove);
    SetOperations.unite(m, toAdd);

    return toAdd.size();
  }

  /**
   * @return a new model in which all occurrences of the old resources
   * are replaced by the new ones. Returns number replacements done.
   */
  public static int replaceResource(Model m, Resource oldR, Resource newR) throws ModelException {

    Map map = new HashMap();
    map.put(oldR, newR);
    return replaceResources(m, map);
  }

  /**
   * @return a new model in which all occurrences of the old resources
   * are replaced by the new ones. Returns number replacements done.
   */
  public static int replaceResources(Model src, Model dest, Map o2n) throws ModelException {

    NodeFactory f = src.getNodeFactory();
    Enumeration en = src.elements();

    int replaced = 0;

    while(en.hasMoreElements()) {

      Statement st = (Statement)en.nextElement();
      Statement st_n = replaceResources(st, f, o2n);
      dest.add(st_n);
      if(st_n != st) // yes, pointer comparison
	replaced++;
    }
    return replaced;
  }

  public static void replaceResources(Collection src, Collection dest, NodeFactory f, Map o2n) throws ModelException {

    Iterator it = src.iterator();

    while(it.hasNext()) {

      Statement st = (Statement)it.next();
      dest.add(replaceResources(st, f, o2n));
    }
  }

  public static Statement replaceResources(Statement st, NodeFactory f, Map o2n) throws ModelException {

    boolean replaced = false;
    Resource subj = st.subject();
    Resource pred = st.predicate();
    RDFNode obj = st.object();

    Object n = null;

    if(obj instanceof Statement) {

      n = obj;
      obj = replaceResources((Statement)obj, f, o2n);
      replaced = n != obj;

    } else if((n = o2n.get(obj)) != null) {
      replaced = true;
      obj = (RDFNode)n;
    }

    if(subj instanceof Statement) {

      n = subj;
      subj = replaceResources((Statement)subj, f, o2n);
      replaced = n != subj;

    } if((n = o2n.get(subj)) != null) {
      replaced = true;
      subj = (Resource)n;
    }

    if((n = o2n.get(pred)) != null) {
      replaced = true;
      pred = (Resource)n;
    }
    return replaced ? f.createStatement(subj, pred, obj) : st;
  }

  // returns list of statements
  public static void replaceMult(Statement st, NodeFactory f, Map o2n, Collection result) throws ModelException {

    List l1 = new ArrayList();
    replaceMultSPO(st, f, o2n, l1, st.subject(), 0);

    List l2 = new ArrayList();
    for(int i=0; i < l1.size(); i++)
      replaceMultSPO((Statement)l1.get(i), f, o2n, l2, st.predicate(), 1);

    for(int i=0; i < l2.size(); i++)
      replaceMultSPO((Statement)l2.get(i), f, o2n, result, st.object(), 2);
  }

  // returns list of statements
  protected static void replaceMultSPO(Statement st, NodeFactory f, Map o2n, Collection result, RDFNode toReplace, int position) throws ModelException {

    Collection replacements;

    if(toReplace instanceof Statement) {

      List l = new ArrayList();
      replaceMult((Statement)toReplace, f, o2n, l);

      if(l.size() == 1 && toReplace == l.get(0)) {
	result.add(st);
	return; // keep the same
      } else
	replacements = l;

    } else {

      Object ro = o2n.get(toReplace);

      if(ro instanceof Collection)

	replacements = (Collection)ro;

      else if(ro != null) {

	replacements = new ArrayList();
	replacements.add(ro);

      } else { // no replacement needed

	result.add(st); // keep the same statement
	return;
      }
    }

    for(Iterator it = replacements.iterator(); it.hasNext();) {

      Statement rs = null;
      Object rr = it.next();

      switch(position) {

      case 0: rs = f.createStatement((Resource)rr, st.predicate(), st.object()); break;
      case 1: rs = f.createStatement(st.subject(), (Resource)rr, st.object()); break;
      case 2: rs = f.createStatement(st.subject(), st.predicate(), (RDFNode)rr); break;
      }
      result.add(rs);
    }
  }



  /**
   * @return a new model in which all occurrences of the old namespace
   * are replaced by the new one. All replaced resources are summarized in the map if not null.
   * If resourcesToIgnore != null, ignore resources listed there.
   */
  public static Model replaceNamespace(Model m, String o, String n, Map o2n, Set resourcesToIgnore) throws ModelException {

    Model res = m.create();
    NodeFactory f = m.getNodeFactory();
    Enumeration en = m.elements();
    while(en.hasMoreElements()) {

      Statement st = (Statement)en.nextElement();
      res.add(replaceNamespace(st, o, n, f, o2n, resourcesToIgnore));
    }
    return res;
  }

  public static Statement replaceNamespace(Statement st, String o, String n, NodeFactory f, Map o2n) throws ModelException {

    return replaceNamespace(st, o, n, f, o2n, null);
  }

  public static Statement replaceNamespace(Statement st, String o, String n, NodeFactory f, Map o2n, Set resourcesToIgnore) throws ModelException {

    boolean replaced = false;
    Resource subj = st.subject();
    Resource pred = st.predicate();
    RDFNode obj = st.object();
    
    if(obj instanceof Resource &&
       !(obj instanceof Statement) &&
       o.equals(((Resource)obj).getNamespace()) &&
       (resourcesToIgnore == null || !resourcesToIgnore.contains(obj))) {

      replaced = true;
      Resource r = f.createResource(n, ((Resource)obj).getLocalName());
      if(o2n != null)
	o2n.put(obj, r);
      obj = r;
    }
       
    if(o.equals(subj.getNamespace()) &&
       (resourcesToIgnore == null || !resourcesToIgnore.contains(subj))) {

      replaced = true;
      Resource r = f.createResource(n, subj.getLocalName());
      if(o2n != null)
	o2n.put(subj, r);
      subj = r;
    }

    if(o.equals(pred.getNamespace()) &&
       (resourcesToIgnore == null || !resourcesToIgnore.contains(pred))) {

      replaced = true;
      Resource r = f.createResource(n, pred.getLocalName());
      if(o2n != null)
	o2n.put(pred, r);
      pred = r;
    }
    return replaced ? f.createStatement(subj, pred, obj) : st;
  }

  public static boolean equalResources(Resource t, Resource that) throws ModelException {

    return t.getURI().equals(that.getURI());

    /*

    boolean b = false;

    String tns = t.getNamespace();
    String tn = t.getLocalName();

    String thatns = that.getNamespace();
    String thatn = that.getLocalName();

    //    System.err.print("Comparing " + t + " vs " + that + ": ");

    if(thatns == null) {
      if(tns == null) { // (null, null)
	//	System.err.println("true");
	return thatn.equals(tn);
      } else // (null, not null) maybe "that" did not detect names
	b = thatn.equals(t.getURI());
    } else {
      if(tns != null) // (not null, not null)
	b = thatn.equals(tn) && thatns.equals(tns);
      else // (not null, null) maybe "this" did not detect names
	b = that.getURI().equals(tn);
    }

    if(b)
      return true;

    // else we need to check whether different namespace splitting is used

    if((tns != null ? tns.length() : 0) + (tn != null ? tn.length() : 0) ==
       (thatns != null ? thatns.length() : 0) + (thatn != null ? thatn.length() : 0)) {

      b = t.getURI().equals(that.getURI());

    }

    //    System.err.println("" + b);
    return b;
    */
  }

  /**
   * This method is used to compute hash code of strings incrementally,
   * w/o the need to concatenate strings.
   * This method MUST be used in all implementations of org.w3c.rdf.model.Resource and
   * org.w3c.rdf.model.Literal!
   * The hash code is computed as
   * <blockquote><pre> 
   * s[0]*31^(n-1) + s[1]*31^(n-2) + ... + s[n-1]
   * </pre></blockquote>
   */
  public static int incrementalHashCode(int hash, char c) {

    return 31*hash + c;
  }

  /**
   * This method is used to compute hash code of strings incrementally,
   * w/o the need to concatenate strings.
   * This method MUST be used in all implementations of org.w3c.rdf.model.Resource and
   * org.w3c.rdf.model.Literal!
   * The hash code is computed as
   * <blockquote><pre> 
   * s[0]*31^(n-1) + s[1]*31^(n-2) + ... + s[n-1]
   * </pre></blockquote>
   */
  public static int incrementalHashCode(int hash, String segment) {

    int len = segment.length();

    for (int i = 0; i < len; i++)
      hash = 31*hash + segment.charAt(i);

    return hash;
  }

  /**
   * This method is used to compute hash code of strings incrementally,
   * w/o the need to concatenate strings.
   * This method MUST be used in all implementations of org.w3c.rdf.model.Resource and
   * org.w3c.rdf.model.Literal!
   * The hash code is computed as
   * <blockquote><pre> 
   * s[0]*31^(n-1) + s[1]*31^(n-2) + ... + s[n-1]
   * </pre></blockquote>
   */
  public static int incrementalHashCode(String segment) {

    return incrementalHashCode(0, segment);
  }

  /**
   * This method is used to compute the hash code of RDF statements.
   * All implementation of org.w3c.rdf.model.Statement MUST use
   * this method.
   * The hash code is computed as
   * <blockquote><pre>
   * ((s.hashCode() * 7) + p.hashCode()) * 7 + o.hashCode()
   * </pre></blockquote>
   */
  public static int statementHashCode(Resource s, Resource p, RDFNode o) {
    
    return ((s.hashCode() * 7) + p.hashCode()) * 7 + o.hashCode();
  }
}
