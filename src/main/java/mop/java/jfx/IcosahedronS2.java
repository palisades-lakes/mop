package mop.java.jfx;

// mvn install & jfx mop.java.jfx.IcosahedronS2

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.StrokeType;
import javafx.scene.transform.Transform;
import javafx.stage.Stage;
import mop.java.cmplx.TwoSimplex;
import mop.java.geom.Point2U;
import mop.java.geom.mesh.TriangleMesh;
import org.apache.commons.geometry.euclidean.twod.Vector2D;
import org.locationtech.jts.geom.GeometryCollection;

import java.util.List;

//---------------------------------------------------------------------
@SuppressWarnings("unchecked")
public final class IcosahedronS2 extends Application {

  // TODO: not sure how much of this is necessary
  // hard to debug if run as script because java source launcher
  // truncates stack traces.
  private static final IFn require =
    Clojure.var("clojure.core", "require");

  static {
    require.invoke(Clojure.read("mop.geom.icosahedron"));
    require.invoke(Clojure.read("mop.geom.rn"));
    require.invoke(Clojure.read("mop.geom.s2"));
    require.invoke(Clojure.read("mop.io.shapefile"));
  }

  private static final IFn icosahedronS2 =
    Clojure.var("mop.geom.icosahedron", "s2-icosahedron");
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

  private static final TriangleMesh u2CutIcosahedron () {
    return (TriangleMesh) icosahedronU2Cut.invoke();
  }

  private static final Vector2D toLonLat (final Object p) {
    return (Vector2D) toLonLatVar.invoke(p); }

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
    final Color fill = Color.web("#ffffff00");
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
    final TriangleMesh mesh = u2CutIcosahedron();
    final List<TwoSimplex> faces = mesh.cmplx().faces();
    final IFn embedding = mesh.embedding();
    for (final TwoSimplex face : faces) {
      System.out.println(face);
      final Point2U u0 = (Point2U) embedding.invoke(face.z0());
      final Point2U u1 = (Point2U) embedding.invoke(face.z1());
      final Point2U u2 = (Point2U) embedding.invoke(face.z2());
      System.out.println("u:" + u0 + ", " + u1 + ", " + u2);
      final Vector2D p0 = toLonLat(u0);
      final Vector2D p1 = toLonLat(u1);
      final Vector2D p2 = toLonLat(u2);
      System.out.println("p:" + p0 + ", " + p1 + ", " + p2);
      final Polygon triangle =
        new Polygon(p0.getX(), p0.getY(),
                    p1.getX(), p1.getY(),
                    p2.getX(), p2.getY());
      final double area = signedArea(p0, p1, p2);
//      System.out.println(area);
      // strokeWidth 0.0 doesn't seem to work.
      // may need to invert scaling transform to get more-or-less
      // constant width on screen
      triangle.setStrokeWidth(0.2);
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

  private static final Group makeGroup (final double w,
                                        final double h) {
    final Group land = land();
    final Group icosahedron = icosahedron();
    final Group group = new Group(land, icosahedron);

    group.setAutoSizeChildren(true);
    final ObservableList<Transform> transforms = group.getTransforms();
    final Bounds bounds = group.getBoundsInLocal();
    final double sx = w / bounds.getWidth();
    final double sy = h / bounds.getHeight();
    System.out.println(sx + ", " + sy);
    final Transform yFlip = Transform.scale(sx, -sy, 0, 0);
    System.out.println(yFlip);
    transforms.add(yFlip);
    return group;
  }
  //-------------------------------------------------------------------

  private static final Scene makeScene () {

    final double w = (2 * 360 * 4) / 3.0;
    final double h = (2 * 180 * 4) / 3.0;
    final Group group = makeGroup(w, h);
    final StackPane stackPane = new StackPane(group);
    final ScrollPane scrollPane = new ScrollPane(stackPane);
    scrollPane.setFitToWidth(true);
    scrollPane.setFitToHeight(true);
    return new Scene(scrollPane, w, h);
  }

  //-------------------------------------------------------------------

  @Override
  public final void start (final Stage stage) {
    final Scene scene = makeScene();
//    stage.setMinWidth(scene.getWidth());
//    stage.setMinHeight(scene.getHeight());
    stage.setScene(scene);
    stage.setResizable(true);
    stage.show();
  }

  //-------------------------------------------------------------------
  // main
  //-------------------------------------------------------------------
  @SuppressWarnings("unused")
  public final static void main (final String[] args) { launch(); }

}
//---------------------------------------------------------------------
