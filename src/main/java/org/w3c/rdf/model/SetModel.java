package org.w3c.rdf.model;

/**
 * An RDF model that natively supports set operations `union', `difference' and `intersection'.
 *
 * @see org.w3c.rdf.util.SetOperations
 */
public interface SetModel extends Model {

  /**
   * Set union with another model.
   * @return  <code>this</code>, i.e. the model itself
   */
  public SetModel unite(Model m) throws ModelException;

  /**
   * Set difference with another model.
   * @return  <code>this</code>, i.e. the model itself
   */
  public SetModel subtract(Model m) throws ModelException;

  /**
   * Set intersection with another model.
   * @return  <code>this</code>, i.e. the model itself
   */
  public SetModel intersect(Model m) throws ModelException;

}

