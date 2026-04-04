(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.scripts.jts.cdt
  {:doc     "Conforming delaunay triangulation."
   :author  "palisades dot lakes at gmail dot com"
   :version "2026-04-04"}

  (:require
   [mop.commons.time :as mct]
   [mop.io.shapefile :as miosh])

  (:import
   [org.locationtech.jts.geom
    GeometryFactory]
   [org.locationtech.jts.simplify TopologyPreservingSimplifier]
   [org.locationtech.jts.triangulate
    ConformingDelaunayTriangulationBuilder]))
;;----------------------------------------------------------------
;; mvn -q install & clj src\scripts\clojure\mop\scripts\jts\cdt.clj
;;----------------------------------------------------------------
(let [gfactory (GeometryFactory.)
      land (miosh/read-jts-geometries
            "data/natural-earth/10m_physical/ne_10m_land.shp"
            #_"data/natural-earth/50m_physical/ne_50m_land.shp"
            #_"data/natural-earth/ne_110m_land.shp"
            gfactory)
      _ (println "land: " (.getNumGeometries land))
      tolerance 0.75
      simple (mct/seconds
              "simplify"
              (TopologyPreservingSimplifier/simplify land tolerance))
      _ (println "simple: " (.getNumGeometries simple))
      cdtb (ConformingDelaunayTriangulationBuilder.)
      _ (.setTolerance cdtb tolerance)
      _ (.setSites cdtb simple)
      _ (.setConstraints cdtb simple)
      triangles (mct/seconds "triangulate" (.getTriangles cdtb gfactory))
      ]
  (println "triangles: " (.getNumGeometries triangles))
  )