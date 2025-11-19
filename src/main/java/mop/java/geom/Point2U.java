package mop.java.geom;

//----------------------------------------------------------------------

import org.apache.commons.geometry.core.Point;
import org.apache.commons.geometry.core.internal.SimpleTupleFormat;
import org.apache.commons.geometry.euclidean.threed.Vector3D;
import org.apache.commons.geometry.spherical.twod.Point2S;
import org.jetbrains.annotations.NotNull;

//----------------------------------------------------------------------

/**
 * Wrap <b>R</b><sup>2</sup> around the unit sphere, such that
 * <pre>
 * azimuth = u mod 2PI
 * polar = v mod PI
 * </pre>
 * Immutable;
 *
 * @author palisades dot lakes at gmail dot com
 * @version 2025-11-13
 */

@SuppressWarnings("PatternVariableCanBeUsed")
public record Point2U (double _u, double _v)

  implements Point<Point2U> {

  //--------------------------------------------------------------------

  public final double getU () { return _u; }

  public final double getV () { return _v; }

  //--------------------------------------------------------------------

  @Override
  public final double distance (final Point2U p) {
    return toPoint2S().distance(p.toPoint2S()); }

  @Override
  public final int getDimension () { return 2; }

  @Override
  public final boolean isNaN () {
    return Double.isNaN(getU()) || Double.isNaN(getV()); }

  @Override
  public final boolean isInfinite () {
    return Double.isInfinite(getU()) || Double.isInfinite(getV()); }

  @Override
  public final boolean isFinite () { return !isInfinite(); }

  //--------------------------------------------------------------------
  // Object
  //--------------------------------------------------------------------

  @Override
  public final @NotNull String toString () {
    return SimpleTupleFormat.getDefault().format(getU(), getV()); }

  @Override
  public final boolean equals (final Object other) {
    if (this == other) { return true; }
    if (!(other instanceof Point2U)) { return false; }
    //noinspection PatternVariableCanBeUsed
    final Point2U rhs = (Point2U) other;
    if (rhs.isNaN()) { return this.isNaN(); }
    return
      (Double.compare(_u,rhs._u) == 0)
      &&
      (Double.compare(_v, rhs._v) == 0); }

  @Override
  public final int hashCode() {
    if (isNaN()) { return 542; }
    return 134 * (37 * Double.hashCode(_u) +  Double.hashCode(_v)); }

  //--------------------------------------------------------------------
  // construction
  //--------------------------------------------------------------------

  public static final Point2U of (final double u,
                                  final double v) {
    return new Point2U(u, v); }

  //--------------------------------------------------------------------

  public final Point2S toPoint2S () {
    return Point2S.of(getU(), getV()); }

  public static final Point2U of (final Point2S p) {
    return of(p.getAzimuth(), p.getPolar()); }

  @SuppressWarnings("unused")
  public static final Point2U of (final Vector3D v) {
    return of(Point2S.from(v)); }

  //--------------------------------------------------------------------

  public static final Point2U MINUS_K = of(Point2S.MINUS_K);
  public static final Point2U PLUS_K = of(Point2S.PLUS_K);

  //--------------------------------------------------------------------
} // end class
//----------------------------------------------------------------------

