(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;------------------------------------------------------------------
;; TODO: move to geotools package/namespace(s)
(ns mop.io.shapefile
  {:doc
   "Read shapefiles into jts geometries, using geotools."
   :author  "palisades dot lakes at gmail dot com"
   :version "2026-04-07"}
  (:require
   [clojure.java.io :as io])
  (:import
   [org.geotools.api.data
    DataStore FileDataStoreFinder SimpleFeatureSource]
   [org.geotools.api.feature.simple
    SimpleFeature]
   [org.geotools.data.simple
    SimpleFeatureCollection SimpleFeatureIterator]
   [org.locationtech.jts.geom
    GeometryCollection GeometryFactory MultiPolygon Polygon]))
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
         geometries (.createGeometryCollection factory polygons)]
     #_(assert (.isValid geometries))
     (.dispose store)
     geometries))
  ([shp] (read-jts-geometries shp (GeometryFactory.))))
;;---------------------------------------------------------------------
