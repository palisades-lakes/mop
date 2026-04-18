;; mvn -q -DskipTests -Dmaven.test.skip=true install & cljfx src\scripts\clojure\mop\scripts\jts\debug.clj
;;----------------------------------------------------------------
(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.scripts.jts.debug
  {:doc     "Conformal delaunay triangulation of a cut, projected icosahedron."
   :author  "palisades dot lakes at gmail dot com"
   :version "2026-04-13"}
  (:require
   [clojure.math :as math]
   [mop.cmplx.complex :as cmplx]
   [mop.geom.mesh :as mesh]
   [mop.geom.rn :as rn]
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
    (let [a (cmplx/simplex "a")
          b (cmplx/simplex "b") c (cmplx/simplex "c") d (cmplx/simplex "d")
          e (cmplx/simplex "e") f (cmplx/simplex "f") g (cmplx/simplex "g")
          h (cmplx/simplex "h") i (cmplx/simplex "i") j (cmplx/simplex "j")
          k (cmplx/simplex "k") l (cmplx/simplex "l") m (cmplx/simplex "m")
          n (cmplx/simplex "n")
          ;hn (cmplx/simplex "hn")
          ;mn (cmplx/simplex "mn")
          ;ab (cmplx/simplex "ab")
          ;ag (cmplx/simplex "ag")
          ;; TODO: problem with txt coordinates when cut at exactly 0 and 2PI
          u2-embedding {a Point2U/PLUS_K
                        b (Point2U/of (+ a0 (* 0 da)) p1)
                        c (Point2U/of (+ a0 (* 2 da)) p1)
                        d (Point2U/of (+ a0 (* 4 da)) p1)
                        e (Point2U/of (+ a0 (* 6 da)) p1)
                        f (Point2U/of (+ a0 (* 8 da)) p1)
                        g (Point2U/of (+ a0 (* 10 da)) p1)
                        h (Point2U/of (+ a0 (* -1 da)) p2)
                        i (Point2U/of (+ a0 (* 1 da)) p2)
                        j (Point2U/of (+ a0 (* 3 da)) p2)
                        k (Point2U/of (+ a0 (* 5 da)) p2)
                        l (Point2U/of (+ a0 (* 7 da)) p2)
                        m (Point2U/of (+ a0 (* 9 da)) p2)
                        n Point2U/MINUS_K}
          ll-embedding (into {} (map (fn [[k v]] [k (s2/to-ll v)]) u2-embedding))
          ;; ll-embedding
          #_(merge
             ll-embedding
             {hn (rn/interpolate (ll-embedding h) 0.4 (ll-embedding n))
              mn (rn/interpolate (ll-embedding m) 0.4 (ll-embedding n))
              ab (rn/interpolate (ll-embedding b) 0.35 (ll-embedding a))
              ag (rn/interpolate (ll-embedding g) 0.35 (ll-embedding a))})
          cmplx (cmplx/simplicial-complex-2d
                 (map #(apply cmplx/simplex %)
                      [
                       ;[a b c]
                       ;[a c d]
                       ;[a d e] [a e f]
                       ;[a f g]
                       ;[b i c] [c j d] [d k e] [e l f] [f m g]
                       ;[b h i] [c i j] [d j k] [e k l] [f l m]
                       [n i h]
                       ;[n j i]
                       ;[n k j]
                       ;[n l k]
                       ;[n m l]
                       ]))
          ]
      (mesh/triangle-mesh cmplx ll-embedding)
      )))
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
        polygons (jts/mesh-polygons mesh factory)
        _ (jts/write-wkt polygons "out/wkt/polygons.wkt")
        _ (jts/print-aspect-ratios polygons)
        ;polygons-group (jts/jfx polygons "#FFFFFF00" "#FF0000FF")
        edges (jts/mesh-linestrings mesh factory)
        _ (jts/write-wkt edges "out/wkt/edges.wkt")
        ;;edges-group (jts/jfx edges "#FFFFFF00" "#FF0000FF")
        points (jts/mesh-points mesh factory)
        _ (jts/write-wkt points "out/wkt/points.wkt")
        ^Geometry cdtb (jts/cdt points edges 0.0e1)
        _ (jts/print-aspect-ratios cdtb)
        _ (.setUserData cdtb "triangulation")
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
