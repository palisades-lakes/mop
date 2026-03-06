package mop.java.geom.mesh;

import clojure.lang.IFn;
import mop.java.cmplx.CellComplex;

/**
 * Embedded cell complex.
 *
 * @author palisades dot lakes at gmail dot com
 * @version 2026-03-06
 */

public interface Mesh {
  /** Defines the (micro)topology of the mesh. */
  CellComplex cmplx ();
  /** Maps zero simplices (vertices) to points in some space. */
  IFn embedding ();
}
