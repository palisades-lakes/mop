;;----------------------------------------------------------------
(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.gt.gt
  {:doc
   "Geotools functions.
   <br>
   Coordinate reference systems and transforms."
   :author  "palisades dot lakes at gmail dot com"
   :version "2026-04-10"}
  (:import
   [org.geotools.api.referencing.operation MathTransform]
   [org.geotools.geometry.jts JTS]
   [org.geotools.referencing CRS]
   [org.locationtech.jts.geom Coordinate CoordinateXY Geometry]))
;;----------------------------------------------------------------
;; TODO: operate at higher level to get input crs from input point somehow
;; TODO: defmulti on input object type, cover geometric libraries

(let [^MathTransform mt (CRS/findMathTransform
                         (CRS/decode "EPSG:4326", true)
                         (CRS/decode (str "AUTO2:97002," 0.0 "," 0.0), true))
      in (double-array 2 Double/NaN)
      out (double-array 2 Double/NaN)]
  (defn wgs84-to-stereographic [x]
    (cond
      (instance? Coordinate x) (let [^Coordinate x x]
                                 (aset in 0 (.getX x))
                                 (aset in 1 (.getY x))
                                 (.transform mt in 0 out 0 1)
                                 (CoordinateXY. (aget out 0) (aget out 1)))
      (instance? Geometry x) (JTS/transform ^Geometry x mt)
      :else (throw (UnsupportedOperationException.
                    (str "can't transform " x))))))