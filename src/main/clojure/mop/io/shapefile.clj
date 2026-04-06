(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;------------------------------------------------------------------
(ns mop.io.shapefile
  {:doc
   "Read shapefiles into jts geometries, using geotools.
    Convert to JFX shapes, mop meshes?"
   :author  "palisades dot lakes at gmail dot com"
   :version "2026-04-06"}
  (:require
   [clojure.java.io :as io])
  (:import
   [javafx.scene Group]
   [javafx.scene.paint Color]
   [javafx.scene.shape Shape StrokeType]
   [org.geotools.api.data
    DataStore FileDataStoreFinder SimpleFeatureSource]
   [org.geotools.api.feature.simple
    SimpleFeature]
   [org.geotools.data.simple
    SimpleFeatureCollection SimpleFeatureIterator]
   [org.locationtech.jts.geom
    Coordinate GeometryCollection GeometryFactory MultiPolygon Polygon]))
;;---------------------------------------------------------------------
;; TODO: or java compile/run-time dispatch based on type of input

(defmulti ^javafx.scene.Node jfx-node
          (fn [g _ _] (class g)))

(defn- ^doubles
  jts-coords-to-doubles [^"[Lorg.locationtech.jts.geom.Coordinate;" coords]
  (let [n (alength coords)
        ^doubles xys (make-array Double/TYPE (* 2 n))]
    (dotimes [i n]
      (let [^Coordinate coord (aget coords i)]
        (aset xys (* 2 i) (.getX coord))
        (aset xys (inc (* 2 i)) (.getY coord))))
    xys))

(defmethod jfx-node Polygon [^Polygon jts ^Color fill ^Color stroke]
  (let [exterior (javafx.scene.shape.Polygon.
                  (jts-coords-to-doubles
                   (.getCoordinates (.getExteriorRing jts))))
        ;; TODO: assuming all interior rings are holes?
        ;; TODO: Holes in holes? eg islands in lakes?
        n-holes (.getNumInteriorRing jts)
        ^Shape polygon (loop [^Shape polygon exterior
                              i 0]
                         (if (>= i n-holes)
                           polygon
                           (let [^Shape hole (javafx.scene.shape.Polygon.
                                              (jts-coords-to-doubles
                                               (.getCoordinates
                                                (.getInteriorRingN jts i))))]
                             (recur (Shape/subtract polygon hole) (inc i)))))]
    (.setId polygon (.getUserData jts))
    (when fill (.setFill polygon fill))
    (.setStroke polygon stroke)
    (.setStrokeWidth polygon 1)
    (.setStrokeType polygon StrokeType/INSIDE)
    polygon))

(defmethod jfx-node MultiPolygon [^MultiPolygon jts ^Color fill ^Color stroke]
  (let [group (Group.)
        children (.getChildren group)
        n (.getNumGeometries jts)]
    (.setId group (.getUserData jts))
    (dotimes [i n]
      (.add children (jfx-node (.getGeometryN jts i) fill stroke)))
    group))

(defmethod jfx-node GeometryCollection [^GeometryCollection jts ^Color fill ^Color stroke]
  (let [group (Group.)
        children (.getChildren group)
        n (.getNumGeometries jts)]
    (.setId group (.getUserData jts))
    (dotimes [i n]
      (.add children (jfx-node (.getGeometryN jts i) fill stroke)))
    group))
;;---------------------------------------------------------------------

(defn- extract-polygons [^SimpleFeature feature]
  (let [^String id (.getID feature)
        ^MultiPolygon multipolygon (.getDefaultGeometry feature)
        nGeometries (.getNumGeometries multipolygon)
        ^"[Lorg.locationtech.jts.geom.Polygon;" polygons (make-array Polygon nGeometries)]
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
         ^"[Lorg.locationtech.jts.geom.Polygon;"
         polygons (into-array
                   org.locationtech.jts.geom.Polygon
                   (flatten (collect-polygons featureCollection)))
         geometries (.createGeometryCollection factory polygons)]
     #_(assert (.isValid geometries))
     (.dispose store)
     geometries))
  ([shp] (read-jts-geometries shp (GeometryFactory.))))
;;---------------------------------------------------------------------
