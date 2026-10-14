package mop.java.cmplx;

import java.util.SortedSet;

/**
 * Simplicial and other (eg quadrilateral) cell complexes.
 * <p>
 * Immutable .
 * <p>
 * Identity equality.
 * </p>
 *
 * @author palisades dot lakes at gmail dot com
 * @version 2026-10-14
 */

public interface CellComplex {

  /** Return an unmodifiable set. */
  public SortedSet<ZeroSimplex> vertices ();

  /** Return an unmodifiable set. */
  public SortedSet<? extends Cell> faces ();

}
