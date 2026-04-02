package mop.java.jfx;

import clojure.lang.IFn;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.*;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeType;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import mop.java.cmplx.TwoSimplex;
import mop.java.geom.Point2U;
import mop.java.geom.mesh.TriangleMesh;
import org.apache.commons.geometry.euclidean.twod.Vector2D;
import org.locationtech.jts.geom.GeometryCollection;

import java.util.List;

import static mop.java.jfx.Util.subdivide4;


//---------------------------------------------------------------------

/**
 * Pane containing geographic layers, with standardized event handling.
 * <p>
 *
 * @author palisades dot lakes at gmail dot com
 * @version 2026-03-31
 */

@SuppressWarnings({ "unchecked", "unused" })
public final class WorldPane extends Pane {

  //-------------------------------------------------------------------
  // class slots and methods
  //-------------------------------------------------------------------

  // TODO: set this per layer or even per descendant node

  private static final double pixelStrokeWidth = 1;

  private static final void setWorldStrokeWidth (final Node node,
                                                 final double w) {
    switch (node) {
      case final Shape shape -> shape.setStrokeWidth(w);
      case final Parent parent ->
        parent.getChildrenUnmodifiable().forEach(
          (child) -> setWorldStrokeWidth(child, w));
      default -> {/* do nothing */} } }

  //-------------------------------------------------------------------
  // TODO: more general layer definitions

  private static final Group land () {
    final GeometryCollection polygons =
//      Util.readJTSGeometries("data/natural-earth/10m_physical/ne_10m_land.shp");
    Util.readJTSGeometries("data/natural-earth/50m_physical/ne_50m_land.shp");
//    Util.readJTSGeometries("data/natural-earth/ne_110m_land.shp");
    final Color fill = Color.web("#22990044");
    final Color stroke = Color.web("#a6611aFF");
    final Group group = Util.jfxNode(polygons, fill, stroke);
    group.setId("land");
    return group; }

  //-------------------------------------------------------------------

  private static final Group icosahedron () {
    final Group group = new Group();
    group.setId("icosahedron");
    final Color positiveStroke = Color.web("#2166ac", 0.5);
    //final Color positiveFill = Color.web("#d1e5f0", 0.2);
    final Color positiveFill = Color.web("#ffffff", 0.0);
    final Color negativeFill = Color.web("#fddbc7", 0.5);
    final Color negativeStroke = Color.web("#b2182b", 1);
    final TriangleMesh mesh =
      subdivide4(subdivide4(subdivide4(
      subdivide4(subdivide4(Util.u2CutIcosahedron()
      )))
      ));
    System.out.println("n faces: " + mesh.cmplx().faces().size());
    final List<TwoSimplex> faces = mesh.cmplx().faces();
    final IFn embedding = mesh.embedding();
    for (final TwoSimplex face : faces) {
      final Point2U u0 = (Point2U) embedding.invoke(face.z0());
      final Point2U u1 = (Point2U) embedding.invoke(face.z1());
      final Point2U u2 = (Point2U) embedding.invoke(face.z2());
      final Vector2D p0 = Util.toLonLat(u0);
      final Vector2D p1 = Util.toLonLat(u1);
      final Vector2D p2 = Util.toLonLat(u2);
      final Polygon triangle =
        new Polygon(p0.getX(), p0.getY(),
                    p1.getX(), p1.getY(),
                    p2.getX(), p2.getY());
      triangle.setId(face.toString());
      final double area = Util.signedArea(p0, p1, p2);
//    // strokeWidth 0.0 doesn't seem to work.
      // may need to invert scaling transform to get more-or-less
      // constant width on screen
      // problem seems to be related to jfx forcing windows dpi scaling
      // on its own coordinates.
      triangle.setStrokeWidth(1);
      triangle.setStrokeType(StrokeType.CENTERED);
      if (0.0 <= area) {
        triangle.setFill(positiveFill);
        triangle.setStroke(positiveStroke); }
      else {
        triangle.setFill(negativeFill);
        triangle.setStroke(negativeStroke); }
      group.getChildren().add(triangle); }
    return group; }

  //-------------------------------------------------------------------

  private static final Group makeWorldGroup () {
    final Group land = land();
    final Group icosahedron = icosahedron();
    // current rescaling fails with Pane rather than generic group
    final Group world = new Group(icosahedron, land);
    world.setId("world");
    // parent Pane handles events
    land.setFocusTraversable(false);
    icosahedron.setFocusTraversable(false);
    world.setFocusTraversable(false);
    world.setMouseTransparent(true);
    return world; }

//  private static final Group makeWorldGroup () {
//    final Group icosahedron = icosahedron();
//    // current rescaling fails with Pane rather than generic group
//    final Group world = new Group(icosahedron);
//    world.setId("world");
//    // parent Pane handles events
//    icosahedron.setFocusTraversable(false);
//    world.setFocusTraversable(false);
//    world.setMouseTransparent(true);
//    return world; }

