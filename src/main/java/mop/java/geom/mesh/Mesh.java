package mop.java.geom.mesh;

import clojure.lang.IFn;
import mop.java.cmplx.Cell;
import mop.java.cmplx.CellComplex;
import mop.java.cmplx.ZeroSimplex;

import java.util.SortedSet;

/** Embedded cell complex.
 *
 * @author palisades dot lakes at gmail dot com
 * @version 2026-10-14
 */

public interface Mesh {

  /** Defines the (micro)topology of the mesh. */
  public CellComplex cmplx ();

  /** Maps zero simplices (vertices) to points in some space. */
  public IFn embedding ();

  public default SortedSet<ZeroSimplex> vertices () {
    return cmplx().vertices(); }

  /** Return an unmodifiable set. */
  public default SortedSet<? extends Cell> faces () {
    return cmplx().faces(); }

  //--------------------------------------------------------------------
} // end interface
//--------------------------------------------------------------------
