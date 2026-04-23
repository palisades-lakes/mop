;; mvn -q install & cljfx src\scripts\clojure\mop\scripts\jts\icosaland.clj
;;----------------------------------------------------------------
(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.scripts.jts.icosaland
  {:doc     "Use JavaFX to display a conformal delaunay triangles of
         natural earth boundaries."
   :author  "palisades dot lakes at gmail dot com"
   :version "2026-04-23"}

  (:require
   [mop.cmplx.complex :as cmplx]
   [mop.geom.icosahedron :as icosahedron]
   [mop.geom.mesh :as mesh]
   [mop.geom.s2 :as s2]
   [mop.gt.gt :as gt]
   [mop.jts.jts :as jts])
  (:import
   [java.util Collection]
   [javafx.scene Group]
   [mop.java.geom.mesh TriangleMesh]
   [mop.java.jfx JfxWorld]
   [org.locationtech.jts.geom
    GeometryCollection GeometryFactory PrecisionModel]
   [org.locationtech.jts.geom.util GeometryCombiner LinearComponentExtracter]
   [org.locationtech.jts.operation.overlayng OverlayNGRobust]))
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
                                #_cmplx/midpoint-subdivide-4
                                cmplx/midpoint-subdivide-4
                                cmplx/midpoint-subdivide-4)
                               (icosahedron/u2-cut-icosahedron))
        embedding (into {} (map (fn [[k v]] [k (jts/coordinate (s2/to-ll v))])
                                (.embedding u2-mesh)))]
    (mesh/triangle-mesh (.cmplx u2-mesh) embedding)))
;;----------------------------------------------------------------
(defn make-world []
  (let [tolerance 5.0e-5
        ^PrecisionModel pm (jts/precision-model tolerance)
        factory (GeometryFactory. pm)
        mesh (jts/mesh-polygons (ll-icosahedron) factory)
        land (land-polygons factory)
        meshland (GeometryCombiner/combine mesh land)
        points (jts/unique-points meshland tolerance)
        lines (LinearComponentExtracter/getGeometry meshland true)
        lines (OverlayNGRobust/union lines)
        triangles (jts/cdt points lines tolerance true)
        ;;land (gt/wgs84-to-stereographic land)
        mesh-group (jts/jfx mesh "#00000000" "#0000FF88")
        land-group (jts/jfx land "#22990044" "#00FF0088")
        triangles-group (jts/jfx triangles "#00000000" "#FF000088")
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