  //-------------------------------------------------------------------
  // instance slots
  //-------------------------------------------------------------------
  // TODO: get rid of persistent mutable state?

  private double xDragOrigin = Double.NaN;
  private double yDragOrigin = Double.NaN;

  private final void origin (final MouseEvent e) {
    xDragOrigin = e.getSceneX();
    yDragOrigin = e.getSceneY(); }

  private final void drag (final MouseEvent e) {
    final double x = e.getSceneX();
    final double y = e.getSceneY();
    assert (!(Double.isNaN(xDragOrigin) || Double.isNaN(yDragOrigin)));
    setTranslateX(x - xDragOrigin + getTranslateX());
    setTranslateY(y - yDragOrigin + getTranslateY());
    xDragOrigin = x;
    yDragOrigin = y; }

  //-------------------------------------------------------------------
  // instance methods
  //-------------------------------------------------------------------
  // TODO: this is approximate, because changing the stroke width
  // changes the bounds used to compute it.

  private final void scaleStrokeWidth (final double pixelWidth) {
    final ObservableList<Node> children =
      this.getChildrenUnmodifiable();
    // TODO: handle multiple children
    assert 1 == children.size();
    final Parent root = (Parent) children.getFirst();
    final Bounds rootBounds = root.getBoundsInLocal();
    final double rw = rootBounds.getWidth();
    final double rh = rootBounds.getHeight();
    final Bounds parentBounds = this.getLayoutBounds();
    final double sw = parentBounds.getWidth();
    final double sh = parentBounds.getHeight();
    if ((0.0 < rw) && (0.0 < rh) && (0.0 < sw) && (0.0 < sh)) {
      final double s = Math.min(sw / rw, sh / rh);
      // NOTE: changing strokeWidth changes local/layout bounds
      setWorldStrokeWidth(root, pixelWidth / (s*getScaleX())); } }

  public final void rescale () {
    // NOTE: changing strokeWidth changes local/layout bounds
    scaleStrokeWidth(pixelStrokeWidth);
    final ObservableList<Node> children =
      this.getChildrenUnmodifiable();
    // TODO: handle multiple children
    assert 1 == children.size();
    final Parent child = (Parent) children.getFirst();
    final Bounds childBounds = child.getBoundsInLocal();
    final double cw = childBounds.getWidth();
    final double ch = childBounds.getHeight();
    final Bounds parentBounds = this.getLayoutBounds();
    final double sw = parentBounds.getWidth();
    final double sh = parentBounds.getHeight();
    if ((0.0 < cw) && (0.0 < ch) && (0.0 < sw) && (0.0 < sh)) {
      final Transform preTranslate =
        new Translate(-childBounds.getMinX(),
                      -childBounds.getMaxY());
      final double s = Math.min(sw / cw, sh / ch);
      final Transform scale = new Scale(s, -s);
      child.getTransforms().setAll(scale, preTranslate);
      //System.out.println("rescaled");
    }
//    else {
//      System.out.println("not rescaled");
//    }
  }

  //-------------------------------------------------------------------
  // hidden constructor
  //-------------------------------------------------------------------

  private WorldPane (final Group worldGroup) {
    super(worldGroup);
    this.setBackground(Background.fill(Color.web("#0000cc22")));
    this.setId(worldGroup.getId() + " pane");
    this.setFocusTraversable(true);

    this.setOnKeyPressed((final KeyEvent e) -> {
      e.consume();
      if (KeyCode.R == e.getCode()) {
        this.setTranslateX(0.0);
        this.setTranslateY(0.0);
        this.setScaleX(1.0);
        this.setScaleY(1.0); } } );

    // TODO: zoom holding mouse point fixed
    this.setOnScroll((final ScrollEvent e) -> {
      e.consume();
      if (!e.isDirect()) { // ignore touch events?
        final double relativeZoom = (e.getDeltaY() < 0) ? 0.9 : 1.1;
        final double s = relativeZoom * getScaleX();
        setScaleX(s);
        setScaleY(s);
        scaleStrokeWidth(pixelStrokeWidth);} } );

    this.setOnMousePressed((final MouseEvent e) -> {
      e.consume();
      // ignore touch events?
      if (!e.isSynthesized()) { origin(e); }});

    this.setOnMouseDragged((final MouseEvent e) -> {
      e.consume();
      // ignore touch events?
      if (!e.isSynthesized()) { drag(e); }});

    final ChangeListener changeListener = (obs, oldVal, newVal) -> {
      if (!oldVal.equals(newVal)) { this.rescale(); } };
    this.layoutBoundsProperty().addListener(changeListener);
  }

  public static final WorldPane make () {
    return new WorldPane(makeWorldGroup()); }

//---------------------------------------------------------------------
}
//---------------------------------------------------------------------
