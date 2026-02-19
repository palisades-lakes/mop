package mop.java.cmplx;

import java.util.List;

/**
 * Simplicial and other (eg quadrilateral) cell complexes.
 * <p>
 * Immutable.
 * <p>
 * Identity equality.
 * <p>
 */

public interface CellComplex {

  /** Return an unmodifiable list. */
  public List vertices ();

  /** Return an unmodifiable list. */
  public List faces ();

}
