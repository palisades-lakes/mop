package mop.java.geom.mesh;

import clojure.lang.IFn;
import mop.java.cmplx.CellComplex;
import mop.java.cmplx.QuadComplex;

/**
 * Embedded two dimensional quad complex. Minimal representation:
 * vertices and faces only.
 *
 * @author palisades dot lakes at gmail dot com
 * @version 2026-03-06
 */
public final class QuadMesh implements Mesh {

  private final QuadComplex _cmplx;

  public final CellComplex cmplx () { return _cmplx; }

  private final IFn _embedding;

  public final IFn embedding () { return _embedding; }

  //--------------------------------------------------------------------
  // construction
  //--------------------------------------------------------------------

  private QuadMesh (final QuadComplex cmplx,
                    final IFn embedding) {
    _cmplx = cmplx;
    _embedding = embedding;
  }

  /**
   * Accumulate the sorted vertices from the provided faces. Do not
   * retain a reference to <code>faces</code>. Use unmodifiable lists
   * internally.
   */
  public static final QuadMesh make (final QuadComplex cmplx,
                                     final IFn embedding) {
    return new QuadMesh(cmplx, embedding);
  }

  //--------------------------------------------------------------------
} // end class
//--------------------------------------------------------------------
