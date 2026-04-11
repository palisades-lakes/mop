;; mvn -q install & cljfx src\scripts\clojure\mop\scripts\jts\land.clj
;;----------------------------------------------------------------
(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.scripts.jts.land
  {:doc     "Use JavaFX to display a conformal delaunay triangles of
  natural earth boundaries."
   :author  "palisades dot lakes at gmail dot com"
   :version "2026-04-10"}

  (:require
   [mop.gt.gt :as gt]
   [mop.io.shapefile :as miosh]
   [mop.jts.jts :as jts])
  (:import
   [java.util Collection]
   [javafx.geometry Insets]
   [javafx.scene Group Scene]
   [javafx.scene.layout BorderPane]
   [mop.java.jfx JfxApplication WorldPane]
   [org.locationtech.jts.geom
    GeometryCollection GeometryFactory]))
;;----------------------------------------------------------------
(defn ^GeometryCollection land-polygons [^GeometryFactory factory]
  (let [geometries (miosh/read-jts-geometries
                    #_"data/natural-earth/10m_physical/ne_10m_land.shp"
                    #_"data/natural-earth/50m_physical/ne_50m_land.shp"
                    "data/natural-earth/110m_physical/ne_110m_land.shp"
                    factory)
        ;is-valid (IsValidOp. geometries)
        ;geometries (if (.isValid is-valid)
        ;               geometries
        ;               (GeometryFixer/fix geometries))
        ;is-valid (IsValidOp. geometries)
        ]
    ;(assert (.isSimple geometries))
    ;(assert (.isValid is-valid) (str (.getValidationError is-valid)))
    geometries))
;;----------------------------------------------------------------
(defn make-world []
  (let [factory (GeometryFactory.)
        land (land-polygons factory)
        land (gt/wgs84-to-stereographic land)
        land-group (jts/jfx land "#22990044" "#000088FF")
        ;triangles (jts/cdt land land 1.0)
        ;triangles-group (jts/jfx triangles "#FFFFFF00" "#000088FF")
        ;; 'children' binding with type hint seems necessary to avoid
        ;; reflection warnings; inline type hint gives warning?
        ^Collection children [land-group #_triangles-group]
        world (Group. children)]
    (.setId world "world")
    ;; parent Pane handles events
    ;; TODO: is this necessary or useful?
    (.setFocusTraversable world false)
    (.setMouseTransparent world true)
    world))
;;----------------------------------------------------------------
(defn make-scene ^Scene [^double w ^double h]
  (let [pane (WorldPane/make (make-world))
        _ (BorderPane/setMargin pane (Insets. 32))
        wrapper (BorderPane. pane)
        scene (Scene. wrapper w h)]
    (.setUserData scene "natural earth cdt scene")
    scene))
;;----------------------------------------------------------------
;;(println (System/getProperty "glass.win.uiScale"))
(System/setProperty "glass.win.uiScale" "1")
;;(println (System/getProperty "glass.win.uiScale"))
;;(System/setProperty "javafx.pulseLogger" "true")
;;(System/setProperty "prism.verbose" "true")
;;(System/setProperty "prism.order" "d3d")
(JfxApplication/setSceneBuilder make-scene)
(JfxApplication/launch JfxApplication (make-array String 0))
