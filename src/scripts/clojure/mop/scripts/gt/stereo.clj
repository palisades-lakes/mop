;; mvn -DskipTests -q install & cljfx src\scripts\clojure\mop\scripts\gt\stereo.clj
;;----------------------------------------------------------------
(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.scripts.gt.stereo
  {:doc     "Work out stereographic projection."
   :author  "palisades dot lakes at gmail dot com"
   :version "2026-04-10"}
  (:import
   [java.util Arrays]
   [org.geotools.api.referencing.crs CoordinateReferenceSystem]
   [org.geotools.api.referencing.operation MathTransform]
   [org.geotools.referencing CRS]))
;;----------------------------------------------------------------
  (let [
        ^CoordinateReferenceSystem
        crs-out (CRS/decode (str "AUTO2:97002," 0.0 "," 0.0), true)
        ^CoordinateReferenceSystem crs-in (CRS/decode "EPSG:4326", true)
        ^MathTransform mt (CRS/findMathTransform crs-in crs-out)
        ^doubles x (into-array Double/TYPE [0.0 0.0])
        ^doubles y (into-array Double/TYPE [Double/NaN Double/NaN])
        ]
    (println crs-out)
    (println crs-in)
    (println mt)
    (.transform mt x 0 y 0 1)
    (println (Arrays/toString y))
  )
