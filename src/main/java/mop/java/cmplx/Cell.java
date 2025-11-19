package mop.java.cmplx;

//----------------------------------------------------------------------

import java.util.Collection;

/**
 * Simplices and other (eg quadrilateral) cells.
 * <p>
 * Immutable.
 * <p>
 * Identity equality.
 *
 * @author palisades dot lakes at gmail dot com
 * @version 2025-11-18
 */

public interface Cell
extends Comparable {
  //public boolean isOriented ();
  public Collection vertices (); }
