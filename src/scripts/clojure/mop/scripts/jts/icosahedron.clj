;; mvn -q -DskipTests -Dmaven.test.skip=true install & cljfx src\scripts\clojure\mop\scripts\jts\icosahedron.clj
;;----------------------------------------------------------------
(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.scripts.jts.icosahedron
  {:doc     "Conformal delaunay triangulation of a cut, projected icosahedron."
   :author  "palisades dot lakes at gmail dot com"
   :version "2026-04-09"}
  (:require
   [mop.cmplx.complex :as cmplx]
   [mop.geom.icosahedron :as icosahedron]
   [mop.geom.mesh :as mesh]
   [mop.geom.s2 :as s2]
   [mop.jts.jts :as jts])
  (:import
   [java.util Collection]
   [javafx.scene Group]
   [mop.java.geom.mesh TriangleMesh]
   [mop.java.jfx JfxWorld]
   [org.locationtech.jts.geom GeometryFactory]))
;;----------------------------------------------------------------
(defn make-world []
  (let [factory (GeometryFactory.)
        ^TriangleMesh u2-mesh ((comp
                                #_cmplx/midpoint-subdivide-4
                                cmplx/midpoint-subdivide-4)
                               (icosahedron/u2-cut-icosahedron))
        embedding (into {} (map
                            (fn [[k v]] [k (jts/coordinate (s2/to-ll v))])
                            (.embedding u2-mesh)))
        mesh (mesh/triangle-mesh (.cmplx u2-mesh) embedding)
        polygons (jts/mesh-polygons mesh factory)
        _ (jts/print-aspect-ratios polygons)
        ;polygons-group (jts/jfx polygons "#FFFFFF00" "#FF0000FF")
        edges (jts/mesh-linestrings mesh factory)
        edges-group (jts/jfx edges "#FFFFFF00" "#FF0000FF")
        ;;points (jts/mesh-points mesh factory)
        ;;points (jts/centroids polygons)
        ;; midpoint produces invalid geometry
        ;; triangles (jts/midpoint-cdt points edges 5.0e-1)
        ;^Geometry triangles (jts/cdt points edges 1.0)
        ;_ (.setUserData triangles "triangulation")
        ;_ (jts/assert-valid triangles)
        ;triangulation-group (jts/jfx triangles "#FFFFFF00" "#0000FFAA")
        ;; 'children' binding with type hint seems necessary to avoid
        ;; reflection warnings; inline type hint gives warning?
        ^Collection children [edges-group #_triangulation-group]
        world (Group. children)]
    (.setId world "world")
    ;; parent Pane handles events
    ;; TODO: is this necessary or useful?
    (.setFocusTraversable world false)
    (.setMouseTransparent world true)
    world))
;;----------------------------------------------------------------
;;(println (System/getProperty "glass.win.uiScale"))
(System/setProperty "glass.win.uiScale" "1")
;;(println (System/getProperty "glass.win.uiScale"))
;;(System/setProperty "javafx.pulseLogger" "true")
;;(System/setProperty "prism.verbose" "true")
;;(System/setProperty "prism.order" "d3d")
(JfxWorld/setWorldBuilder make-world)
(JfxWorld/launch JfxWorld (make-array String 0))
