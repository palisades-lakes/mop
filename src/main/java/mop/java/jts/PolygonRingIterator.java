package mop.java.jts;

import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;

import java.util.Iterator;
import java.util.NoSuchElementException;

//---------------------------------------------------------------------

/** Iterator over the LinearRings of a JTS Polygon.
 * First ring returned is the exterior, followed by the
 * interior ones, if any.
 * <br>
 * WARNING: Polygons and LinearRings are mutable.
 * Shared mutable objects are common throughout JTS,
 * so the cost of a deep copy, or even a shallow copy,
 * is probably not worthwhile.
 *
 * @author palisades dot lakes at gmail dot com
 * @version 2026-04-18
 */
public final class PolygonRingIterator
  implements Iterator<LinearRing> {

  private final Polygon _polygon;
  private int _next = -1;

  //--------------------------------------------------------------------
  // Iterator methods
  //--------------------------------------------------------------------

  @Override
  public boolean hasNext () {
    return _next < _polygon.getNumInteriorRing(); }

  @Override
  public LinearRing next () {
    if (hasNext()) {
      if (-1 == _next) {
        _next++; return _polygon.getExteriorRing(); }
      return _polygon.getInteriorRingN(_next++); }
    throw new NoSuchElementException(); }

  //--------------------------------------------------------------------
  // construction
  //--------------------------------------------------------------------

  private PolygonRingIterator (final Polygon polygon) {
    super();
    _polygon = polygon; }

  public static final PolygonRingIterator
  make (final Polygon polygon) {
    return new PolygonRingIterator(polygon); }

  //--------------------------------------------------------------------
} // end class
//--------------------------------------------------------------------

