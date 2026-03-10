package mop.java.scripts;

// jfx src/scripts/java/mop/java/scripts/IcosahedronS2Cut.java

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import mop.java.cmplx.TwoSimplex;
import mop.java.geom.mesh.TriangleMesh;
import org.apache.commons.geometry.spherical.twod.Point2S;

import java.util.List;

@SuppressWarnings("unchecked")
public final class IcosahedronS2Cut extends Application {

  private static final TriangleMesh s2Icosahedron () {
    final IFn require = Clojure.var("clojure.core", "require");
    System.out.println(require);
    require.invoke(Clojure.read("mop.geom.icosahedron"));
    final IFn icosahedron =
      Clojure.var("mop.geom.icosahedron", "s2-cut-icosahedron");
    System.out.println(icosahedron);

    return (TriangleMesh) icosahedron.invoke();
  }

  @Override
  public final void start (final javafx.stage.Stage stage) {

    final int w = 640;
    final int h = 640;
    final Group group = new Group();
    final Color fill = Color.web("DARKKHAKI", 0.5);
    final Color stroke = Color.web("BROWN");
    final TriangleMesh mesh = s2Icosahedron();
    System.out.println(mesh);
    final List<TwoSimplex> faces = mesh.cmplx().faces();
    final IFn embedding = mesh.embedding();
    for (final TwoSimplex face : faces) {
      final Point2S p0 = (Point2S) embedding.invoke(face.z0());
      final Point2S p1 = (Point2S) embedding.invoke(face.z1());
      final Point2S p2 = (Point2S) embedding.invoke(face.z2());
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
    group.setAutoSizeChildren(true);
    final StackPane stackPane = new StackPane(group);
    final Scene scene = new Scene(stackPane, w, h);
    stage.setScene(scene);
    stage.show();
  }

  @SuppressWarnings("unused")
  public final static void main (final String[] args) { launch(); }
}