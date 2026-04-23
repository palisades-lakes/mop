;; mvn -q -DskipTests -Dmaven.test.skip=true install & cljfx src\scripts\clojure\mop\scripts\jts\lines.clj
;;----------------------------------------------------------------
(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.scripts.jts.lines
  {:doc "Fix and simplify invalid geometries."
   :author  "palisades dot lakes at gmail dot com"
   :version "2026-04-23"}

  (:require
   [mop.jts.jts :as jts])
  (:import
   [org.locationtech.jts.geom GeometryFactory PrecisionModel]
   [org.locationtech.jts.geom.util GeometryCombiner]
   [org.locationtech.jts.operation.overlayng
    OverlayNGRobust PrecisionUtil]))
;;----------------------------------------------------------------
  (let [pm (PrecisionModel. 1.0e6)
        factory (GeometryFactory. pm)
        line0 (jts/read-wkt-string factory "LINESTRING (0 0, 1 1)")
        line1 (jts/read-wkt-string factory "LINESTRING (0 1, 1 0)")
        line2 (jts/read-wkt-string factory "LINESTRING (1.0e-7 1, 1 1.0e-7)")
        lines (GeometryCombiner/combine line0 line1 line2)
        union (OverlayNGRobust/union lines)]
    (assert (.isSimple union))
    (assert (.isValid union))
    (println (.toString pm))
    (println "lines:\n" (.toString lines))
    (println "lines precision:\n"
             (.toString (PrecisionUtil/robustPM lines)))
    (println "union:\n" (.toString union))
    (println "union precision:\n"
             (.toString (PrecisionUtil/robustPM union))))
;;----------------------------------------------------------------
