package mop.java.cmplx;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simplices and other (eg quadrilateral) cells.
 * <p>
 * Immutable.
 * <p>
 * Identity equality.
 * <p>
 * TODO: Consider explicit inclusion (possibly lazy) of (oriented)
 * sub-cells.
 *
 * @author palisades dot lakes at gmail dot com
 * @version 2026-02-18
 */

public interface Cell extends Comparable {


  static final AtomicInteger _counter = new AtomicInteger(-1);
  static int counter () { return _counter.getAndIncrement(); }

  /** Currently, only require the included zero simplices.
   * Higher dimensional sub-cells may be transient and created as needed.
   * Or, implementations may specify concrete and persistent sub-cells.
   * <br>
   * TODO: For un-oriented cells, <code>vertices</code> could be a Set.
   * For oriented cells, order matters only up to circular permutation.
   * Look for a data structure that handles circular permutation
   * invariance efficiently?
   * In the meantime, implementations can use a list with the elements
   * ordered so that the minimal (in the sense of Comparable) vertex
   * comes first.
   */
  public List vertices ();

  /** In general, 2 cells are equivalent is they have the same vertices.
   */
  @SuppressWarnings("unused")
  public boolean isOriented ();

  /** Do the 2 cells have the same sub-cells and same orientation
   * (when oriented).
   */

  @SuppressWarnings("unused")
  public boolean equivalent (Cell other);

}
