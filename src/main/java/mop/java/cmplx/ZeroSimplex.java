package mop.java.cmplx;

import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * AKA '(Abstract) Vertex'. The basic unit of identity used to build
 * simplicial and quad complexes, and embedded meshes from them. Most
 * code uses <code>int</code>s as a low overhead substitute, but this is
 * dangerous with multiple meshes that may share vertices, edges, etc.
 *
 * @author palisades dot lakes at gmail dot com
 * @version 2026-02-18
 */
public final class ZeroSimplex implements Cell {
  //--------------------------------------------------------------------

  private final int _count;
  private final int count () { return _count; }

  private final String _name;

  public final String name () { return _name; }

  //--------------------------------------------------------------------
  // Object
  //--------------------------------------------------------------------

  @Override
  public final String toString () { return _name; }

  @Override
  public final int hashCode () { return _count; }

  @Override
  public final boolean equals (final Object that) {
    assert that instanceof ZeroSimplex;
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
  public final int compareTo (final @NonNull Object that) {
    assert that instanceof ZeroSimplex;
    return _count - ((ZeroSimplex) that).count(); }

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
    return this == that; }

  //--------------------------------------------------------------------
  // construction
  //--------------------------------------------------------------------

  private ZeroSimplex (final String name) {
    _count = Cell.counter(); _name = name; }

  public static final ZeroSimplex make (final String name) {
    return new ZeroSimplex(name);
  }
  //--------------------------------------------------------------------
} // end class
//--------------------------------------------------------------------
