package mop.java.jfx;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import javafx.collections.ObservableList;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import mop.java.geom.mesh.TriangleMesh;
import org.apache.commons.geometry.euclidean.twod.Vector2D;
import org.locationtech.jts.geom.GeometryCollection;

/**
 * Common JFX functions.
 * <p>
 *
 * @author palisades dot lakes at gmail dot com
 * @version 2026-03-23
 */

@SuppressWarnings("unused")
public final class JfxUtil {

  //-------------------------------------------------------------------
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

  public static final TriangleMesh subdivide4 (
    final TriangleMesh mesh) {
    return (TriangleMesh) subdivide4Var.invoke(mesh);
  }

  public static final TriangleMesh u2CutIcosahedron () {
    return (TriangleMesh) icosahedronU2Cut.invoke();
  }

  public static final Vector2D toLonLat (final Object p) {
    return (Vector2D) toLonLatVar.invoke(p);
  }

  public static final double signedArea (final Vector2D p0,
                                         final Vector2D p1,
                                         final Vector2D p2) {
    return (double) signedAreaVar.invoke(p0, p1, p2);
  }

  public static final Group jfxNode (final Object geometry,
                                     final Color fill,
                                     final Color stroke) {
    return (Group) jfxNodeVar.invoke(geometry, fill, stroke);
  }

  public static final GeometryCollection readJTSGeometries (
    final String path) {
    return (GeometryCollection) readJTSGeometriesVar.invoke(path);
  }

  public static final void printBounds (final Node node) {
    System.out.println("\n" + node.getClass().getSimpleName()
                         + ": " + node.getId());
    System.out.println("local:\n" + node.getBoundsInLocal());
    System.out.println("parent:\n" + node.getBoundsInParent());
    System.out.println("layout:\n" + node.getLayoutBounds());
    // only go one level down from root
    if ((null == node.getParent()) && (node instanceof Parent)) {
      final Parent parent = (Parent) node;
      parent.getChildrenUnmodifiable()
            .forEach(JfxUtil::printBounds);
    }
  }

  public static final void printBounds (final Scene scene) {
    System.out.println("\n" + scene.getClass().getSimpleName()
                         + ": " + scene.getUserData());
    System.out.println(scene.getX() + " " + scene.getY() + " "
                         + scene.getWidth() + " " + scene.getHeight());
    final Node root = scene.getRoot();
    if (null != root) { printBounds(root); }
  }

  public static final void printBounds (final Stage window) {
    System.out.println("\n" + window.getClass().getSimpleName() +
                         ": " + window.getTitle());
    System.out.println(window.getX() + " " + window.getY() + " "
                         + window.getWidth() + " " + window.getHeight());
    final Scene scene = window.getScene();
    if (null != scene) { printBounds(scene); }
  }

  public static double areaInch2 (final Screen screen) {
    final double ipd = 1.0 / screen.getDpi();
    final Rectangle2D bounds = screen.getVisualBounds();
    // TODO: scale op for rectangle and other geometry objects
    // TODO: partial ordering by width and height
    return ipd * bounds.getWidth() * ipd * bounds.getHeight();
  }

  public static final Screen chooseScreen () {
    final ObservableList<Screen> screens = Screen.getScreens();
    Screen largest = Screen.getPrimary();
    double maxArea = areaInch2(largest);
    for (final Screen screen : screens) {
      final double area = areaInch2(screen);
      if (area > maxArea) {
        maxArea = area;
        largest = screen;
      }
    }
    return largest;
  }

//-------------------------------------------------------------------
// disable construction
//-------------------------------------------------------------------

  private JfxUtil () {
    throw new UnsupportedOperationException(
      "Can't instantiate " + getClass());
  }

//---------------------------------------------------------------------
}


