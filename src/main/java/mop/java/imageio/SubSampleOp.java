package mop.java.imageio;

import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.RasterOp;
import java.awt.image.WritableRaster;

/** Trivial method to reduce image resolution.
 *  assigning to each low resolution pixel the value of the single
 *  closest high resolution pixel in scaled up pixel coordinates.
 * <br>
 *  Implementing this because I can't find a rescaling op that handles
 *  Float32 Grayscale, TYPE_CUSTOM images that are common, at least
 *  from NASA and NOAA for elevation data, etc.
 * <br>
 * TODO: Implement more intelligent algorithms,
 * especially for increasing resolution as well as reducing.
 * Requires first thinking thru how to measure quality of output.
 */
public final class SubSampleOp
  implements RasterOp, BufferedImageOp {

  //-------------------------------------------------------------------
  // slots
  //-------------------------------------------------------------------

  private final double _lowToHighScale;
  public final double lowToHighScale () { return _lowToHighScale; }

  //-------------------------------------------------------------------
  // RasterOp methods
  //-------------------------------------------------------------------
  /** Rendering hints are ignored.
   */

  @Override
  public final RenderingHints getRenderingHints () { return null; }

  @Override
  public final Rectangle2D getBounds2D (final Raster src) {
    return new Rectangle2D.Double(
      0.0,
      0.0,
      src.getWidth() / lowToHighScale(),
      src.getHeight() / lowToHighScale()); }

  @Override
  public final Point2D getPoint2D (final Point2D srcPt,
                                   final Point2D dstPt) {
    final double x = srcPt.getX() / lowToHighScale();
    final double y = srcPt.getY() / lowToHighScale();
    if (null == dstPt) { return new Point2D.Double(x,y); }
    dstPt.setLocation(x, y);
    return dstPt; }

  @Override
  public final WritableRaster createCompatibleDestRaster (final Raster src) {
    final Rectangle2D r = getBounds2D(src);
    return src.createCompatibleWritableRaster((int)r.getX(),
                                              (int)r.getY(),
                                              (int)r.getWidth(),
                                              (int)r.getHeight()); }

  @Override
  public final WritableRaster filter (final Raster src,
                                      final WritableRaster dest) {
    final int w = (int) (src.getWidth() / lowToHighScale());
    final int h = (int) (src.getHeight() / lowToHighScale());
    final WritableRaster dst =
      ((null == dest)
       ? src.createCompatibleWritableRaster(w,h)
       : dest);
    assert ((w == dst.getWidth()) && (h == dst.getHeight()));
    return dst;
  }

  //-------------------------------------------------------------------
  // BufferedImageOp methods
  //-------------------------------------------------------------------

  @Override
  public final Rectangle2D getBounds2D (final BufferedImage src) {
    return getBounds2D(src.getRaster()); }

  @Override
  public final BufferedImage createCompatibleDestImage (
    final BufferedImage src,
    final ColorModel destCM) {
    return null;
  }

  @Override
  public final BufferedImage filter (final BufferedImage src,
                                     final BufferedImage dest) {
    return null;
  }

  //-------------------------------------------------------------------
  // constructor
  //-------------------------------------------------------------------

  private  SubSampleOp (final double lowToHighScale) {
    _lowToHighScale = lowToHighScale; }

  public  static final SubSampleOp make (final double lowToHighScale) {
    return new  SubSampleOp(lowToHighScale); }

  //-------------------------------------------------------------------
}
//-------------------------------------------------------------------
