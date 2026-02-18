package mop.java.cmplx;

import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * Abstract quadrilateral cell.
 * Not a simplex, so maybe should be elsewhere.
 * An oriented (ordered up to circular permutation) quadruple
 * of zero simplexes.
 *
 * @author palisades dot lakes at gmail dot com
 * @version 2026-02-18
 */
public final class Quad implements Cell {

  //--------------------------------------------------------------------
  // TODO: reuse array?

  private static final ZeroSimplex[]
  minimalCircularPermutation (final ZeroSimplex z0,
                              final ZeroSimplex z1,
                              final ZeroSimplex z2,
                              final ZeroSimplex z3) {
    final ZeroSimplex[] z = new mop.java.cmplx.ZeroSimplex[4];
    if ((0 > z0.compareTo(z1))
      && (z0.compareTo(z2) < 0)
      && (z0.compareTo(z3) < 0)) {
      z[0] = z0; z[1] = z1; z[2] = z2; z[3] = z3; }
    else if ((0 > z1.compareTo(z2)) && (z1.compareTo(z3) < 0)) {
      z[0] = z1; z[1] = z2; z[2] = z3; z[3] = z0; }
    else if (0 > z2.compareTo(z3)) {
      z[0] = z2; z[1] = z3; z[2] = z0; z[3] = z1; }
    else {
      z[0] = z3; z[1] = z0; z[2] = z1; z[3] = z2; }
    return z; }

  //--------------------------------------------------------------------

  private final int _count;
  private final int count () { return _count; }

  private final ZeroSimplex _z0;
  public final ZeroSimplex z0 () { return _z0; }
  private final ZeroSimplex _z1;
  public final ZeroSimplex z1 () { return _z1; }
  private final ZeroSimplex _z2;
  public final ZeroSimplex z2 () { return _z2; }
  private final ZeroSimplex _z3;
  public final ZeroSimplex z3 () { return _z3; }

  //--------------------------------------------------------------------
  // Object
  //--------------------------------------------------------------------

  @Override
  public final String toString () {
    return
      "Q[" + z0() + "," + z1() + z2() + "," + z3() + "]"; }

  @Override
  public final int hashCode () { return _count; }

//  @Override
//  public final boolean equals (final Object that) {
//    assert that instanceof Quad;
//    return (this == that); }

  //--------------------------------------------------------------------
  // Comparable
  //--------------------------------------------------------------------

  /** Ordering will be match order of creation within a thread. will be
   * used to identify which point goes with which zero simplex in
   * embeddings.
   * TODO: lexicographic ordering via vertices?
   */
  @Override
  public final int compareTo (final @NonNull Object that) {
    return _count - ((Quad) that).count(); }

  //--------------------------------------------------------------------
  // Cell
  //--------------------------------------------------------------------
  /** Note: returns immutable list. */
  @Override
  public final List vertices () {
    return List.of(z0(), z1(), z2(), z3()); }

  @Override
  public final boolean isOriented () { return true; }

  @Override
  public final boolean equivalent (final Cell that) {
    if (this == that)  { return true; }
    if (! (that instanceof mop.java.cmplx.Quad)) { return false; }
    final Quad x = (Quad) that;
    return
      ((z0() == x.z0()) && (z1() == x.z1()) &&
        (z2() == x.z2()) && (z3() == x.z3())); }

  //--------------------------------------------------------------------
  // construction
  //--------------------------------------------------------------------

  private Quad (final ZeroSimplex z0,
                final ZeroSimplex z1,
                final ZeroSimplex z2,
                final ZeroSimplex z3) {
    assert z0 != z1;
    assert z0 != z2;
    assert z0 != z3;
    assert z1 != z2;
    assert z1 != z3;
    assert z2 != z3;
    _count = Cell.counter();
    final ZeroSimplex[] z = minimalCircularPermutation(z0, z1, z2, z3);
    _z0 = z[0]; _z1 = z[1]; _z2 = z[2]; _z3 = z[3]; }

  public static final mop.java.cmplx.Quad make (final ZeroSimplex z0,
                                                final ZeroSimplex z1,
                                                final ZeroSimplex z2,
                                                final ZeroSimplex z3) {
    return new Quad(z0, z1, z2, z3); }

  //--------------------------------------------------------------------
} // end class
//--------------------------------------------------------------------
