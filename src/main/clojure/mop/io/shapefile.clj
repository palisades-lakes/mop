(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;------------------------------------------------------------------
(ns mop.io.shapefile
  {:doc
   "Read shapefiles into jts geometries, using geotools.
   <br>
   Convert to JFX shapes, mop meshes?"
   :author  "palisades dot lakes at gmail dot com"
   :version "2026-03-18"}
  (:require
   [clojure.java.io :as io])
  (:import
   [javafx.scene Group]
   [javafx.scene.paint Color]
   [javafx.scene.shape Shape StrokeType]
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

#_(defmethod jfx-node Polygon [^Polygon jts ^Color fill ^Color stroke]
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
        (.setStrokeWidth polygon 0.3)
        (.setStrokeType polygon StrokeType/INSIDE)
        polygon)))

(defn- ^doubles jts-coords-to-doubles [^"[Lorg.locationtech.jts.geom.Coordinate;" coords]
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
    (.setFill polygon fill)
    (.setStroke polygon stroke)
    (.setStrokeWidth polygon 0.3)
    (.setStrokeType polygon StrokeType/INSIDE)
    polygon))

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
