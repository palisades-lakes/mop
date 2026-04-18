;; mvn -q -DskipTests -Dmaven.test.skip=true install & cljfx src\scripts\clojure\mop\scripts\jts\cdtb3points.clj
;;----------------------------------------------------------------
(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.scripts.jts.cdtb3points
  {:doc     "Conformal delaunay triangulation of of 3 sites, no constraints."
   :author  "palisades dot lakes at gmail dot com"
   :version "2026-04-13"}
  (:require
   [clojure.math :as math]
   [mop.cmplx.complex :as cmplx]
   [mop.geom.mesh :as mesh]
   [mop.geom.s2 :as s2]
   [mop.jts.jts :as jts])
  (:import
   [java.util Collection]
   [javafx.scene Group]
   [mop.java.geom Point2U]
   [mop.java.geom.mesh TriangleMesh]
   [mop.java.jfx JfxWorld]
   [org.locationtech.jts.geom Geometry GeometryFactory]))
;;----------------------------------------------------------------
(let [a0 (- -0.1 Math/PI)
      da (double (* Math/PI 0.2))
      p1 (double (math/atan 2.0))
      p2 (double (- Math/PI p1))]
  (defn ^TriangleMesh icosacap []
    (let [ h (cmplx/simplex "h") i (cmplx/simplex "i") n (cmplx/simplex "n")
          ;; TODO: problem with txt coordinates when cut at exactly 0 and 2PI
          u2-embedding {h (Point2U/of (+ a0 (* -1 da)) p2)
                        i (Point2U/of (+ a0 (* 1 da)) p2)
                        n Point2U/MINUS_K}
          ll-embedding (into {} (map (fn [[k v]] [k (s2/to-ll v)]) u2-embedding))
          cmplx (cmplx/simplicial-complex-2d
                 (map #(apply cmplx/simplex %) [[n i h]]))]
      (mesh/triangle-mesh cmplx ll-embedding))))
;;----------------------------------------------------------------
(defn make-world []
  (let [factory (GeometryFactory.)
        ^TriangleMesh ll-mesh ((comp
                                #_cmplx/midpoint-subdivide-4
                                #_cmplx/midpoint-subdivide-4)
                               (icosacap))
        embedding (into {} (map (fn [[k v]] [k (jts/coordinate v)])
                                (.embedding ll-mesh)))
        mesh (mesh/triangle-mesh (.cmplx ll-mesh) embedding)
        ;;polygons (jts/mesh-polygons mesh factory)
        points (jts/mesh-points mesh factory)
        _ (jts/write-wkt points "out/wkt/points3.wkt")
        ^Geometry cdtb (jts/cdt points nil 0.0)
        _ (jts/print-aspect-ratios cdtb)
        _ (.setUserData cdtb "cdtb3points")
        _ (jts/write-wkt cdtb "out/wkt/cdtb.wkt")
        _ (jts/assert-valid cdtb)
        cdtb-group (jts/jfx cdtb "#FFFFFF00" "#0000FF88")
        ;; 'children' binding with type hint seems necessary to avoid
        ;; reflection warnings; inline type hint gives warning?
        ^Collection children [#_edges-group cdtb-group]
        world (Group. children)]
    (.setId world "world")
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
