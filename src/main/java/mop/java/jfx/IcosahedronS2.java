package mop.java.jfx;

// jfx mop.java.jfx.IcosahedronS2

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.stage.Stage;
import mop.java.cmplx.TwoSimplex;
import mop.java.geom.mesh.TriangleMesh;
import org.apache.commons.geometry.euclidean.twod.Vector2D;
import org.apache.commons.geometry.spherical.twod.Point2S;

import java.util.List;

@SuppressWarnings("unchecked")
public final class IcosahedronS2 extends Application {

  private static final Vector2D toLonLat (final Point2S p) {
    final double azimuth = Math.toDegrees(p.getAzimuth());
    final double lon = (180 < azimuth) ? azimuth - 360 : azimuth;
    final double lat = 90 - Math.toDegrees(p.getPolar());
    return Vector2D.of(lon, lat); }

  private static final IFn require =
    Clojure.var("clojure.core", "require");

  static {
    System.out.println("Static: " + require);
    require.invoke(Clojure.read("mop.geom.icosahedron"));
    require.invoke(Clojure.read("mop.geom.rn")); }

  private static final IFn icosahedronS2 =
    Clojure.var("mop.geom.icosahedron", "s2-icosahedron");
  static { System.out.println("static 2: " + icosahedronS2); }
  private static final TriangleMesh s2Icosahedron () {
    return (TriangleMesh) icosahedronS2.invoke(); }

  private static final IFn signedAreaVar =
    Clojure.var("mop.geom.rn", "signed-area");

  static { System.out.println("static 3: " + signedAreaVar); }

  private static final double signedArea (final Vector2D p0,
                                          final Vector2D p1,
                                          final Vector2D p2) {
    return (double) signedAreaVar.invoke(p0, p1, p2); }

  @Override
  public final void start (final Stage stage) {

    final int w = 640;
    final int h = 640;
    final Group group = new Group();
    final Color positiveStroke = Color.web("#2166ac", 1);
    final Color positiveFill = Color.web("#d1e5f0", 0.2);
    final Color negativeFill = Color.web("#fddbc7", 0.5);
    final Color negativeStroke = Color.web("#b2182b",1);
    final TriangleMesh mesh = s2Icosahedron();
    final List<TwoSimplex> faces = mesh.cmplx().faces();
    final IFn embedding = mesh.embedding();
    for (final TwoSimplex face : faces) {
      System.out.println();
      System.out.println(face);
      final Vector2D p0 = toLonLat((Point2S) embedding.invoke(face.z0()));
      final Vector2D p1 = toLonLat((Point2S) embedding.invoke(face.z1()));
      final Vector2D p2 = toLonLat((Point2S) embedding.invoke(face.z2()));
      System.out.println(p0);
      System.out.println(p1);
      System.out.println(p2);
      final Polygon triangle =
        new Polygon(p0.getX(),p0.getY(),
                    p1.getX(),p1.getY(),
                    p2.getX(),p2.getY());
      final double area = signedArea(p0, p1, p2);
      System.out.println(area);
      if (0.0 <= area) {
        triangle.setFill(positiveFill);
        triangle.setStroke(positiveStroke); }
      else {
        triangle.setFill(negativeFill);
        triangle.setStroke(negativeStroke); }
      group.getChildren().add(triangle); }
    group.setAutoSizeChildren(true);
    final StackPane stackPane = new StackPane(group);
    final Scene scene = new Scene(stackPane, w, h);
    stage.setScene(scene);
    stage.show(); }
  //-------------------------------------------------------------------
  // main
  //-------------------------------------------------------------------
  @SuppressWarnings("unused")
  public final static void main (final String[] args) { launch(); }
  //-------------------------------------------------------------------
}