package mop.java.cmplx;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

/** Simplicial and other (eg quadrilateral) cell complexes.
 * <p>
 * Immutable.
 * <p>
 * Identity equality.
 * </p>
 *
 * @author palisades dot lakes at gmail dot com
 * @version 2026-11-19
 */

public interface CellComplex {

  /** Return an unmodifiable set. */
  public SortedSet<ZeroSimplex> vertices ();

  public default List<ZeroSimplex> vertexList () {
    return new ArrayList<>(vertices()); }


  /** Return an unmodifiable set. */
  public SortedSet<? extends Cell> faces ();

  @SuppressWarnings("unused")
  public default List<Cell> faceList () {
    return new ArrayList<>(faces()); }

}
