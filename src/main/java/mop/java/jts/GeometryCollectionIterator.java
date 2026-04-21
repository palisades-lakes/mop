package mop.java.jts;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;

import java.util.Iterator;
import java.util.NoSuchElementException;

//---------------------------------------------------------------------
/** <code>org.locationtech.jts.geom.GeometryCollectionIterator</code>
 * walks the geometry tree, returning the collection itself,
 * subcollections, recursively down to the leaves, the non-collection
 * geoemtries.
 * <br>
 * This is a simpler, top level iterator over the values returned by
 * <code>getGeometryN(i)</code>.
 * <br>
 * WARNING: GeometryCollections are mutable.
 * Shared mutable objects are common throughout JTS,
 * so the cost of a deep copy, or even a shallow copy,
 * is probably not worthwhile.
 * <br>
 * WARNING: not thread safe because unsynchronized access to
 * <code>_next</code>. Not threadsafe anyway due to abundant shared
 * mutable state, so syncing doesn't seem worthwhile.
 *
 * @author palisades dot lakes at gmail dot com
 * @version 2026-04-20
 */
public final class GeometryCollectionIterator
  implements Iterator<Geometry> {

  private final GeometryCollection _geometryCollection;
  private int _next = 0;

  //--------------------------------------------------------------------
  // Iterator methods
  //--------------------------------------------------------------------

  @Override
  public final boolean hasNext () {
    return _next < _geometryCollection.getNumGeometries(); }

  @Override
  public Geometry next () {
    if (hasNext()) {
      return _geometryCollection.getGeometryN(_next++); }
    throw new NoSuchElementException(); }

  //--------------------------------------------------------------------
  // construction
  //--------------------------------------------------------------------

  private GeometryCollectionIterator (final GeometryCollection gc) {
    super();
    _geometryCollection = gc; }

  public static final GeometryCollectionIterator
  make (final GeometryCollection gc) {
    return new GeometryCollectionIterator(gc); }

  //--------------------------------------------------------------------
} // end class
//--------------------------------------------------------------------

