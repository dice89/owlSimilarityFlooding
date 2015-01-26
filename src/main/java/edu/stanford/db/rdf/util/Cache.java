package edu.stanford.db.rdf.util;

import java.lang.ref.*;
import java.util.*;

// Following "feature" of java.util.WeakHashMap was discovered:
//
// JDK 1.3: put() overrides the value, but not the key ("feature" of HashMap).
//          Thus, the key (and WeakKey) of the new value remains the same, pointing to the old value.
//          When GC is called for the overriden (old) value, it trashes the key, and so removes the new value.
//
// JDK 1.4: Different implementation of WeakHashMap, but same problem remains:
//          Now, the Entry plays the role of the WeakKey.
//          When the old value is overriden in put(), same Entry object is used.
//          The Entry object uses as reference the key of the entry. The key is not overiden.
//          Thus, when GC is called for the old value, it trashes the new value.
//
// Fix/workaround: remove the entry from map before overriding with put() and make WeakKey.equals()
// test for identity.

public class Cache {

  Object[] clockArray;
  int clockPtr = 0;

  HashMap map = new HashMap();     // WeakKey     -> WeakReference | WeakLongKey
  HashMap inverse = new HashMap(); // WeakLongKey -> WeakLongKey

  /* Reference queue for cleared WeakReferences */
  private ReferenceQueue queue = new ReferenceQueue();

  int discarded = 0;

  /* Remove all invalidated entries from the map, that is, remove all entries
     whose keys have been discarded.  This method should be invoked once by
     each public mutator in this class.  We don't invoke this method in
     public accessors because that can lead to surprising
     ConcurrentModificationExceptions. */
  private void processQueue() {
    Object wk; // WeakKey | WeakLongKey
    while ((wk = queue.poll()) != null) {
      discarded++;
//       if(debug)

      Object rm = null, ri = null;
      // remove it from both maps
      if(wk instanceof WeakKey)
	rm = map.remove(wk);
      else if(inverse != null && wk instanceof WeakLongKey)
	ri = inverse.remove(wk);

      //      System.err.println("[Cache] removing: " + wk + " from map: " + rm + ", from inverse: " + ri);
    }
  }


  public Cache(int cacheSize) {
    if(cacheSize == 0)
      cacheSize = 1; // to prevent unnecessary checks
    clockArray = new Object[cacheSize];
  }

  public Cache(int cacheSize, boolean hasInverse) {

    this(cacheSize);
    if(!hasInverse)
      inverse = null;
  }

  /**
   * Both key and value may be Object or LongIDAware.
   * Key and value may be equal
   */
  public void put(Object key, Object value) {

    if(key == null || value == null)
      throw new NullPointerException("[Cache] cannot cache nulls: key=" + key + ", value=" + value);

    processQueue();

    // make sure we cache the value at least for a while
    clockArray[clockPtr] = value;
    if(clockPtr + 1 >= clockArray.length)
      clockPtr = 0;
    else
      clockPtr++;

    //    Object wkey = key instanceof LongIDAware ? (Object)WeakLongKey.create((LongIDAware)key, queue) : (Object)WeakKey.create(key, queue);
    Object wkey = (Object)WeakKey.create(key, queue);

    Object wvalue =
      (key == value) ? (Object)wkey :
      (value instanceof LongIDAware) ? (Object)WeakLongKey.create((LongIDAware)value, queue) : (Object)new WeakReference(value);

    // needed due to bug in WeakHashMap
    map.remove(new LookupWeakKey(key));
    // now, register with map
    map.put(wkey, wvalue);
    
    if(inverse != null) { // value is instanceof LongIDAware

      WeakLongKey w = wvalue instanceof WeakLongKey ? (WeakLongKey)wvalue : WeakLongKey.create((LongIDAware)value, queue);
      // needed due to bug in WeakHashMap
      inverse.remove(new LookupLongKey(((LongIDAware)value).getLongID()));
      // now, register with inverse
      inverse.put(w, w);
    }
  }

  public Object get(Object key) {

    // no null keys
    if(key == null)
      return null;

    Object wkey = new LookupWeakKey(key);
    // key instanceof LongIDAware ? (Object)WeakLongKey.create((LongIDAware)key) : (Object)WeakKey.create(key);
    WeakReference r = (WeakReference)map.get(wkey);
    return r != null ? r.get() : null;
  }

  public Object get(long id) {

    if(inverse != null) {
      WeakReference r = (WeakReference)inverse.get(new LookupLongKey(id));
      return r != null ? r.get() : null;
    } else
      return null;
  }

