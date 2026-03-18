(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;------------------------------------------------------------------
(ns mop.io.shapefile
  {:doc
   "Read shapefiles into jts geometries, using geotools.
   <br>
   Convert to JFX shapes, mop meshes?"
   :author  "palisades dot lakes at gmail dot com"
   :version "2026-03-76"}
  (:require
   [clojure.java.io :as io])
  (:import
   [javafx.scene Group]
   [javafx.scene.paint Color]
   [javafx.scene.shape StrokeType]
   [org.geotools.api.data
    DataStore DataStoreFinder]
   [org.geotools.api.feature.simple
    SimpleFeature]
   [org.locationtech.jts.geom
    Coordinate GeometryCollection GeometryFactory MultiPolygon Polygon]))
;;---------------------------------------------------------------------
;; TODO: or java compile/run-time dispatch based on type of input

(defmulti ^javafx.scene.Node jfx-node
          (fn [g _ _] (class g)))

(defmethod jfx-node Polygon [^Polygon jts ^Color fill ^Color stroke]
  (let [coords (.getCoordinates jts)
        n (alength coords)
        ^doubles xys (make-array Double/TYPE (* 2 n))]
    (dotimes [i n]
      (let [^Coordinate coord (aget coords i)]
        (aset xys (* 2 i) (.getX coord))
        (aset xys (inc (* 2 i)) (.getY coord))))
    (let [polygon (javafx.scene.shape.Polygon. xys)]
      (.setFill polygon fill)
      (.setStroke polygon stroke)
      (.setStrokeWidth polygon 0.25)
      (.setStrokeType polygon StrokeType/OUTSIDE)
      polygon)))

(defmethod jfx-node MultiPolygon [^MultiPolygon jts ^Color fill ^Color stroke]
  (let [group (Group.)
        children (.getChildren group)
        n (.getNumGeometries jts)]
    (dotimes [i n]
      (.add children (jfx-node (.getGeometryN jts i) fill stroke)))
    group))
;;---------------------------------------------------------------------
(defn read-jts-geometries ^GeometryCollection [shp]
  (let [shp (.toURL (.toURI (io/file shp)))
        ^DataStore store (DataStoreFinder/getDataStore {"url" shp})
        ;; TODO: handle multiple type names
        ^String name (first (into [] (.getTypeNames store)))
        features (.toArray (.getFeatures (.getFeatureSource store name)))
        n (alength features)
        ^"[Lorg.locationtech.jts.geom.Polygon;" polygons (make-array Polygon n)]
    (dotimes [i n]
      (let [^SimpleFeature feature (aget features i)
            ^MultiPolygon multipolygon (.getDefaultGeometry feature)]
        ;; TODO: handle multiple polygons in each multipolygon
        ;; TODO: preserve IDs
        (assert (= 1 (.getNumGeometries multipolygon))
                (str (.getID feature) " "
                     (.getNumGeometries multipolygon)))
        (aset polygons i (.getGeometryN multipolygon 0))))
    ;; TODO: reuse GeometryFactory's
    (MultiPolygon. polygons (GeometryFactory.))))
