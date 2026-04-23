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
   [mop.cmplx.complex :as cmplx]
   [mop.geom.icosahedron :as icosahedron]
   [mop.geom.mesh :as mesh]
   [mop.geom.s2 :as s2]
   [mop.gt.gt :as gt]
   [mop.gt.gt :as gt]
   [mop.jts.jts :as jts])
  (:import
   [java.util Collection]
   [javafx.scene Group]
   [mop.java.geom.mesh TriangleMesh]
   [mop.java.jfx JfxWorld]
   [org.locationtech.jts.geom Geometry GeometryCollection GeometryFactory]))
;;----------------------------------------------------------------
(defn ^GeometryCollection land-polygons [^GeometryFactory factory]
  (let [geometries (gt/read-jts-geometries
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
(defn ^TriangleMesh ll-icosahedron []
  (let [^TriangleMesh u2-mesh ((comp
                                #_cmplx/midpoint-subdivide-4)
                               (icosahedron/u2-cut-icosahedron))
        embedding (into {} (map (fn [[k v]] [k (jts/coordinate (s2/to-ll v))])
                                (.embedding u2-mesh)))]
    (mesh/triangle-mesh (.cmplx u2-mesh) embedding)))

;;----------------------------------------------------------------
(defn make-world []
  (let [factory (GeometryFactory.)
        land (land-polygons factory)
        land-group (jts/jfx land "#22990044" "#000088FF")
        mesh (ll-icosahedron)
        ;;polygons (jts/mesh-polygons mesh factory)
        ;;polygons (gt/wgs84-to-stereographic polygons)
        ;;polygons-group (jts/jfx polygons "#FFFFFF00" "#FF0000FF")
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
