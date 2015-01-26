package com.interdataworking;

import org.w3c.rdf.model.*;
import java.util.*;

public interface UntypedGateway {

  /**
   * Transforms an ordered list of models into another list of models
   */
  public List execute(List input) throws ModelException;

  public int getMinInputLen();

  public int getMaxInputLen();

  public int getMinOutputLen();

  public int getMaxOutputLen();
}
