package mop.java.jfx;

// mvn clean install
// mvn -q -o -DskipTests -Dclojure-maven-plugin.clojure.test.skip=true -Dmaven.test.skip=true install & jfx mop.java.jfx.Main

import clojure.lang.IFn;
import javafx.application.Application;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.StrokeType;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
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
 * @version 2026-03-23
 */

@SuppressWarnings({ "unchecked", "unused" })
public final class IcosahedronS2 extends Application {

  //-------------------------------------------------------------------

  private static final Group land () {
    final GeometryCollection polygons =
      JfxUtil.readJTSGeometries("data/natural-earth/ne_110m_land.shp");
    final Color fill = Color.web("#22990044");
    final Color stroke = Color.web("#a6611aFF");
    final Group group = JfxUtil.jfxNode(polygons, fill, stroke);
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
      // subdivide4(
//        subdivide4(
//          subdivide4(
            JfxUtil.subdivide4(
      JfxUtil.u2CutIcosahedron()
//                      )
//                    )
      //)
    )
      ;
    final List<TwoSimplex> faces = mesh.cmplx().faces();
    final IFn embedding = mesh.embedding();
    for (final TwoSimplex face : faces) {
      final Point2U u0 = (Point2U) embedding.invoke(face.z0());
      final Point2U u1 = (Point2U) embedding.invoke(face.z1());
      final Point2U u2 = (Point2U) embedding.invoke(face.z2());
      final Vector2D p0 = JfxUtil.toLonLat(u0);
      final Vector2D p1 = JfxUtil.toLonLat(u1);
      final Vector2D p2 = JfxUtil.toLonLat(u2);
      final Polygon triangle =
        new Polygon(p0.getX(), p0.getY(),
                    p1.getX(), p1.getY(),
                    p2.getX(), p2.getY());
      triangle.setId(face.toString());
      final double area = JfxUtil.signedArea(p0, p1, p2);
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

  //-------------------------------------------------------------------

  private static final Group makeWorld () {
    final Group land = land();
    final Group icosahedron = icosahedron();
    final Group world = new Group(icosahedron, land);
    world.setId("world");
    return world;
  }

//-------------------------------------------------------------------

  private static final void rescale (final Scene scene) {
    final Parent root = scene.getRoot();
    final Bounds rootBounds = root.getBoundsInLocal();
    final double rw = rootBounds.getWidth();
    final double rh = rootBounds.getHeight();
    final double sw = scene.getWidth();
    final double sh = scene.getHeight();
    final double s = Math.min(sw / rw, sh / rh);
    final Transform preTranslate =
      new Translate(-rootBounds.getMinX(), -rootBounds.getMaxY());
    final Transform scale = new Scale(s, -s);
    root.getTransforms().setAll(scale, preTranslate);
  }

//-------------------------------------------------------------------

  private static final Scene makeScene (final double w,
                                        final double h) {
    final Parent root = makeWorld();
    final Scene scene = new Scene(root, w, h);
    scene.setUserData("cut icosahedronS2");
    rescale(scene);
    return scene;
  }

  //-------------------------------------------------------------------

  @Override
  public final void start (final Stage stage) {
    stage.setTitle("cut icosahedron (subdivided)");
    final var bounds = JfxUtil.chooseScreen().getVisualBounds();
    final double w = 0.75 * bounds.getWidth();
    final double h = 0.5 * w;
    stage.setX(
      (0.5 * (bounds.getMinX() + bounds.getMaxX())) - (0.5 * w));
    stage.setY(
      (0.5 * (bounds.getMinY() + bounds.getMaxY())) - (0.5 * h));
    stage.setMinWidth(Math.min(w, 360));
    stage.setMinHeight(Math.min(h, 180));
    stage.setMaxWidth(bounds.getWidth());
    stage.setMaxHeight(bounds.getHeight());

    final Scene scene = makeScene(w, h);
    stage.setScene(scene);
    stage.sizeToScene();
    stage.show();
  }

//-------------------------------------------------------------------
// main
//-------------------------------------------------------------------

  public final static void run (final String[] args) { launch(args); }

}
//---------------------------------------------------------------------
