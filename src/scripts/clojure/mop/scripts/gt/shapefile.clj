;;(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;---------------------------------------------------------------------
;; clj src\scripts\clojure\mop\scripts\gt\shapefile.clj
;;---------------------------------------------------------------------
(ns mop.scripts.gt.shapefile
  {:doc     "Experiment with shapefile parsing via geotools."
   :author  "palisades dot lakes at gmail dot com"
   :version "2026-03-14"}
  (:require
   [mop.io.shapefile :as shp])
  (:import
   [javafx.scene Group]))
;;---------------------------------------------------------------------
(let [polygons (shp/read-jts-geometries "data/natural-earth/ne_110m_land.shp")
      ^Group group (shp/jfx-node polygons)]
  (println group)
  (dorun (map println (.getChildren group))))