  public String toString() {

    return "CACHE[" + map.size() + (inverse != null ? ", " + inverse.size() : "") + ", " + discarded + "]";
  }


  /** This class is used for looking up WeakKeys only */

  static private class LookupWeakKey {

    Object key;

    private LookupWeakKey(Object key) {

      this.key = key;
    }

    public String toString() {

      return "LookupWeakKey[" + key + "]";
    }

    // same as hash code of WeakKey
    public int hashCode() {
      return key.hashCode();
    }

    /** o is guaranteed to be a WeakLongKey */
    public boolean equals(Object o) {
      //      if (this == o) return true;
      //      if (!(o instanceof WeakKey)) return false;

      if (o instanceof WeakKey) {
	Object t = key;
	Object u = ((WeakKey)o).get();
	if ((t == null) || (u == null)) return false;
	if (t == u) return true;
	return t.equals(u);

      } else if(o instanceof WeakLongKey) {

	return ((LongIDAware)key).getLongID() == ((WeakLongKey)o).id;
      }

      return false;
    }
  }

  /** Borrowed directly from WeakHashMap in JDK 1.3 */

    static private class WeakKey extends WeakReference {
	private int hash;	/* Hashcode of key, stored here since the key
				   may be tossed by the GC */

	private WeakKey(Object k) {
	    super(k);
	    hash = k.hashCode();
	}

	private static WeakKey create(Object k) {
	    if (k == null) return null;
	    else return new WeakKey(k);
	}

	private WeakKey(Object k, ReferenceQueue q) {
	    super(k, q);
	    hash = k.hashCode();
	}

	private static WeakKey create(Object k, ReferenceQueue q) {

	  // System.err.println("Creating WeakKey: " + k);
	    if (k == null) return null;
	    else return new WeakKey(k, q);
	}

        /* A WeakKey is equal to another WeakKey iff they both refer to objects
	   that are, in turn, equal according to their own equals methods */
	public boolean equals(Object o) {
	    if (this == o) return true;

	    if(o instanceof WeakKey) // test for identity
	      return this == o;

	    else if(o instanceof LookupWeakKey) {
	      // test for equality
	      Object t = this.get();
	      Object u = ((LookupWeakKey)o).key;
	      if ((t == null) || (u == null)) return false;
	      if (t == u) return true;
	      return t.equals(u);
	    }
	    return false;
	}

      public int hashCode() {
	return hash;
      }

    public String toString() {

	return "WeakKey[" + this.get() + ", " + Integer.toHexString(System.identityHashCode(this)) + "]";
    }

    }

  /** This class is used for looking up WeakKeys only */

  static private class LookupLongKey {

    long id;

    private LookupLongKey(long id) {

      this.id = id;
    }

    public String toString() {

      return "LookupLongKey[" + id + "]";
    }

    // same as hash code of WeakKey
    public int hashCode() {
      return (int)id;
    }

    /** o is guaranteed to be a WeakLongKey */
    public boolean equals(Object o) {
      //      if (this == o) return true;
      //      if (!(o instanceof WeakKey)) return false;
      return id == ((WeakLongKey)o).id;
    }
  }


  /** Based on WeakKey */

    static private class WeakLongKey extends WeakReference {
	private long id;	/* Hashcode of key, stored here since the key
				   may be tossed by the GC */

      private WeakLongKey(long id) {
	super(null);
	this.id = id;
      }

	private WeakLongKey(LongIDAware k) {
	    super(k);
	    id = k.getLongID();
	}

	private static WeakLongKey create(LongIDAware k) {
	    if (k == null) return null;
	    else return new WeakLongKey(k);
	}

	private WeakLongKey(LongIDAware k, ReferenceQueue q) {
	    super(k, q);
	    id = k.getLongID();
	    // System.err.println("Creating WeakLongKey: " + k);
	}

	private static WeakLongKey create(LongIDAware k, ReferenceQueue q) {
	    if (k == null) return null;
	    else return new WeakLongKey(k, q);
	}

      public String toString() {

	return "WeakLongKey[" + id + ", " + Integer.toHexString(System.identityHashCode(this)) + "]";
      }

        /* A WeakKey is equal to another WeakKey iff they both refer to objects
	   that are, in turn, equal according to their own equals methods */
	public boolean equals(Object o) {

	  if (this == o) return true;

	  if(o instanceof WeakLongKey) // test for identity
	    return this == o;

	  else if(o instanceof LookupLongKey)
	    // test for equality
	    return id == ((LookupLongKey)o).id;

	  return false;
	}

        public int hashCode() {
	    return (int)id;
	}

    }

}

