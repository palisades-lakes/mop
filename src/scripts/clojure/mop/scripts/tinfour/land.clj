;; mvn -q install & cljfx src\scripts\clojure\mop\scripts\tinfour\land.clj
;;----------------------------------------------------------------
(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.scripts.tinfour.land
  {:doc "Use JavaFX to display a conformal delaunay triangles of
         natural earth boundaries."
   :author  "palisades dot lakes at gmail dot com"
   :version "2026-04-20"}

  (:require
   [mop.gt.gt :as gt]
   [mop.jts.jts :as jts]
   [mop.tinfour.tinfour :as tinfour])
  (:import
   [java.util Collection]
   [javafx.scene Group]
   [mop.java.jfx JfxWorld]
   [org.locationtech.jts.geom
    GeometryCollection GeometryFactory]))
;;----------------------------------------------------------------
(defn ^GeometryCollection land-polygons [^GeometryFactory factory]
  (gt/read-jts-geometries
   #_"data/natural-earth/10m_physical/ne_10m_land.shp"
   #_"data/natural-earth/50m_physical/ne_50m_land.shp"
   "data/natural-earth/110m_physical/ne_110m_land.shp"
   factory))
;;----------------------------------------------------------------
(defn make-world []
  (let [factory (GeometryFactory.)
        land (land-polygons factory)
        ;;land (gt/wgs84-to-stereographic land)
        land-group (jts/jfx land "#22990044" "#FF0000FF")
        triangles (tinfour/cdt land land 1.0e-6)
        triangles-group (tinfour/jfx-group triangles "#FFFFFF00" "#88888888")
        ;; 'children' binding with type hint seems necessary to avoid
        ;; reflection warnings; inline type hint gives warning?
        ^Collection children [land-group triangles-group]
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
