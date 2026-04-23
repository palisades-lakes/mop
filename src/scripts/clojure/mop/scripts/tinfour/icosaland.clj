;; mvn -q install & cljfx src\scripts\clojure\mop\scripts\tinfour\icosaland.clj
;;----------------------------------------------------------------
(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.scripts.tinfour.icosaland
  {:doc     "Use JavaFX to display a conformal delaunay triangles of
         natural earth boundaries."
   :author  "palisades dot lakes at gmail dot com"
   :version "2026-04-21"}

  (:require
   [mop.cmplx.complex :as cmplx]
   [mop.geom.icosahedron :as icosahedron]
   [mop.geom.mesh :as mesh]
   [mop.geom.s2 :as s2]
   [mop.gt.gt :as gt]
   [mop.jts.jts :as jts]
   [mop.tinfour.tinfour :as tinfour])
  (:import
   [java.util Collection]
   [javafx.scene Group]
   [mop.java.geom.mesh TriangleMesh]
   [mop.java.jfx JfxWorld]
   [org.locationtech.jts.geom
    GeometryCollection GeometryFactory]
   [org.locationtech.jts.geom.util GeometryCombiner]))
;;----------------------------------------------------------------
(defn ^GeometryCollection land-polygons [^GeometryFactory factory]
  (gt/read-jts-geometries
   #_"data/natural-earth/10m_physical/ne_10m_land.shp"
   #_"data/natural-earth/50m_physical/ne_50m_land.shp"
   "data/natural-earth/110m_physical/ne_110m_land.shp"
   factory))
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
        mesh (jts/mesh-polygons (ll-icosahedron) factory)
        land (land-polygons factory)
        meshland (GeometryCombiner/combine mesh land)
        ;; unions polygons
        ;;meshland (GeometryFixer/fix meshland)
        ;;_ (assert (.isSimple meshland))
        ;;_ (assert (.isValid meshland))
        ;;land (gt/wgs84-to-stereographic land)
        mesh-group (jts/jfx mesh "#00000000" "#0000FF88")
        land-group (jts/jfx land "#22990044" "#00FF0088")
        triangles (tinfour/cdt meshland meshland 1.0e-2 true)
        triangles-group (tinfour/jfx-group triangles "#00000000" "#FF000088")
        ;; 'children' binding with type hint seems necessary to avoid
        ;; reflection warnings; inline type hint gives warning?
        ^Collection children [mesh-group land-group triangles-group]
        world (Group. children)]
    (.setId world "world")
    world))
;;----------------------------------------------------------------
(System/setProperty "glass.win.uiScale" "1")
;;(System/setProperty "javafx.pulseLogger" "true")
;;(System/setProperty "prism.verbose" "true")
;;(System/setProperty "prism.order" "d3d")
(JfxWorld/setWorldBuilder make-world)
(JfxWorld/launch JfxWorld (make-array String 0))
