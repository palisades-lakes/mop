package mop.java.imageio;

import javax.imageio.IIOImage;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RasterOp;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import java.util.Hashtable;

/** Trivial operation to reduce image resolution.
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
 * <br>
 * Each low resolution pixel corresponds to a square/rectangle of
 * high resolution pixels. Could use some statistic
 * (mean, median, mode, ...) rather than just picking one.
 * Could fit some piecewise smooth surface as well, tho that's probably
 * overkill.
 * Could allow a variety of high res neighborhood definitions...
 * <br>
 * Careful thought is particularly required for color images;
 * interpolating in RGB is likely to give undesirable results.
 * HSV probably better, if not some coordinate system to approximates
 * perceptual distance.
 * <br>
 * Immutable, threadsafe.
 */
public final class MaxDimensionOp
  implements RasterOp, BufferedImageOp {

  //-------------------------------------------------------------------
  // slots
  //-------------------------------------------------------------------
  // TODO: will sometimes multiplying by srcDim/maxDim and sometimes by
  // maxDim/srcDim cause problems double-int rounding problems?

  private final int _maxDimension;

  public final int maxDimension () { return _maxDimension; }

  //-------------------------------------------------------------------
  // local methods
  //-------------------------------------------------------------------

  private static final boolean equalColorModels (final ColorModel cm0,
                                                 final ColorModel cm1) {
    return
      ((cm0 == cm1)
        || cm0.equals(cm1)
        || ((cm0.getClass() == cm1.getClass())
        && (cm0.getColorSpace().equals(cm1.getColorSpace()))
        && Arrays.equals(cm0.getComponentSize(), cm1.getComponentSize())
        && (cm0.getNumComponents() == cm1.getNumComponents())
        && (cm0.getNumColorComponents() == cm1.getNumColorComponents())
        && (cm0.getPixelSize() == cm1.getPixelSize())
        && (cm0.getTransferType() == cm1.getTransferType())
        && (cm0.getTransparency() == cm1.getTransparency())
        && (cm0.hasAlpha()  == cm1.hasAlpha())
        && (cm0.isAlphaPremultiplied() == cm1.isAlphaPremultiplied())));

  }
  //-------------------------------------------------------------------
  // RasterOp methods
  //-------------------------------------------------------------------
  /** Rendering hints are ignored.
   */

  @Override
  public final RenderingHints getRenderingHints () {
    // TODO: return empty instance of RenderingHints?
    return null; }

  @Override
  public final Rectangle2D getBounds2D (final Raster src) {
    final int w = src.getWidth();
    final int h = src.getHeight();
    final double s = ((double) maxDimension()/Math.max(w,h));
    return new Rectangle2D.Double(0.0,0.0,s*w,s*h); }

  // TODO: Force create for each source image (size)?
  @Override
  public final Point2D getPoint2D (final Point2D srcPt,
                                   final Point2D dstPt) {
    throw new UnsupportedOperationException(
      "Transform depends on source image."); }

  @Override
  public final WritableRaster
  createCompatibleDestRaster (final Raster src) {
    final Rectangle2D r = getBounds2D(src);
    return src.createCompatibleWritableRaster((int)r.getX(),
                                              (int)r.getY(),
                                              (int)r.getWidth(),
                                              (int)r.getHeight()); }

  /** If src is small enough already, return null.
   */
  @Override
  public final WritableRaster filter (final Raster src,
                                      final WritableRaster dest) {
    final int win = src.getWidth();
    final int hin = src.getHeight();
    final double s = ((double) maxDimension()/Math.max(win,hin));
    if (1.0 <= s) { return null; }
    final int w = (int) (s*src.getWidth());
    final int h = (int) (s*src.getHeight());
    final WritableRaster dst =
      ((null == dest)
       ? src.createCompatibleWritableRaster(w,h)
       : dest);
    assert ((w == dst.getWidth()) && (h == dst.getHeight()));
    // TODO: not sure what we are assuming about the images
    //assert (DataBuffer.TYPE_FLOAT == src.getTransferType());
    //assert (DataBuffer.TYPE_FLOAT == src.getDataBuffer().getDataType());
    //assert (1 == src.getNumBands());
    assert (src.getSampleModel() instanceof PixelInterleavedSampleModel);
    // simple, fast enough for occasional use
    final int nb = dst.getNumBands();
    final double r = 1.0/s;
    for (int x=0;x<w;x++) {
      for (int y=0;y<h;y++) {
        for (int b=0;b<nb;b++) {
          final int xSrc = (int) (x*r);
          final int ySrc = (int) (y*r);
          dst.setSample(x,y,b,src.getSample(xSrc,ySrc,b)); } } }

    return dst; }

  //-------------------------------------------------------------------
  // BufferedImageOp methods
  //-------------------------------------------------------------------

  @Override
  public final Rectangle2D getBounds2D (final BufferedImage src) {
    return getBounds2D(src.getRaster()); }

  @Override
  public final BufferedImage
  createCompatibleDestImage (final BufferedImage src,
                             final ColorModel destCM) {
    assert null != src;
    final Hashtable<String,Object> props = new Hashtable<>();
    final String[] keys = src.getPropertyNames();
    if (null != keys) {
      for (final String key : keys) {
        props.put(key, src.getProperty(key)); } }
    // TODO: is this right?
    // TODO: Is the 'compatible' raster assuming the src color model?
    // TODO ColorModel.equals() probably isn't right
    final ColorModel dstCM =
      ((null == destCM) ? src.getColorModel() : destCM);
    assert (equalColorModels(destCM,src.getColorModel()));
    return new BufferedImage(
      dstCM,
      createCompatibleDestRaster(src.getData()),
      src.isAlphaPremultiplied(),
      props); }

  /** If src is small enough already, return null.
   */
  @Override
  public final BufferedImage filter (final BufferedImage src,
                                     final BufferedImage dest) {
    final int win = src.getWidth();
    final int hin = src.getHeight();
    final double s = ((double) maxDimension()/Math.max(win,hin));
    if (1.0 <= s) { return null; }
    final int w = (int) (s*src.getWidth());
    final int h = (int) (s*src.getHeight());
    final BufferedImage dst =
      ((null == dest)
       ? createCompatibleDestImage(src,src.getColorModel())
       : dest);
    assert ((w == dst.getWidth()) && (h == dst.getHeight()));
    filter(src.getData(),dst.getRaster());
    return dst; }

  //-------------------------------------------------------------------
  // extend filter to IIOImage
  // TODO: IIOImageOp interface?
  // TODO: add mutable IIOImage as dest argument?
  //-------------------------------------------------------------------

  /** If src small enough already, return null.
   */
  public final IIOImage filter (final IIOImage src) {
    // TODO: handle non-BufferedImages? might just have a Raster.
    final BufferedImage in = (BufferedImage) src.getRenderedImage();
    final BufferedImage out = filter(in,null);
    if (null == out) { return null; }
    return new IIOImage(out, src.getThumbnails(), src.getMetadata()); }

  //-------------------------------------------------------------------
  // constructor
  //-------------------------------------------------------------------

  private MaxDimensionOp (final int maxDim) {
    _maxDimension = maxDim; }

  public static final MaxDimensionOp
  make (final int maxDim) {
    return new MaxDimensionOp(maxDim); }

  //-------------------------------------------------------------------
}
//-------------------------------------------------------------------
