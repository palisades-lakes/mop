package mop.java.scripts;

// jfx src/scripts/java/mop/java/scripts/IcosahedronU2.java

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.stage.Stage;
import mop.java.cmplx.TwoSimplex;
import mop.java.geom.Point2U;
import mop.java.geom.mesh.TriangleMesh;

import java.util.List;

@SuppressWarnings("unchecked")
public final class IcosahedronU2 extends Application {

  private static final TriangleMesh s2Icosahedron () {
    final IFn require = Clojure.var("clojure.core", "require");
    System.out.println(require);
    require.invoke(Clojure.read("mop.geom.icosahedron"));
    final IFn icosahedronS2 =
      Clojure.var("mop.geom.icosahedron", "u2-cut-icosahedron");
    System.out.println(icosahedronS2);

    return (TriangleMesh) icosahedronS2.invoke();
  }

  @Override
  public final void start (final Stage stage) {

    final int w = 640;
    final int h = 640;
    final javafx.scene.Group group = new javafx.scene.Group();
    final javafx.scene.paint.Color fill =
      javafx.scene.paint.Color.web("DARKKHAKI", 0.5);
    final javafx.scene.paint.Color stroke =
      javafx.scene.paint.Color.web("BROWN");
    final TriangleMesh mesh = s2Icosahedron();
    System.out.println(mesh);
    final List<TwoSimplex> faces = mesh.cmplx().faces();
    final IFn embedding = mesh.embedding();
    for (final TwoSimplex face : faces) {
      final Point2U p0 = (Point2U) embedding.invoke(face.z0());
      final Point2U p1 = (Point2U) embedding.invoke(face.z1());
      final Point2U p2 = (Point2U) embedding.invoke(face.z2());
      final Polygon triangle =
        new Polygon(Math.toDegrees(p0.getU()),
                    Math.toDegrees(p0.getV()),
                    Math.toDegrees(p1.getU()),
                    Math.toDegrees(p1.getV()),
                    Math.toDegrees(p2.getU()),
                    Math.toDegrees(p2.getV()));
      triangle.setFill(fill);
      triangle.setStroke(stroke);
      group.getChildren().add(triangle); }
    System.out.println(group.boundsInLocalProperty().getValue());
    System.out.println(group.boundsInParentProperty().getValue());
    System.out.println(group.getLayoutBounds());
    System.out.println(group.getLocalToParentTransform());
    System.out.println(group.getLocalToSceneTransform());
    final StackPane stackPane = new StackPane(group);
    stackPane.setPrefSize(w, h);
    stackPane.setStyle("-fx-background-color: lightgray;");
    System.out.println(stackPane.boundsInLocalProperty().getValue());
    System.out.println(stackPane.boundsInParentProperty().getValue());
    System.out.println(stackPane.getLayoutBounds());
    System.out.println(stackPane.getPadding());
    System.out.println(stackPane.getLocalToParentTransform());
    System.out.println(stackPane.getLocalToSceneTransform());
    final Scene scene = new Scene(stackPane, w, h, Color.web("aliceblue"));
    System.out.println(scene.getCamera());
    System.out.println(scene.getWidth());
    System.out.println(scene.getHeight());
    stage.setScene(scene);
    stage.show();
  }

  @SuppressWarnings("unused")
  public final static void main (final String[] args) { launch(); }
}
