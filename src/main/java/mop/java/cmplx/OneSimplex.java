package mop.java.cmplx;

import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * AKA '(Abstract) Edge'. An ordered pair of zero simplexes.
 *
 * @author palisades dot lakes at gmail dot com
 * @version 2026-02-18
 */
public final class OneSimplex implements Cell {

  private final int _count;

  private final int count () { return _count; }

  private final ZeroSimplex _z0;

  public final ZeroSimplex z0 () { return _z0; }

  private final ZeroSimplex _z1;

  public final ZeroSimplex z1 () { return _z1; }

  //--------------------------------------------------------------------
  // Object
  //--------------------------------------------------------------------

  @Override
  public final String toString () { return z0() + "->" + z1(); }

  @Override
  public final int hashCode () { return _count; }

//  @Override
//  public final boolean equals (final Object that) {
//    assert that instanceof OneSimplex;
//    return (this == that); }

  //--------------------------------------------------------------------
  // Comparable
  //--------------------------------------------------------------------

  /**
   * Ordering will be match order of creation within a thread. will be
   * used to identify which point goes with which zero simplex in
   * embeddings.
   * TODO: lexicographic ordering via vertices?
   */
  @Override
  public final int compareTo (final @NonNull Object that) {
    assert that instanceof OneSimplex;
    return _count - ((OneSimplex) that).count(); }

  //--------------------------------------------------------------------
  // Cell
  //--------------------------------------------------------------------

  /**
   * Note: returns immutable list.
   */
  @Override
  public final List vertices () { return List.of(z0(), z1()); }

  @Override
  public final boolean isOriented () { return true; }

  @Override
  public final boolean equivalent (final Cell that) {
    if (this == that) { return true; }
    if (!(that instanceof final OneSimplex x)) { return false; }
    return (z0() == x.z0()) && (z1() == x.z1()); }

  //--------------------------------------------------------------------
  // construction
  //--------------------------------------------------------------------

  private OneSimplex (final ZeroSimplex z0,
                      final ZeroSimplex z1) {
    assert z0 != z1;
    _count = Cell.counter(); _z0 = z0; _z1 = z1; }

  public static final OneSimplex make (final ZeroSimplex z0,
                                       final ZeroSimplex z1) {
    return new OneSimplex(z0, z1); }

  //--------------------------------------------------------------------
} // end class
//--------------------------------------------------------------------
