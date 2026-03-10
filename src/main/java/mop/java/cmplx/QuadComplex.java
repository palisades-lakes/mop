package mop.java.cmplx;

import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Abstract 2d quadrilateral cell complex.
 * Minimal representation: vertices and faces only.
 *
 * @author palisades dot lakes at gmail dot com
 * @version 2026-03-10
 */
public final class QuadComplex implements CellComplex {

  private final List<ZeroSimplex> _vertices;
  public final List<ZeroSimplex> vertices () { return _vertices; }

  private final List<Quad> _faces;
  public final List<Quad> faces () { return _faces; }

  //--------------------------------------------------------------------
  // construction
  //--------------------------------------------------------------------
  /** Accumulate the sorted vertices from the provided faces.
   */

  private QuadComplex (final Collection<Quad> faces) {
    final SortedSet<ZeroSimplex> vertices = new TreeSet<>();
    for (final Quad f : faces) {
      vertices.add(f.z0());
      vertices.add(f.z1());
      vertices.add(f.z2());
      vertices.add(f.z3()); }
    _vertices = List.copyOf(vertices);
    _faces = List.copyOf(faces); }

  /** Accumulate the sorted vertices from the provided faces.
   * Do not retain a reference to <code>faces</code>.
   * Use unmodifiable lists internally.
   */
  public static final QuadComplex make (final Collection<Quad> faces) {
    return new QuadComplex(faces); }

  //--------------------------------------------------------------------
} // end class
//--------------------------------------------------------------------
