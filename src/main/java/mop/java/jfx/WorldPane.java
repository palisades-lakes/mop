package mop.java.jfx;

import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.*;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;

//---------------------------------------------------------------------

/**  Pane containing geographic layers, with standardized event handling.
 * <p>
 *
 * @author palisades dot lakes at gmail dot com
 * @version 2026-04-08
 */

@SuppressWarnings({ "unchecked", "unused" })
public final class WorldPane extends Pane {

  //-------------------------------------------------------------------
  // class slots and methods
  //-------------------------------------------------------------------

  // TODO: set this per layer or even per descendant node

  private static final double pixelStrokeWidth = 1;

  private static final void setWorldStrokeWidth (final Node node,
                                                 final double w) {
    switch (node) {
      case final Shape shape -> shape.setStrokeWidth(w);
      case final Parent parent ->
        parent.getChildrenUnmodifiable().forEach(
          (child) -> setWorldStrokeWidth(child, w));
      default -> {/* do nothing */} } }

  //-------------------------------------------------------------------
  // instance slots
  //-------------------------------------------------------------------
  // TODO: get rid of persistent mutable state?

  private double xDragOrigin = Double.NaN;
  private double yDragOrigin = Double.NaN;

  private final void origin (final MouseEvent e) {
    xDragOrigin = e.getSceneX();
    yDragOrigin = e.getSceneY(); }

  private final void drag (final MouseEvent e) {
    final double x = e.getSceneX();
    final double y = e.getSceneY();
    assert (!(Double.isNaN(xDragOrigin) || Double.isNaN(yDragOrigin)));
    setTranslateX(x - xDragOrigin + getTranslateX());
    setTranslateY(y - yDragOrigin + getTranslateY());
    xDragOrigin = x;
    yDragOrigin = y; }

  //-------------------------------------------------------------------
  // instance methods
  //-------------------------------------------------------------------
  // TODO: this is approximate, because changing the stroke width
  // changes the bounds used to compute it.

  private final void scaleStrokeWidth (final double pixelWidth) {
    final ObservableList<Node> children =
      this.getChildrenUnmodifiable();
    // TODO: handle multiple children
    assert 1 == children.size();
    final Parent root = (Parent) children.getFirst();
    final Bounds rootBounds = root.getBoundsInLocal();
    final double rw = rootBounds.getWidth();
    final double rh = rootBounds.getHeight();
    final Bounds parentBounds = this.getLayoutBounds();
    final double sw = parentBounds.getWidth();
    final double sh = parentBounds.getHeight();
    if ((0.0 < rw) && (0.0 < rh) && (0.0 < sw) && (0.0 < sh)) {
      final double s = Math.min(sw / rw, sh / rh);
      // NOTE: changing strokeWidth changes local/layout bounds
      setWorldStrokeWidth(root, pixelWidth / (s*getScaleX())); } }

  public final void rescale () {
    // NOTE: changing strokeWidth changes local/layout bounds
    scaleStrokeWidth(pixelStrokeWidth);
    final ObservableList<Node> children =
      this.getChildrenUnmodifiable();
    // TODO: handle multiple children
    assert 1 == children.size();
    final Parent child = (Parent) children.getFirst();
    final Bounds childBounds = child.getBoundsInLocal();
    final double cw = childBounds.getWidth();
    final double ch = childBounds.getHeight();
    final Bounds parentBounds = this.getLayoutBounds();
    final double sw = parentBounds.getWidth();
    final double sh = parentBounds.getHeight();
    if ((0.0 < cw) && (0.0 < ch) && (0.0 < sw) && (0.0 < sh)) {
      final Transform preTranslate =
        new Translate(-childBounds.getMinX(),
                      -childBounds.getMaxY());
      final double s = Math.min(sw / cw, sh / ch);
      final Transform scale = new Scale(s, -s);
      child.getTransforms().setAll(scale, preTranslate); } }

  //-------------------------------------------------------------------
  // hidden constructor
  //-------------------------------------------------------------------

  private WorldPane (final Group layers) {
    super(layers);
    this.setBackground(Background.fill(Color.web("#cccccc11")));
    this.setId(layers.getId() + " pane");
    this.setFocusTraversable(true);

    this.setOnKeyPressed((final KeyEvent e) -> {
      e.consume();
      if (KeyCode.R == e.getCode()) {
        this.setTranslateX(0.0);
        this.setTranslateY(0.0);
        this.setScaleX(1.0);
        this.setScaleY(1.0); } } );

    // TODO: zoom holding mouse point fixed
    this.setOnScroll((final ScrollEvent e) -> {
      e.consume();
      if (!e.isDirect()) { // ignore touch events?
        final double relativeZoom = (e.getDeltaY() < 0) ? 0.9 : 1.1;
        final double s = relativeZoom * getScaleX();
        setScaleX(s);
        setScaleY(s);
        scaleStrokeWidth(pixelStrokeWidth);} } );

    this.setOnMousePressed((final MouseEvent e) -> {
      e.consume();
      // ignore touch events?
      if (!e.isSynthesized()) { origin(e); }});

    this.setOnMouseDragged((final MouseEvent e) -> {
      e.consume();
      // ignore touch events?
      if (!e.isSynthesized()) { drag(e); }});

    final ChangeListener changeListener = (obs, oldVal, newVal) -> {
      if (!oldVal.equals(newVal)) { this.rescale(); } };
    this.layoutBoundsProperty().addListener(changeListener);
  }

  public static final WorldPane make (final Group layers) {
    return new WorldPane(layers); }

//---------------------------------------------------------------------
}
//---------------------------------------------------------------------
