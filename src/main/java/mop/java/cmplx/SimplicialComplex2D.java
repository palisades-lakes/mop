package mop.java.cmplx;

import java.util.*;

/**
 * Two dimensional simplicial complex.
 * Minimal representation: vertices and faces only.
 *
 * @author palisades dot lakes at gmail dot com
 * @version 2026-10-14
 */
public final class SimplicialComplex2D implements CellComplex {

  // TODO: ordered sets
  private final SortedSet<ZeroSimplex> _vertices;
  public final SortedSet<ZeroSimplex> vertices () { return _vertices; }

  private final SortedSet<TwoSimplex> _faces;
  public final SortedSet<TwoSimplex> faces () { return _faces; }

  //--------------------------------------------------------------------
  // Object
  //--------------------------------------------------------------------

  @Override
  public final String toString () {
    return
      "vertices:\n" + vertices().toString()
        + "\nfaces:\n" + faces().toString(); }

  //--------------------------------------------------------------------
  // construction
  //--------------------------------------------------------------------
  /** Accumulate the sorted vertices from the provided faces.
   */

  private SimplicialComplex2D (final Collection<TwoSimplex> faces) {
    final SortedSet<ZeroSimplex> vertices = new TreeSet<>();
    for (final TwoSimplex f : faces) {
      vertices.add(f.z0());
      vertices.add(f.z1());
      vertices.add(f.z2()); }
    _vertices = Collections.unmodifiableSortedSet(vertices);
    _faces = Collections.unmodifiableSortedSet(new TreeSet<>(faces)); }

  /** Accumulate the sorted vertices from the provided faces.
   * Do not retain a reference to <code>faces</code>.
   * Use unmodifiable lists internally.
   */

  public static final SimplicialComplex2D
  make (final Collection<TwoSimplex> faces) {
    return new SimplicialComplex2D(faces); }

  //--------------------------------------------------------------------
} // end class
//--------------------------------------------------------------------
