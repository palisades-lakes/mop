package mop.java.cmplx;

import java.util.List;
import java.util.Objects;

/** AKA '(Abstract) Edge'. An unordered pair of distinct zero simplexes.
 * <br>
 * Constructor orders the zero simplexes so that
 * <code>z0 < z1</code>.
 *
 * @author palisades dot lakes at gmail dot com
 * @version 2026-10-25
 */
public final record UndirectedOneSimplex (ZeroSimplex z0,
                                         ZeroSimplex z1)
  implements Cell, Comparable<UndirectedOneSimplex> {

  //--------------------------------------------------------------------
  // Cell
  //--------------------------------------------------------------------
  /** Note: returns immutable list.
   */
  @Override
  public final List vertices () { return List.of(z0(), z1()); }

  @Override
  public final boolean isOriented () { return false; }

  @Override
  public final boolean equivalent (final Cell that) {
    if (this == that) { return true; }
    if (that instanceof UndirectedOneSimplex(final ZeroSimplex z2,
                                             final ZeroSimplex z3)) {
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
  public final int compareTo (final UndirectedOneSimplex that) {
    final int c = z0().compareTo(that.z0());
    if (0==c) { return z1().compareTo(that.z1()); }
    return c; }

  //--------------------------------------------------------------------
  // Object
  //--------------------------------------------------------------------

  @Override
  public final String toString () { return z0() + "-" + z1(); }

  @Override
  public final int hashCode () { return Objects.hash(z0(),z1()); }

  @Override
  public final boolean equals (final Object that) {
    if (this == that) { return true; }
    if (! (that instanceof UndirectedOneSimplex)) { return false; }
    final UndirectedOneSimplex e = (UndirectedOneSimplex) that;
    // oriented aka directed edges
    return (z0() == e.z0()) && (z1() == e.z1()); }

  //--------------------------------------------------------------------
  // construction
  //--------------------------------------------------------------------

  public UndirectedOneSimplex (final ZeroSimplex z0,
                               final ZeroSimplex z1) {
    final int c = z0.compareTo(z1);
    assert 0 != c;
    if (0 > c) { this.z0 = z0; this.z1 = z1; }
    else { this.z0 = z1; this.z1 = z0; } }

  public static final UndirectedOneSimplex make (final ZeroSimplex z0,
                                                 final ZeroSimplex z1) {
    return new UndirectedOneSimplex(z0, z1); }

  //--------------------------------------------------------------------
} // end class
//--------------------------------------------------------------------
