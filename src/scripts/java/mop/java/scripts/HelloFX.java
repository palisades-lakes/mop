package mop.java.scripts;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

// jfx src/scripts/java/mop/java/scripts/HelloFX.java

public final class HelloFX extends Application {

  @Override
  public final void start (final Stage stage) {
    final String javaVersion = System.getProperty("java.version");
    final String javafxVersion = System.getProperty("javafx.version");
    final Label l = new Label(
      "Hello, JavaFX " + javafxVersion + ", running on Java " + javaVersion + ".");
    final Scene scene = new Scene(new StackPane(l), 640, 480);
    stage.setScene(scene);
    stage.show();
  }

  @SuppressWarnings("unused")
  public final static void main (final String[] args) {
    launch();
  }

}