package mop.java.jfx;

// mvn -nsu clean install
// mvn -q -o -nsu -DskipTests install & jfx mop.java.jfx.Main

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;


//---------------------------------------------------------------------

/**
 * Experiment with icosahedron and map display via jfx.
 * <p>
 *
 * @author palisades dot lakes at gmail dot com
 * @version 2026-03-31
 */

public final class IcosahedronS2 extends Application {

    private final Scene scene;

  //-------------------------------------------------------------------

  private static Scene makeScene (final double w,
                                  final double h) {
    final Pane pane = WorldPane.make();
    BorderPane.setMargin(pane, new Insets(32));
    final BorderPane wrapper = new BorderPane(pane);
    final Scene scene = new Scene(wrapper, w, h);
    scene.setUserData("cut icosahedronS2 scene");

    // TODO: doesn't capture touch scroll events!
    // disable touch events
    // https://stackoverflow.com/questions/32124473/how-do-i-disable-touch-events-in-javafx
//    scene.setOnTouchMoved(e -> e.consume());
//    scene.setOnTouchPressed(e -> e.consume());
//    scene.setOnTouchReleased(e -> e.consume());
//    scene.setOnTouchStationary(e -> e.consume());
//    scene.addEventFilter(TouchEvent.ANY, (final TouchEvent e) -> {
//      //System.out.println(e.getEventType());
//      e.consume(); } );

    return scene;
  }

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
    stage.setScene(scene);
    stage.show();
  }

  //-------------------------------------------------------------------
  // Construction
  //-------------------------------------------------------------------

  public IcosahedronS2 () {
    super();
    final var bounds = Util.chooseScreen().getVisualBounds();
    final double w = 0.75 * bounds.getWidth();
    final double h = 0.5 * w;
    scene = makeScene(w, h);
  }

  //-------------------------------------------------------------------
  // main
  //-------------------------------------------------------------------

  public final static void run (final String[] args) { launch(args); }

}
//---------------------------------------------------------------------
