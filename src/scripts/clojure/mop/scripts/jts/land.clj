;; mvn -q install & cljfx src\scripts\clojure\mop\scripts\jts\land.clj
;;----------------------------------------------------------------
(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.scripts.jts.land
  {:doc     "Use JavaFX to display a conformal delaunay triangles of
  natural earth boundaries."
   :author  "palisades dot lakes at gmail dot com"
   :version "2026-04-11"}

  (:require
   [mop.gt.gt :as gt]
   [mop.io.shapefile :as miosh]
   [mop.jts.jts :as jts])
  (:import
   [java.util Collection]
   [javafx.scene Group]
   [mop.java.jfx JfxWorld]
   [org.locationtech.jts.geom
    GeometryCollection GeometryFactory]))
;;----------------------------------------------------------------
(defn ^GeometryCollection land-polygons [^GeometryFactory factory]
  (miosh/read-jts-geometries
   #_"data/natural-earth/10m_physical/ne_10m_land.shp"
   #_"data/natural-earth/50m_physical/ne_50m_land.shp"
   "data/natural-earth/110m_physical/ne_110m_land.shp"
   factory))
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
    world))
;;----------------------------------------------------------------
(System/setProperty "glass.win.uiScale" "1")
;;(System/setProperty "javafx.pulseLogger" "true")
;;(System/setProperty "prism.verbose" "true")
;;(System/setProperty "prism.order" "d3d")
(JfxWorld/setWorldBuilder make-world)
(JfxWorld/launch JfxWorld (make-array String 0))
