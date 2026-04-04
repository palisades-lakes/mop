package mop.java.jfx;

import clojure.lang.IFn;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

//---------------------------------------------------------------------
/**
 * Experiment with map display via jfx.
 * <p>
 *
 * @author palisades dot lakes at gmail dot com
 * @version 2026-04-04
 */

public final class JfxApplication extends Application {

  // TODO: very bad! public mutable class slot!
  // Is there some point when I can put the scene into an instance slot?
  private static IFn _sceneBuilder;
  public static final IFn getSceneBuilder () {
    return _sceneBuilder; }
  public static final void setSceneBuilder (final IFn sceneBuilder) {
    _sceneBuilder = sceneBuilder; }
  public static final Scene makeScene (final double w, final double h) {
    return (Scene) _sceneBuilder.invoke(w,h); }

  //-------------------------------------------------------------------

  @Override
  public final void start (final Stage stage) {
    stage.setTitle("cut icosahedron (subdivided)");
    stage.sizeToScene();
    final var bounds = Util.chooseScreen().getVisualBounds();
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
