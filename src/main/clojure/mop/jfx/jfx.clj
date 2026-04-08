(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;------------------------------------------------------------------
(ns mop.jfx.jfx
  {:doc
   "Java FX utilities"
   :author  "palisades dot lakes at gmail dot com"
   :version "2026-04-07"}
  (:import
   [javafx.scene Group]
   [javafx.scene.paint Color]
   [javafx.scene.shape Polyline Shape StrokeType]
   [org.locationtech.jts.geom
    Coordinate GeometryCollection LineString MultiPolygon Polygon]))
;;---------------------------------------------------------------------
;; TODO: or java compile/run-time dispatch based on type of input

(defmulti ^javafx.scene.Node node
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

(defmethod node Polygon [^Polygon jts ^Color fill ^Color stroke]
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
    (.setId polygon (str (.getUserData jts)))
    (when fill (.setFill polygon fill))
    (.setStroke polygon stroke)
    (.setStrokeWidth polygon 1)
    (.setStrokeType polygon StrokeType/INSIDE)
    polygon))

(defmethod node LineString [^LineString jts ^Color fill ^Color stroke]
  (let [^Polyline polyline (javafx.scene.shape.Polyline.
                            (jts-coords-to-doubles
                             (.getCoordinates jts)))]
    (.setId polyline (str (.getUserData jts)))
    ;; ignore fill
    ;;(when fill (.setFill polyline fill))
    (.setStroke polyline stroke)
    (.setStrokeWidth polyline 1)
    (.setStrokeType polyline StrokeType/CENTERED)
    polyline))

(defmethod node MultiPolygon [^MultiPolygon jts ^Color fill ^Color stroke]
  (let [group (Group.)
        children (.getChildren group)
        n (.getNumGeometries jts)]
    (.setId group (str (.getUserData jts)))
    (dotimes [i n]
      (.add children (node (.getGeometryN jts i) fill stroke)))
    group))

(defmethod node GeometryCollection [^GeometryCollection jts ^Color fill ^Color stroke]
  (let [group (Group.)
        children (.getChildren group)
        n (.getNumGeometries jts)]
    (.setId group (str (.getUserData jts)))
    (dotimes [i n]
      (.add children (node (.getGeometryN jts i) fill stroke)))
    group))
;;---------------------------------------------------------------------
