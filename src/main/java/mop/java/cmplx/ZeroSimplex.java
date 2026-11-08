package mop.java.cmplx;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** * AKA '(Abstract) Vertex'. The basic unit of identity used to build
 * simplicial and quad complexes, and embedded meshes from them. Most
 * code uses <code>int</code>s as a low overhead substitute, but this is
 * dangerous with multiple meshes that may share vertices, edges, etc.
 *
 * @author palisades dot lakes at gmail dot com
 * @version 2026-11-07
 */
public final class ZeroSimplex
  implements Cell, Comparable<ZeroSimplex> {

  //--------------------------------------------------------------------

private static final AtomicInteger _counter = new AtomicInteger(0);
  private static int counter () { return _counter.getAndIncrement(); }

  private final int _count;
  private final int count () { return _count; }

  private final String _prefix;

  public final String prefix () { return _prefix; }

  //--------------------------------------------------------------------
  // Cell
  //--------------------------------------------------------------------
  /** Note: returns immutable list.
   */
  @Override
  public final List vertices () { return List.of(this); }

  @Override
  public final boolean equivalent (final Cell that) {
    return this == that; }

  //--------------------------------------------------------------------
  // Comparable
  //--------------------------------------------------------------------
  /** ordering will be match order of creation within a thread. will be
   * used to identify which point goes with which zero simplex in
   * embeddings.
   */

  @Override
  public final int compareTo (final ZeroSimplex that) {
    return _count - that.count(); }

  //--------------------------------------------------------------------
  // Object
  //--------------------------------------------------------------------

  @Override
  public final String toString () { return prefix() + count(); }

  @Override
  public final int hashCode () { return count(); }

  @Override
  public final boolean equals (final Object that) {
    assert that instanceof ZeroSimplex;
    return count() == ((ZeroSimplex) that).count(); }

  //--------------------------------------------------------------------
  // construction
  //--------------------------------------------------------------------

  private ZeroSimplex (final String prefix) {
    _count = counter(); _prefix = prefix; }

  public static final ZeroSimplex make (final String prefix) {
    return new ZeroSimplex(prefix); }

  public static final ZeroSimplex make () {
    return make("z"); }

  //--------------------------------------------------------------------
} // end class
//--------------------------------------------------------------------
