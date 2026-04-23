;; mvn -q -DskipTests -Dmaven.test.skip=true install & cljfx src\scripts\clojure\mop\scripts\tinfour\lines.clj
;;----------------------------------------------------------------
(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.scripts.tinfour.lines
  {:doc "Use JavaFX to display a conformal delaunay triangles of
         natural earth boundaries."
   :author  "palisades dot lakes at gmail dot com"
   :version "2026-04-22"}

  (:require
   [mop.jts.jts :as jts])
  (:import
   [org.locationtech.jts.geom GeometryFactory]
   [org.locationtech.jts.geom.util GeometryCombiner]
   [org.locationtech.jts.operation.overlayng OverlayNGRobust]))
;;----------------------------------------------------------------
  (let [factory (GeometryFactory.)
        line0 (jts/read-wkt-string factory "LINESTRING (0 0, 1 1)")
        line1 (jts/read-wkt-string factory "LINESTRING (0 1, 1 0)")
        line2 (jts/read-wkt-string factory "LINESTRING (1.0e-6 1, 1 1.0e-6)")
        lines (GeometryCombiner/combine line0 line1 line2)
        union (OverlayNGRobust/union lines)]
    (println "lines: " (.toString lines))
    (println "union: " (.toString union)))
;;----------------------------------------------------------------
