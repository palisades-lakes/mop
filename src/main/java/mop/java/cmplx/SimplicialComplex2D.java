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

  // TODO: ordered set
  private final List<ZeroSimplex> _vertices;
  public final List<ZeroSimplex> vertices () { return _vertices; }

  private final List<TwoSimplex> _faces;
  public final List<TwoSimplex> faces () { return _faces; }

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
    _vertices = List.copyOf(vertices);
    _faces = List.copyOf(faces); }

  /** Accumulate the sorted vertices from the provided faces.
   * Do not retain a reference to <code>faces</code>.
   * Use unmodifiable lists internally.
   */
  public static final SimplicialComplex2D make (
    final Collection<TwoSimplex> faces) {
    return new SimplicialComplex2D(faces); }

  //--------------------------------------------------------------------
} // end class
//--------------------------------------------------------------------
