package mop.java.jfx;

// mvn clean install
// mvn -q -DskipTests -Dclojure-maven-plugin.clojure.test.skip=true -Dmaven.test.skip=true install & jfx mop.java.jfx.IcosahedronS2

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import javafx.application.Application;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.StrokeType;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;
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
 * @version 2026-03-22
 */

@SuppressWarnings({"unchecked","unused"})
public final class IcosahedronS2 extends Application {

  // TODO: not sure how much of this is necessary
  // hard to debug if run as script because java source launcher
  // truncates stack traces.
  private static final IFn require =
    Clojure.var("clojure.core", "require");

  static {
    require.invoke(Clojure.read("mop.cmplx.complex"));
    require.invoke(Clojure.read("mop.geom.icosahedron"));
    require.invoke(Clojure.read("mop.geom.rn"));
    require.invoke(Clojure.read("mop.geom.s2"));
    require.invoke(Clojure.read("mop.io.shapefile"));
  }

  private static final IFn subdivide4Var =
    Clojure.var("mop.cmplx.complex", "midpoint-subdivide-4");
//  private static final IFn icosahedronS2 =
//    Clojure.var("mop.geom.icosahedron", "s2-icosahedron");
  private static final IFn icosahedronU2Cut =
    Clojure.var("mop.geom.icosahedron", "u2-cut-icosahedron");
  private static final IFn toLonLatVar =
    Clojure.var("mop.geom.s2", "to-ll");
  private static final IFn signedAreaVar =
    Clojure.var("mop.geom.rn", "signed-area");
  private static final IFn jfxNodeVar =
    Clojure.var("mop.io.shapefile", "jfx-node");
  private static final IFn readJTSGeometriesVar =
    Clojure.var("mop.io.shapefile", "read-jts-geometries");

//  private static final TriangleMesh s2Icosahedron () {
//    return (TriangleMesh) icosahedronS2.invoke();
//  }

  private static final TriangleMesh subdivide4 (final TriangleMesh mesh) {
    return (TriangleMesh) subdivide4Var.invoke(mesh);
  }

  private static final TriangleMesh u2CutIcosahedron () {
    return (TriangleMesh) icosahedronU2Cut.invoke();
  }

  private static final Vector2D toLonLat (final Object p) {
    return (Vector2D) toLonLatVar.invoke(p);
  }

  private static final double signedArea (final Vector2D p0,
                                          final Vector2D p1,
                                          final Vector2D p2) {
    return (double) signedAreaVar.invoke(p0, p1, p2);
  }

  private static final Group jfxNode (final Object geometry,
                                      final Color fill,
                                      final Color stroke) {
    return (Group) jfxNodeVar.invoke(geometry, fill, stroke);
  }

  private static final GeometryCollection readJTSGeometries (
    final String path) {
    return (GeometryCollection) readJTSGeometriesVar.invoke(path);
  }

  //-------------------------------------------------------------------

  private static final Group land () {
    final GeometryCollection polygons =
      readJTSGeometries("data/natural-earth/ne_110m_land.shp");
    final Color fill = Color.web("#22990044");
    final Color stroke = Color.web("#a6611aFF");
    return jfxNode(polygons, fill, stroke);
  }

  //-------------------------------------------------------------------

  private static final Group icosahedron () {
    final Group group = new Group();
    final Color positiveStroke = Color.web("#2166ac", 0.5);
    //final Color positiveFill = Color.web("#d1e5f0", 0.2);
    final Color positiveFill = Color.web("#ffffff", 0.0);
    final Color negativeFill = Color.web("#fddbc7", 0.5);
    final Color negativeStroke = Color.web("#b2182b", 1);
    final TriangleMesh mesh =
     // subdivide4(
        subdivide4(
          subdivide4(
            subdivide4(
              u2CutIcosahedron()
                      )
                    )
                  //)
    );
    final List<TwoSimplex> faces = mesh.cmplx().faces();
    final IFn embedding = mesh.embedding();
    for (final TwoSimplex face : faces) {
      final Point2U u0 = (Point2U) embedding.invoke(face.z0());
      final Point2U u1 = (Point2U) embedding.invoke(face.z1());
      final Point2U u2 = (Point2U) embedding.invoke(face.z2());
      final Vector2D p0 = toLonLat(u0);
      final Vector2D p1 = toLonLat(u1);
      final Vector2D p2 = toLonLat(u2);
      final Polygon triangle =
        new Polygon(p0.getX(), p0.getY(),
                    p1.getX(), p1.getY(),
                    p2.getX(), p2.getY());
      final double area = signedArea(p0, p1, p2);
//    // strokeWidth 0.0 doesn't seem to work.
      // may need to invert scaling transform to get more-or-less
      // constant width on screen
      // problem seems to be related to jfx forcing windows dpi scaling
      // on its own coordinates.
      triangle.setStrokeWidth(0.1);
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

  private static final Group makeGroup () {
    final Group land = land();
    final Group icosahedron = icosahedron();
    return new Group(icosahedron, land);
  }

  //-------------------------------------------------------------------

  private static final Pane makePane () {
    final Group group = makeGroup();
    //return new Pane(group);
    return new StackPane(group);
  }

  //-------------------------------------------------------------------

  private static final void rescale (final Scene scene) {
    final Parent parent = scene.getRoot();
    final Node child = parent.getChildrenUnmodifiable().getFirst();
    final Bounds childBounds = child.getLayoutBounds();
    final Bounds parentBounds = parent.getLayoutBounds();
    final double s = Math.min((parentBounds.getWidth())/childBounds.getWidth(),
                              (parentBounds.getHeight())/childBounds.getHeight());
    final Transform scale = new Scale(s, -s, childBounds.getCenterX(), childBounds.getCenterY());
    child.getTransforms().setAll(scale);//,preTranslate);
    parent.layout();
  }

  //-------------------------------------------------------------------

  private static final Scene makeScene (final double w,
                                        final double h) {
    final Pane parent = makePane();
    final Scene scene = new Scene(parent, w, h);
    rescale(scene);
    return scene;
  }

  //-------------------------------------------------------------------

  @Override
  public final void start (final Stage stage) {
    stage.setMinWidth(360);
    stage.setMinHeight(180);
    final Scene scene = makeScene(1280, 768);
    stage.setScene(scene);
//    stage.widthProperty().addListener(
//      (observable, oldValue, newValue)
//        -> rescale(scene));
//    stage.heightProperty().addListener(
//      (observable, oldValue, newValue)
//        -> rescale(scene));
    stage.sizeToScene();
    stage.setTitle("cut icosahedron (subdivided)");
    stage.show();
    System.out.println(
      "render: " + stage.getRenderScaleX() + " " + stage.getRenderScaleY());
    System.out.println(
      "Output: " + stage.getOutputScaleX() + " " + stage.getOutputScaleY());
//    stage.setRenderScaleX(1.0);
//    stage.setRenderScaleY(1.0);
//    System.out.println("render: " + stage.getRenderScaleX() + " " +
//    stage.getRenderScaleY());
//    System.out.println("Output: " + stage.getOutputScaleX() + " " +
//    stage.getOutputScaleY());
  }

  //-------------------------------------------------------------------
  // main
  //-------------------------------------------------------------------

  public final static void run (final String[] args) {
    System.out.println(System.getProperty("glass.win.uiScale"));
    System.setProperty("glass.win.uiScale", "1");
    System.out.println(System.getProperty("glass.win.uiScale"));
    launch(args); }

}
//---------------------------------------------------------------------
