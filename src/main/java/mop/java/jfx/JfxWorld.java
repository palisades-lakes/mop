package mop.java.jfx;

import clojure.lang.IFn;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Screen;
import javafx.stage.Stage;

//---------------------------------------------------------------------
/**  Experiment with map display via jfx.
 * <p>
 *
 * @author palisades dot lakes at gmail dot com
 * @version 2026-04-11
 */

public final class JfxWorld extends Application {

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
    double maxArea = Double.NEGATIVE_INFINITY;
    for (final Screen screen : screens) {
      final double area = areaInch2(screen);
      if (area > maxArea) { maxArea = area; largest = screen; } }
    return largest; }

  //-------------------------------------------------------------------

  // TODO: very bad! public mutable class slot!
  // Is there some point at which I can put a preconstructed world
  // into an instance slot?
  private static IFn _worldBuilder;
  public static final IFn getWorldBuilder () {
    return _worldBuilder; }
  public static final void setWorldBuilder (final IFn worldBuilder) {
    _worldBuilder = worldBuilder; }
  public static final Group makeWorld () {
    return (Group) getWorldBuilder().invoke(); }

  //----------------------------------------------------------------

  private static final Scene makeScene (final double w,
                                        final double h) {
    final Group world = makeWorld();
    // parent Pane handles events
      // TODO: is this necessary or useful?
    world.setFocusTraversable(false);
    world.setMouseTransparent(true);
    final Pane pane = WorldPane.make(world);
    BorderPane.setMargin(pane, new Insets(16));
    final Pane wrapper = new BorderPane(pane);
    final Scene scene = new Scene(wrapper, w, h);
    scene.setUserData(world.getId());
    return scene;
  }
//-------------------------------------------------------------------

  @Override
  public final void start (final Stage stage) {
    stage.sizeToScene();
    final var bounds = chooseScreen().getVisualBounds();
    final double w = 0.75 * bounds.getWidth();
    final double h = 0.50 * w;
    stage.setX(0.5 * (bounds.getMinX() + bounds.getMaxX() - w));
    stage.setY(0.5 * (bounds.getMinY() + bounds.getMaxY() - h));
    stage.centerOnScreen();
    final Scene scene = makeScene(w, h);
    stage.setScene(scene);
    stage.setTitle(scene.getUserData().toString());
    stage.show();
  }

  //-------------------------------------------------------------------
  // Construction
  //-------------------------------------------------------------------
  public JfxWorld () { super(); }
//---------------------------------------------------------------------
}
//---------------------------------------------------------------------
