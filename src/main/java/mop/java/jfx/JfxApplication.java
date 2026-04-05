package mop.java.jfx;

import clojure.lang.IFn;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

//---------------------------------------------------------------------
/**  Experiment with map display via jfx.
 * <p>
 *
 * @author palisades dot lakes at gmail dot com
 * @version 2026-04-05
 */

public final class JfxApplication extends Application {

  //-------------------------------------------------------------------

  private static double areaInch2 (final Screen screen) {
    final double ipd = 1.0 / screen.getDpi();
    final Rectangle2D bounds = screen.getVisualBounds();
    // TODO: scale op for rectangle and other geometry objects
    // TODO: partial ordering by width and height
    return ipd * bounds.getWidth() * ipd * bounds.getHeight(); }

  public static final Screen chooseScreen () {
    final ObservableList<Screen> screens = Screen.getScreens();
    Screen largest = Screen.getPrimary();
    double maxArea = areaInch2(largest);
    for (final Screen screen : screens) {
      final double area = areaInch2(screen);
      if (area > maxArea) { maxArea = area; largest = screen; } }
    return largest; }

  //-------------------------------------------------------------------

  // TODO: very bad! public mutable class slot!
  // Is there some point at which I can put a preconstructed scene
  // into an instance slot?
  private static IFn _sceneBuilder;
  public static final IFn getSceneBuilder () {
    return _sceneBuilder; }
  public static final void setSceneBuilder (final IFn sceneBuilder) {
    _sceneBuilder = sceneBuilder; }
  public static final Scene makeScene (final double w, final double h) {
    return (Scene) getSceneBuilder().invoke(w,h); }

  //-------------------------------------------------------------------

  @Override
  public final void start (final Stage stage) {
    stage.setTitle("cut icosahedron (subdivided)");
    stage.sizeToScene();
    final var bounds = chooseScreen().getVisualBounds();
    final double w = 0.75 * bounds.getWidth();
    final double h = 0.50 * w;
    stage.setX(0.5 * (bounds.getMinX() + bounds.getMaxX() - w));
    stage.setY(0.5 * (bounds.getMinY() + bounds.getMaxY() - h));
    stage.centerOnScreen();
    stage.setScene(makeScene(w, h));
    stage.show();
  }

  //-------------------------------------------------------------------
  // Construction
  //-------------------------------------------------------------------
  public JfxApplication () { super(); }
//---------------------------------------------------------------------
}
//---------------------------------------------------------------------
