package mop.java.cmplx;

import java.util.*;

/**
 * Abstract 2d quadrilateral cell complex.
 * Minimal representation: vertices and faces only.
 *
 * @author palisades dot lakes at gmail dot com
 * @version 2026-10-14
 */
public final class QuadComplex implements CellComplex {

  private final SortedSet<ZeroSimplex> _vertices;
  public final SortedSet<ZeroSimplex> vertices () { return _vertices; }

  private final SortedSet<Quad> _faces;
  public final SortedSet<Quad> faces () { return _faces; }

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
    _vertices = Collections.unmodifiableSortedSet(vertices);
    _faces = Collections.unmodifiableSortedSet(new TreeSet<>(faces)); }

  /** Accumulate the sorted vertices from the provided faces.
   * Do not retain a reference to <code>faces</code>.
   * Use unmodifiable lists internally.
   */
  public static final QuadComplex make (final Collection<Quad> faces) {
    return new QuadComplex(faces); }

  //--------------------------------------------------------------------
} // end class
//--------------------------------------------------------------------
