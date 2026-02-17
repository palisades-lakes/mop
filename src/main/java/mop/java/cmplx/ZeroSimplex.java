package mop.java.cmplx;

import java.util.List;

/**
 * AKA '(Abstract) Vertex'. The basic unit of identity used to build
 * simplicial and quad complexes, and embedded meshes from them. Most
 * code uses <code>int</code>s as a low overhead substitute, but this is
 * dangerous with multiple meshes that may share vertices, edges, etc.
 *
 * @author palisades dot lakes at gmail dot com
 * @version 2026-02-16
 */
public final class ZeroSimplex implements Cell {

  private final int _counter;

  private final int counter () { return _counter; }

  private final String _name;

  public final String name () { return _name; }

  //--------------------------------------------------------------------
  // Object
  //--------------------------------------------------------------------

  @Override
  public final String toString () { return _name; }

  @Override
  public final int hashCode () { return _counter; }

  @Override
  public final boolean equals (final Object that) {
    return (this == that);
  }

  //--------------------------------------------------------------------
  // Comparable
  //--------------------------------------------------------------------

  /**
   * ordering will be match order of creation within a thread. will be
   * used to identify which point goes with which zero simplex in
   * embeddings.
   */
  @Override
  public final int compareTo (final Object that) {
    return _counter - ((ZeroSimplex) that).counter();
  }

  //--------------------------------------------------------------------
  // Cell
  //--------------------------------------------------------------------

  /**
   * Note: returns immutable list.
   */
  @Override
  public final List vertices () { return List.of(this); }

  @Override
  public final boolean isOriented () { return true; }

  @Override
  public final boolean equivalent (final Cell that) {
    return this == that;
  }


  //--------------------------------------------------------------------
  // construction
  //--------------------------------------------------------------------

  private ZeroSimplex (final int counter,
                       final String name) {
    _counter = counter;
    _name = name;
  }

  public static final ZeroSimplex make (final int counter,
                                        final String name) {
    return new ZeroSimplex(counter, name);
  }
  //--------------------------------------------------------------------
} // end class
//--------------------------------------------------------------------
