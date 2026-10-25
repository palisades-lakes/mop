package mop.java.cmplx;

import java.util.List;
import java.util.Objects;

/**
 * AKA '(Abstract) Edge'. An ordered pair of distinct zero simplexes.
 *
 * @author palisades dot lakes at gmail dot com
 * @version 2026-10-25
 */
public final record DirectedOneSimplex (ZeroSimplex z0,
                                       ZeroSimplex z1)
  implements Cell, Comparable<DirectedOneSimplex> {

  //--------------------------------------------------------------------
  // Cell
  //--------------------------------------------------------------------
  /** Note: returns immutable list.
   */
  @Override
  public final List vertices () { return List.of(z0(), z1()); }

  @Override
  public final boolean isOriented () { return true; }

  @Override
  public final boolean equivalent (final Cell that) {
    if (this == that) { return true; }
    if (that instanceof DirectedOneSimplex(ZeroSimplex z2,
                                           ZeroSimplex z3)) {
      return (z0() == z2) && (z1() == z3); }
    return false; }

  //--------------------------------------------------------------------
  // Comparable
  //--------------------------------------------------------------------
  /** Ordering will be match order of creation within a thread. will be
   * used to identify which point goes with which zero simplex in
   * embeddings.
   * TODO: lexicographic ordering via vertices?
   */

  @Override
  public final int compareTo (final DirectedOneSimplex that) {
    final int c = z0().compareTo(that.z0());
    if (0==c) { return z1().compareTo(that.z1()); }
    return c; }

  //--------------------------------------------------------------------
  // Object
  //--------------------------------------------------------------------

  @Override
  public final String toString () { return z0() + "->" + z1(); }

  @Override
  public final int hashCode () { return Objects.hash(z0(),z1()); }

  @Override
  public final boolean equals (final Object that) {
    if (this == that) { return true; }
    if (! (that instanceof DirectedOneSimplex)) { return false; }
    final DirectedOneSimplex e = (DirectedOneSimplex) that;
    // oriented aka directed edges
    return (z0() == e.z0()) && (z1() == e.z1()); }

  //--------------------------------------------------------------------
  // construction
  //--------------------------------------------------------------------

  public DirectedOneSimplex { assert z0 != z1; }

  public static final DirectedOneSimplex make (final ZeroSimplex z0,
                                               final ZeroSimplex z1) {
    return new DirectedOneSimplex(z0, z1); }

  //--------------------------------------------------------------------
} // end class
//--------------------------------------------------------------------
