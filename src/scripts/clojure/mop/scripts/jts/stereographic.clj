;; mvn -q install & cljfx src\scripts\clojure\mop\scripts\jts\cdt.clj
;;----------------------------------------------------------------
(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.scripts.jts.stereographic
  {:doc "Conforming delaunay triangulation.
  Natural earth boundaries and regular icosahedral triangulation
  as constraints, in stereographic projection."
   :author  "palisades dot lakes at gmail dot com"
   :version "2026-04-08"}
  (:require
   [mop.cmplx.complex :as cmplx]
   [mop.geom.icosahedron :as icosahedron]
   [mop.geom.mesh :as mesh]
   [mop.geom.s2 :as s2]
   [mop.jts.jts :as jts])
  (:import
   [java.util Collection]
   [javafx.geometry Insets]
   [javafx.scene Group Scene]
   [javafx.scene.layout BorderPane]
   [mop.java.geom.mesh TriangleMesh]
   [mop.java.jfx JfxApplication WorldPane]
   [org.locationtech.jts.geom GeometryFactory PrecisionModel]))
;;----------------------------------------------------------------
(defn make-world []
  (let [tolerance 1.0e-6
        precision (PrecisionModel. tolerance)
        factory (GeometryFactory. precision)
        ^TriangleMesh u2-mesh ((comp
                                #_cmplx/midpoint-subdivide-4
                                #_cmplx/midpoint-subdivide-4)
                               (icosahedron/u2-cut-icosahedron))
        embedding (into {} (map
                            (fn [[k v]] [k (jts/coordinate (s2/to-ll v))])
                            (.embedding u2-mesh)))
        mesh (mesh/triangle-mesh (.cmplx u2-mesh) embedding)
        polygons (jts/mesh-polygons mesh factory)
        polygons-group (jts/jfx polygons "#FFFFFF00" "#FF0000FF")
        edges (jts/mesh-linestrings mesh factory)
        _ (jts/write-wkt edges "edges.wkt")
        points (jts/mesh-points mesh factory)
        _ (jts/write-wkt points "points.wkt")
        ;;points (jts/centroids polygons)
        ;^Geometry triangles (jts/cdt points edges tolerance)
        ;_ (.setUserData triangles "triangulation")
        ;_ (jts/assert-valid triangles)
        triangulation-group (jts/jfx edges "#FFFFFF00" "#0000FF88")
        ;; 'children' binding with type hint seems necessary to avoid
        ;; reflection warnings; inline type hint gives warning?
        ^Collection children [#_polygons-group triangulation-group]
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
        _ (BorderPane/setMargin pane (Insets. 16))
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
