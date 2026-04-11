;; mvn -q -DskipTests -Dmaven.test.skip=true install & cljfx src\scripts\clojure\mop\scripts\jts\cdt.clj
;;----------------------------------------------------------------
(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.scripts.jts.cdt
  {:doc     "Conforming delaunay triangulation.
  Natural earth boundaries and regular icosahedral triangulation
  as constraints."
   :author  "palisades dot lakes at gmail dot com"
   :version "2026-04-11"}
  (:require
   [clojure.math :as math]
   [mop.cmplx.complex :as cmplx]
   [mop.geom.mesh :as mesh]
   [mop.geom.s2 :as s2]
   [mop.gt.gt :as gt]
   [mop.io.shapefile :as miosh]
   [mop.jts.jts :as jts])
  (:import
   [java.util Collection]
   [javafx.scene Group]
   [mop.java.cmplx TwoSimplex]
   [mop.java.geom Point2U]
   [mop.java.geom.mesh TriangleMesh]
   [mop.java.jfx JfxWorld]
   [org.locationtech.jts.geom Geometry GeometryCollection GeometryFactory]))
;;----------------------------------------------------------------
(defn filter-mesh [predicate ^TriangleMesh mesh]
  (mesh/triangle-mesh
   (cmplx/simplicial-complex-2d
    (filter predicate (cmplx/faces (.cmplx mesh))))
   (.embedding mesh)))
(defn not-containing-vertex-by-name [^TwoSimplex face name]
  (not (or (= name (.name (.z0 face)))
           (= name (.name (.z1 face)))
           (= name (.name (.z2 face))))))
(defn remove-faces-w-vtx [^TriangleMesh mesh z]
  (filter-mesh #(not-containing-vertex-by-name % z) mesh))
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
          cmplx (cmplx/simplicial-complex-2d
                 (map #(apply cmplx/simplex %)
                      [
                       [a b c]
                       [a c d]
                       [a d e] [a e f]
                       [a f g]
                       [b i c] [c j d] [d k e] [e l f] [f m g]
                       [b h i] [c i j] [d j k] [e k l] [f l m]
                       [n i h]
                       [n j i]
                       [n k j]
                       [n l k]
                       [n m l]
                       ]))
          ]
      (mesh/triangle-mesh cmplx ll-embedding)
      )))
;;----------------------------------------------------------------
(defn make-world []
  (let [factory (GeometryFactory.)
        land (land-polygons factory)
        land (gt/wgs84-to-stereographic land)
        land-group (jts/jfx land "#22990044" "#000088FF")
        ^TriangleMesh u2-mesh ((comp
                                cmplx/midpoint-subdivide-4
                                cmplx/midpoint-subdivide-4
                                cmplx/midpoint-subdivide-4)
                               (icosacap))
        ^TriangleMesh u2-mesh (remove-faces-w-vtx u2-mesh "a")
        ;^TriangleMesh u2-mesh (remove-faces-w-vtx u2-mesh "n")
        embedding (into {} (map
                            (fn [[k v]] [k (jts/coordinate v)])
                            (.embedding u2-mesh)))
        mesh (mesh/triangle-mesh (.cmplx u2-mesh) embedding)
        polygons (jts/mesh-polygons mesh factory)
        polygons (gt/wgs84-to-stereographic polygons)
        polygons-group (jts/jfx polygons "#FFFFFF00" "#FF0000FF")
        points (jts/mesh-points mesh factory)
        points (gt/wgs84-to-stereographic points)
        ^Geometry triangles (jts/cdt points land 1.0)
        _ (.setUserData triangles "triangulation")
        ;;_ (jts/assert-valid triangles)
        triangulation-group (jts/jfx triangles "#FFFFFF00" "#0000FF88")
        ;; 'children' binding with type hint seems necessary to avoid
        ;; reflection warnings; inline type hint gives warning?
        ^Collection children [land-group #_polygons-group triangulation-group]
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
