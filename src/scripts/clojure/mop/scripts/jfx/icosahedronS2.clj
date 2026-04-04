(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.scripts.jfx.icosahedronS2
  {:doc     "Use JavaFX to display cut icosahedron and natural earth boundaries
  in planar lon,lat coordinates."
   :author  "palisades dot lakes at gmail dot com"
   :version "2026-04-04"}

  (:require
   [mop.cmplx.complex :as cmplx]
   [mop.geom.icosahedron :as icosahedron]
   [mop.geom.rn :as rn]
   [mop.geom.s2 :as s2]
   [mop.io.shapefile :as miosh])
  (:import
   [java.util List]
   [javafx.geometry Insets]
   [javafx.scene Group Scene]
   [javafx.scene.layout BorderPane]
   [javafx.scene.paint Color]
   [javafx.scene.shape Polygon StrokeType]
   [mop.java.cmplx TwoSimplex]
   [mop.java.geom.mesh TriangleMesh]
   [mop.java.jfx IcosahedronS2 JfxApplication WorldPane]
   [org.locationtech.jts.geom GeometryFactory]))
;;----------------------------------------------------------------
;; mvn -q install & cljfx src\scripts\clojure\mop\scripts\jfx\icosahedronS2.clj
;;----------------------------------------------------------------
(defn land [gfactory]
  (let [polygons (miosh/read-jts-geometries
                  "data/natural-earth/10m_physical/ne_10m_land.shp"
                  #_"data/natural-earth/50m_physical/ne_50m_land.shp"
                  #_"data/natural-earth/ne_110m_land.shp"
                  gfactory)
        fill (Color/web "#22990044")
        stroke (Color/web "#a6611aFF")
        group (miosh/jfx-node polygons fill stroke)]
    (.setId group "land")
    (.setFocusTraversable group false)
    group))
;;----------------------------------------------------------------
(defn icosahedron []
  (let [group (Group.)
        _ (.setId group "icosahedron")
        positiveStroke (Color/web "#2166ac", 0.5)
        ;; positiveFil (Color/web "#d1e5f0", 0.2)
        positiveFill (Color/web "#ffffff", 0.0)
        negativeFill (Color/web "#fddbc7", 0.5)
        negativeStroke (Color/web "#b2182b", 1)
        ^TriangleMesh mesh ((comp cmplx/midpoint-subdivide-4
                                  cmplx/midpoint-subdivide-4)
                            (icosahedron/u2-cut-icosahedron))
        faces (.faces (.cmplx mesh))
        _ (println "n faces: " (.size faces))
        embedding (.embedding mesh)
        triangles (mapv (fn [^TwoSimplex face]
                          (let [p0 (s2/to-ll (embedding (.z0 face)))
                                p1 (s2/to-ll (embedding (.z1 face)))
                                p2 (s2/to-ll (embedding (.z2 face)))
                                triangle (Polygon. (double-array
                                                    [(.getX p0) (.getY p0)
                                                     (.getX p1) (.getY p1)
                                                     (.getX p2) (.getY p2)]))
                                _ (.setId triangle (.toString face))
                                area (rn/signed-area p0 p1 p2)]
                            ;; strokeWidth 0.0 doesn't seem to work.
                            ;; may need to invert scaling transform to get more-or-less
                            ;; constant width on screen
                            ;; problem seems to be related to jfx forcing windows dpi scaling
                            ;; on its own coordinates.
                            (.setStrokeWidth triangle 1)
                            (.setStrokeType triangle StrokeType/CENTERED)
                            (if (<= 0.0 area)
                              (do
                                (.setFill triangle positiveFill)
                                (.setStroke triangle positiveStroke))
                              ;;else
                              (do
                                (.setFill triangle negativeFill)
                                (.setStroke triangle negativeStroke)))
                            triangle))
                        faces)]
    (.addAll (.getChildren group) ^List triangles)
    ;; parent handles events
    (.setFocusTraversable group false)
    group))

(defn make-world []
  (let [world (Group. [(icosahedron) (land (GeometryFactory.))])]
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
    (.setUserData scene "cut icosahedronS2 scene")
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

#_(IcosahedronS2/launch IcosahedronS2 (make-array String 0))