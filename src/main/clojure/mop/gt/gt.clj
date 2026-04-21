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
  (:require [clojure.java.io :as io])
  (:import
   [org.geotools.api.data DataStore FileDataStoreFinder SimpleFeatureSource]
   [org.geotools.api.feature.simple SimpleFeature]
   [org.geotools.api.referencing.operation MathTransform]
   [org.geotools.data.simple SimpleFeatureCollection SimpleFeatureIterator]
   [org.geotools.geometry.jts JTS]
   [org.geotools.referencing CRS]
   [org.locationtech.jts.geom
    Coordinate CoordinateXY Geometry GeometryCollection GeometryFactory
    MultiPolygon Polygon]))
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
;;---------------------------------------------------------------------
(defn- extract-polygons [^SimpleFeature feature]
  (let [^String id (.getID feature)
        ^MultiPolygon multipolygon (.getDefaultGeometry feature)
        nGeometries (.getNumGeometries multipolygon)
        ^Polygon/1 polygons (make-array Polygon nGeometries)]
    (dotimes [i nGeometries]
      (let [^Polygon polygon (.getGeometryN multipolygon i)]
        (.setUserData polygon id)
        (aset polygons i polygon)))
    (seq polygons)))

(defn- collect-polygons [^SimpleFeatureCollection featureCollection]
  (with-open [^SimpleFeatureIterator iterator (.features featureCollection)]
    (loop [polygons (transient [])]
      (if (.hasNext iterator)
        (recur (conj! polygons (extract-polygons (.next iterator))))
        ;; else
        (persistent! polygons)))))

(defn ^GeometryCollection read-jts-geometries
  ([shp ^GeometryFactory factory]
   (let [^DataStore store (FileDataStoreFinder/getDataStore (io/file shp))
         ;; TODO: handle multiple type names
         ^String name (first (into [] (.getTypeNames store)))
         ^SimpleFeatureSource featureSource (.getFeatureSource store name)
         ^SimpleFeatureCollection featureCollection (.getFeatures featureSource)
         ^Polygon/1
         polygons (into-array
                   org.locationtech.jts.geom.Polygon
                   (flatten (collect-polygons featureCollection)))
         geometries (.createMultiPolygon factory polygons)]
     #_(assert (.isValid geometries))
     (.dispose store)
     geometries))
  ([shp] (read-jts-geometries shp (GeometryFactory.))))
;;---------------------------------------------------------------------
