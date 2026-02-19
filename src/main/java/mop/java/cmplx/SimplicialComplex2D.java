package mop.java.cmplx;

import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Two dimensional simplicial complex.
 * Minimal representation: vertices and faces only.
 *
 * @author palisades dot lakes at gmail dot com
 * @version 2026-02-18
 */
public final class SimplicialComplex2D implements CellComplex {

  private final List _vertices;
  public final List vertices () { return _vertices; }

  private final List _faces;
  public final List faces () { return _faces; }

  //--------------------------------------------------------------------
  // construction
  //--------------------------------------------------------------------
  /** Accumulate the sorted vertices from the provided faces.
   */

  private SimplicialComplex2D (final Collection faces) {
    final SortedSet<ZeroSimplex> vertices = new TreeSet<>();
    for (final Object f : faces) {
      final TwoSimplex f2 = (TwoSimplex) f;
      vertices.add(f2.z0());
      vertices.add(f2.z1());
      vertices.add(f2.z2()); }
    _vertices = List.copyOf(vertices);
    _faces = List.copyOf(faces); }

  /** Accumulate the sorted vertices from the provided faces.
   * Do not retain a reference to <code>faces</code>.
   * Use unmodifiable lists internally.
   */
  public static final SimplicialComplex2D make (final Collection faces) {
    return new SimplicialComplex2D(faces); }

  //--------------------------------------------------------------------
} // end class
//--------------------------------------------------------------------
