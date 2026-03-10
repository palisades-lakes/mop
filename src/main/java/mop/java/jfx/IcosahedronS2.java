package mop.java.jfx;

// jfx mop.java.jfx.IcosahedronS2.java

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Polygon;
import mop.java.cmplx.TwoSimplex;
import mop.java.geom.mesh.TriangleMesh;
import org.apache.commons.geometry.spherical.twod.Point2S;

@SuppressWarnings("unchecked")
public final class IcosahedronS2 extends Application {

  private static final TriangleMesh s2Icosahedron () {
    final IFn require = Clojure.var("clojure.core", "require");
    System.out.println(require);
    require.invoke(Clojure.read("mop.geom.icosahedron"));
    final IFn icosahedronS2 =
      Clojure.var("mop.geom.icosahedron", "s2-icosahedron");
    System.out.println(icosahedronS2);

    return (TriangleMesh) icosahedronS2.invoke();
  }

  @Override
  public final void start (final javafx.stage.Stage stage) {

    final int w = 640;
    final int h = 640;
    final javafx.scene.Group group = new javafx.scene.Group();
    final javafx.scene.paint.Color fill =
      javafx.scene.paint.Color.web("DARKKHAKI", 0.5);
    final javafx.scene.paint.Color stroke =
      javafx.scene.paint.Color.web("BROWN");
    final TriangleMesh mesh = s2Icosahedron();
    System.out.println(mesh);
    final java.util.List<TwoSimplex> faces = mesh.cmplx().faces();
    final IFn embedding = mesh.embedding();
    for (final TwoSimplex face : faces) {
      final Point2S p0 = (Point2S) embedding.invoke(face.z0());
      final Point2S p1 = (Point2S) embedding.invoke(face.z1());
      final Point2S p2 = (Point2S) embedding.invoke(face.z2());
      System.out.println(p0);
      System.out.println(p1);
      System.out.println(p2);
      final Polygon triangle =
        new Polygon(Math.toDegrees(p0.getAzimuth()),
                    Math.toDegrees(p0.getPolar()),
                    Math.toDegrees(p1.getAzimuth()),
                    Math.toDegrees(p1.getPolar()),
                    Math.toDegrees(p2.getAzimuth()),
                    Math.toDegrees(p2.getPolar()));
      triangle.setFill(fill);
      triangle.setStroke(stroke);
      group.getChildren().add(triangle);
    }
    final StackPane stackPane = new StackPane(group);
    final Scene scene = new Scene(stackPane, w, h);
    stage.setScene(scene);
    stage.show();
  }

  @SuppressWarnings("unused")
  public final static void main (final String[] args) { launch(); }
}