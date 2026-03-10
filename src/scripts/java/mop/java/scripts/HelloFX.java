package mop.java.scripts;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.stage.Stage;

// jfx src/scripts/java/mop/java/scripts/HelloFX.java

public final class HelloFX extends Application {

  @Override
  public final void start (final Stage stage) {
    final String javaVersion = System.getProperty("java.version");
    final String javafxVersion = System.getProperty("javafx.version");
    final int w = 640;
    final int h = 640;
    final Label label = new Label(
      "Hello, JavaFX " + javafxVersion + ", running on Java " + javaVersion + ".");
    final Polygon triangle = new Polygon(0.0,0.0, 0.0,512.0, 512.0,512.0 );
    final Color fill = Color.web("DARKKHAKI", 0.5);
    triangle.setFill(fill);
    final Color stroke = Color.web("BROWN");
    triangle.setStroke(stroke);
    final StackPane stackPane = new StackPane(label,triangle);
    final Scene scene = new Scene(stackPane, w,h);
    stage.setScene(scene);
    stage.show();
  }

  @SuppressWarnings("unused")
  public final static void main (final String[] args) { launch(); } }