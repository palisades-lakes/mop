package mop.java.cmplx;

/**
 * AKA '(Abstract) Edge'. An ordered pair of zero simplexes.
 *
 * @author palisades dot lakes at gmail dot com
 * @version 2026-02-16
 */
public final class Quad implements Cell {

  private final int _counter;
  private final int counter () { return _counter; }

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
  public final int hashCode () { return _counter; }

  @Override
  public final boolean equals (final Object that) {
    return (this == that); }

  //--------------------------------------------------------------------
  // Comparable
  //--------------------------------------------------------------------

  /** Ordering will be match order of creation within a thread. will be
   * used to identify which point goes with which zero simplex in
   * embeddings.
   * TODO: lexicographic ordering via vertices?
   */
  @Override
  public final int compareTo (final Object that) {
    return _counter - ((Quad) that).counter(); }

  //--------------------------------------------------------------------
  // Cell
  //--------------------------------------------------------------------
  /** Note: returns immutable list. */
  @Override
  public final java.util.List vertices () {
    return java.util.List.of(z0(), z1(), z2(), z3()); }

  @Override
  public final boolean isOriented () { return true; }

  @Override
  public final boolean equivalent (final Cell that) {
    if (this == that)  { return true; }
    if (! (that instanceof mop.java.cmplx.Quad)) { return false; }
    final mop.java.cmplx.Quad x = (mop.java.cmplx.Quad) that;
    return
      (z0() == x.z0())
      && (z1() == x.z1())
        && (z2() == x.z2())
        && (z3() == x.z3()); }

  //--------------------------------------------------------------------
  // construction
  //--------------------------------------------------------------------

  private Quad (final int counter,
                final ZeroSimplex z0,
                final ZeroSimplex z1,
                final ZeroSimplex z2,
                final ZeroSimplex z3) {
    _counter = counter; _z0 = z0; _z1 = z1; _z2 = z2; _z3 = z3; }

  public static final mop.java.cmplx.Quad make (final int counter,
                                                final ZeroSimplex z0,
                                                final ZeroSimplex z1,
                                                final ZeroSimplex z2,
                                                final ZeroSimplex z3) {
    return new mop.java.cmplx.Quad(counter, z0, z1, z2, z3); }

  //--------------------------------------------------------------------
} // end class
//--------------------------------------------------------------------
