package mop.java.jfx;

// mvn -nsu clean install
// mvn -q -o -nsu -DskipTests -Dclojure-maven-plugin.clojure.test
// .skip=true -Dmaven.test.skip=true install & jfx mop.java.jfx.Main

import clojure.lang.IFn;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.*;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.StrokeType;
import javafx.stage.Stage;
import mop.java.cmplx.TwoSimplex;
import mop.java.geom.Point2U;
import mop.java.geom.mesh.TriangleMesh;
import org.apache.commons.geometry.euclidean.twod.Vector2D;
import org.locationtech.jts.geom.GeometryCollection;

import java.util.List;

//---------------------------------------------------------------------

/**
 * Experiment with icosahedron and map display via jfx.
 * <p>
 *
 * @author palisades dot lakes at gmail dot com
 * @version 2026-03-26
 */

@SuppressWarnings({ "unchecked", "unused" })
public final class IcosahedronS2 extends Application {

  private final Pane worldPane;

  private final void rescale () {
    Platform.runLater(() -> Util.rescale(worldPane));
  }

  private final Scene scene;

  //-------------------------------------------------------------------

  private static final Group land () {
    final GeometryCollection polygons =
      Util.readJTSGeometries("data/natural-earth/ne_110m_land.shp");
    final Color fill = Color.web("#22990044");
    final Color stroke = Color.web("#a6611aFF");
    final Group group = Util.jfxNode(polygons, fill, stroke);
    group.setId("land");
    return group;
  }

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
      Util.subdivide4(
//        subdivide4(
//          subdivide4(
//            Util.subdivide4(
        Util.u2CutIcosahedron()
//                      )
//                    )
        //)
                     );
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
        triangle.setStroke(positiveStroke);
      }
      else {
        triangle.setFill(negativeFill);
        triangle.setStroke(negativeStroke);
      }
      group.getChildren().add(triangle);
    }
    return group;
  }

  //-------------------------------------------------------------------

  private static final Parent makeWorld () {
    final Group land = land();
    final Group icosahedron = icosahedron();
    // current rescaling fails with Pane
    final Group world = new Group(icosahedron, land);
    world.setId("world");
    return world;
  }

  private static Pane makePane () {
    final Parent world = makeWorld();
    final Pane pane = new Pane(world);
    pane.setBackground(Background.fill(Color.web("#0000cc22")));
    pane.setId(world.getId() + " pane");

    pane.setFocusTraversable(true);

    pane.setOnKeyPressed((final KeyEvent e) -> {
      //System.out.println(pane + " keyPressed " + e.getCode());
      if (KeyCode.R == e.getCode()) {
        pane.setTranslateX(0.0);
        pane.setTranslateY(0.0);
        pane.setScaleX(1.0);
        pane.setScaleY(1.0); } } );

    pane.setOnZoom((final ZoomEvent e) -> {
      final double zoom = e.getTotalZoomFactor();
      pane.setScaleX(zoom);
      pane.setScaleY(zoom); } );

    pane.setOnScroll((final ScrollEvent e) -> {
                         //      final double zoom = e.getTotalDeltaY();
      System.out.println(pane + " scroll " + e.getDeltaY() + " : " + e.getTotalDeltaY());
//      pane.setScaleX(zoom);
//      pane.setScaleY(zoom);
    } );

    pane.setOnMouseDragged((final MouseEvent e) -> {
      //System.out.println(pane + " dragged " + e.getX() + "," + e.getY());
      if (!e.isSynthesized()) {
        pane.setTranslateX(e.getX());
        pane.setTranslateY(e.getY());
      }});

    final ChangeListener changeListener =
      (obs, oldVal, newVal) -> {
        if (!oldVal.equals(newVal)) { Util.rescale(pane); } };
    pane.layoutBoundsProperty().addListener(changeListener);
    return pane;
  }

  //-------------------------------------------------------------------

  private static Scene makeScene (final Pane pane,
                                  final double w,
                                  final double h) {
    BorderPane.setMargin(pane, new Insets(32));
    final BorderPane wrapper = new BorderPane(pane);
    final Scene scene =
      new Scene(wrapper, w, h, Color.web("#cc000033"));
    scene.setUserData("cut icosahedronS2 scene");

    System.out.println(wrapper + ": traversable :" + pane.isFocusTraversable());
    System.out.println(pane + ": traversable :" + pane.isFocusTraversable());

    return scene;
  }

  //-------------------------------------------------------------------

  @Override
  public final void start (final Stage stage) {
    stage.setTitle("cut icosahedron (subdivided)");
    stage.sizeToScene();
    final var bounds = Util.chooseScreen().getVisualBounds();
    final double w = 0.75 * bounds.getWidth();
    final double h = 0.50 * w;
    stage.setX(0.5 * (bounds.getMinX() + bounds.getMaxX() - w));
    stage.setY(0.5 * (bounds.getMinY() + bounds.getMaxY() - h));
    stage.centerOnScreen();
    stage.setScene(scene);
    stage.show();
  }

  //-------------------------------------------------------------------
  // Construction
  //-------------------------------------------------------------------

  public IcosahedronS2 () {
    super();
    worldPane = makePane();
    final var bounds = Util.chooseScreen().getVisualBounds();
    final double w = 0.75 * bounds.getWidth();
    final double h = 0.5 * w;
    scene = makeScene(worldPane, w, h);
  }

  //-------------------------------------------------------------------
  // main
  //-------------------------------------------------------------------

  public final static void run (final String[] args) { launch(args); }

}
//---------------------------------------------------------------------
