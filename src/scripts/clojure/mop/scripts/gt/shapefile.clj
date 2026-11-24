;;(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;---------------------------------------------------------------------
;; clj src\scripts\clojure\mop\scripts\gt\shapefile.clj
;;---------------------------------------------------------------------
(ns mop.scripts.gt.shapefile
  {:doc     "Experiment with shapefile parsing via geotools."
   :author  "palisades dot lakes at gmail dot com"
   :version "2026-11-22"}
  (:require
   [mop.gt.gt :as gt]
   [mop.jfx.jfx :as jfx])
  (:import
   [javafx.scene Group]
   [javafx.scene.paint Color]))
;;---------------------------------------------------------------------
(let [polygons (gt/read-jts-geometries "data/natural-earth/110m_physical/ne_110m_land.shp")
      ^Group group (jfx/node polygons
                     (Color/web "#88888888")
                     (Color/web "#000088FF")
                     )]
  (println group)
  (dorun (map println (.getChildren group))))
