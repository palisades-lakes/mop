package mop.java.cmplx;

import java.util.List;
import java.util.Objects;

/** AKA '(Abstract) Face'.
 * An oriented (ordered up to circular permutation)
 * triple of zero simplexes.
 *
 * @author palisades dot lakes at gmail dot com
 * @version 2026-10-10
 */
public final class TwoSimplex
  implements Cell, Comparable<TwoSimplex> {

  //--------------------------------------------------------------------

  private static final ZeroSimplex[]
  minimalCircularPermutation (final ZeroSimplex z0,
                              final ZeroSimplex z1,
                              final ZeroSimplex z2) {
    final ZeroSimplex[] z = new mop.java.cmplx.ZeroSimplex[3];
    if ((0 > z0.compareTo(z1)) && (z0.compareTo(z2) < 0)) {
      z[0] = z0; z[1] = z1; z[2] = z2; }
    else if (0 > z1.compareTo(z2)) {
      z[0] = z1; z[1] = z2;  z[2] = z0; }
    else {
      z[0] = z2; z[1] = z0; z[2] = z1; }
    return z; }

  //--------------------------------------------------------------------

  private final ZeroSimplex _z0;
  public final ZeroSimplex z0 () { return _z0; }
  private final ZeroSimplex _z1;
  public final ZeroSimplex z1 () { return _z1; }
  private final ZeroSimplex _z2;
  public final ZeroSimplex z2 () { return _z2; }

  //--------------------------------------------------------------------
  // Cell
  //--------------------------------------------------------------------
  /** Note: returns immutable list. */
  @Override
  public final List vertices () {
    return List.of(z0(), z1(), z2()); }

  @Override
  public final boolean isOriented () { return true; }

  @Override
  public final boolean equivalent (final Cell that) {
    // assuming vertices are constrained to same circular permutation
    if (this == that)  { return true; }
    if (! (that instanceof TwoSimplex)) { return false; }
    final TwoSimplex x = (TwoSimplex) that;
    return (z0() == x.z0()) && (z1() == x.z1()) && (z2() == x.z2()); }

  //--------------------------------------------------------------------
  // Comparable
  //--------------------------------------------------------------------
  /** Lexicographic ordering via vertices.
   */

  @Override
  public final int compareTo (final TwoSimplex that) {
    final int c0 = z0().compareTo(that.z0());
    if (0==c0) {
      final int c1 = z1().compareTo(that.z1());
      if (0==c1) { return z2().compareTo(that.z2()); }
      return c1; }
    return c0; }

  //--------------------------------------------------------------------
  // Object
  //--------------------------------------------------------------------

  @Override
  public final String toString () {
    return z0() + "->" + z1() + "->" + z2(); }

  @Override
  public final int hashCode () {
    return Objects.hash(z0(), z1(), z2()); }

  @Override
  public final boolean equals (final Object that) {
    assert that instanceof TwoSimplex;
    return equivalent((TwoSimplex) that); }

  //--------------------------------------------------------------------
  // construction
  //--------------------------------------------------------------------

  private TwoSimplex (final ZeroSimplex z0,
                      final ZeroSimplex z1,
                      final ZeroSimplex z2) {
    assert ! z0.equals(z1);
    assert ! z1.equals(z2);
    assert ! z2.equals(z0);
    final ZeroSimplex[] z = minimalCircularPermutation(z0, z1, z2);
    _z0 = z[0]; _z1 = z[1]; _z2 = z[2]; }

  public static final TwoSimplex make (final ZeroSimplex z0,
                                       final ZeroSimplex z1,
                                       final ZeroSimplex z2) {
    return new TwoSimplex(z0, z1, z2); }

  //--------------------------------------------------------------------
} // end class
//--------------------------------------------------------------------
