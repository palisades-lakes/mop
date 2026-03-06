package mop.java.geom.mesh;

import clojure.lang.IFn;
import mop.java.cmplx.CellComplex;
import mop.java.cmplx.SimplicialComplex2D;

/**
 * Embedded two dimensional simplicial complex. Minimal representation:
 * vertices and faces only.
 *
 * @author palisades dot lakes at gmail dot com
 * @version 2026-03-06
 */
public final class TriangleMesh implements Mesh {

  private final SimplicialComplex2D _cmplx;

  public final CellComplex cmplx () { return _cmplx; }

  private final IFn _embedding;

  public final IFn embedding () { return _embedding; }

  //--------------------------------------------------------------------
  // construction
  //--------------------------------------------------------------------

  private TriangleMesh (final SimplicialComplex2D cmplx,
                        final IFn embedding) {
    _cmplx = cmplx;
    _embedding = embedding;
  }

  /**
   * Accumulate the sorted vertices from the provided faces. Do not
   * retain a reference to <code>faces</code>. Use unmodifiable lists
   * internally.
   */
  public static final TriangleMesh make (final SimplicialComplex2D cmplx,
                                         final IFn embedding) {
    return new TriangleMesh(cmplx, embedding);
  }

  //--------------------------------------------------------------------
} // end class
//--------------------------------------------------------------------
