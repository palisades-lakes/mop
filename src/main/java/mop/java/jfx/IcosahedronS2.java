package mop.java.jfx;

// jfx mop.java.jfx.IcosahedronS2

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.stage.Stage;
import mop.java.cmplx.TwoSimplex;
import mop.java.geom.mesh.TriangleMesh;
import org.apache.commons.geometry.euclidean.twod.Vector2D;
import org.apache.commons.geometry.spherical.twod.Point2S;
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
  private static final IFn toLonLatVar =
    Clojure.var("mop.geom.s2", "s2-to-ll");
  private static final IFn signedAreaVar =
    Clojure.var("mop.geom.rn", "signed-area");
  private static final IFn jfxNodeVar =
    Clojure.var("mop.io.shapefile", "jfx-node");
  private static final IFn readJTSGeometriesVar =
    Clojure.var("mop.io.shapefile", "read-jts-geometries");

  private static final TriangleMesh s2Icosahedron () {
    return (TriangleMesh) icosahedronS2.invoke(); }

  private static final Vector2D toLonLat (final Point2S p) {
    return (Vector2D) toLonLatVar.invoke(p); }

  private static final double signedArea (final Vector2D p0,
                                          final Vector2D p1,
                                          final Vector2D p2) {
    return (double) signedAreaVar.invoke(p0, p1, p2); }

  private static final Group jfxNode (final Object geometry) {
    return (Group) jfxNodeVar.invoke(geometry); }

  private static final GeometryCollection readJTSGeometries (
    final String path) {
    return (GeometryCollection) readJTSGeometriesVar.invoke(path); }

  @Override
  public final void start (final Stage stage) {

    final int w = (360 * 4) / 3;
    final int h = (180 * 4) / 3;
    //stage.setMinWidth(w);
    //stage.setMinHeight(h);

    System.out.println(readJTSGeometriesVar);
    final GeometryCollection polygons =
      readJTSGeometries("data/natural-earth/ne_110m_land.shp");
    final Group land = jfxNode(polygons);

    final Group group = new Group();
    group.getChildren().add(land);

    final Color positiveStroke = Color.web("#2166ac", 0.5);
    //final Color positiveFill = Color.web("#d1e5f0", 0.2);
    final Color positiveFill = Color.web("#ffffff", 0.0);
    final Color negativeFill = Color.web("#fddbc7", 0.5);
    final Color negativeStroke = Color.web("#b2182b", 1);
    final TriangleMesh mesh = s2Icosahedron();
    final List<TwoSimplex> faces = mesh.cmplx().faces();
    final IFn embedding = mesh.embedding();
    for (final TwoSimplex face : faces) {
      System.out.println();
      System.out.println(face);
      final Vector2D p0 =
        toLonLat((Point2S) embedding.invoke(face.z0()));
      final Vector2D p1 =
        toLonLat((Point2S) embedding.invoke(face.z1()));
      final Vector2D p2 =
        toLonLat((Point2S) embedding.invoke(face.z2()));
      final Polygon triangle =
        new Polygon(p0.getX(), p0.getY(),
                    p1.getX(), p1.getY(),
                    p2.getX(), p2.getY());
      final double area = signedArea(p0, p1, p2);
      System.out.println(area);
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
    group.setAutoSizeChildren(true);
    final StackPane stackPane = new StackPane(group);
    final ScrollPane scrollPane = new ScrollPane(stackPane);
    scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
    scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
    scrollPane.setFitToWidth(true);
    scrollPane.setFitToHeight(true);
    final Scene scene = new Scene(scrollPane, w, h);
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
