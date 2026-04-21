;;----------------------------------------------------------------
(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.tinfour.tinfour
  {:doc     "Tinfour utilities: https://github.com/gwlucastrig/Tinfour"
   :author  "palisades dot lakes at gmail dot com"
   :version "2026-04-20"}
  (:require [clojure.java.io :as io]
            [mop.jfx.jfx :as jfx])
  (:import
   [java.io IOException]
   [java.util Arrays List]
   [javafx.scene Group]
   [javafx.scene.shape StrokeType]
   [mop.java.jts GeometryCollectionIterator PolygonRingIterator]
   [org.locationtech.jts.geom
    Coordinate Geometry GeometryCollection LineString LinearRing Point Polygon]
   [org.tinfour.common
    LinearConstraint PolygonConstraint SimpleTriangle TriangleCount Vertex]
   [org.tinfour.standard IncrementalTin]))
;;---------------------------------------------------------------------
;; convenience functions converting JTS geometry
;; to Tinfour sites and constraints
;;---------------------------------------------------------------------
(defmulti ^Vertex vertex class)

(defmethod vertex Coordinate [^Coordinate p]
  (Vertex. (.getX p) (.getY p) (.getZ p)))

(defmethod vertex Point [^Point p]
  (Vertex. (.getX p) (.getY p) 0.0))
;;---------------------------------------------------------------------
;; TODO: dedupe coordinates/vertices?

(defn ^List sites [^Geometry g]
  "Convert all the points (<code>Coordinate</code>s) in <code>g</code>
  to <code>Vertex</code> to use as sites."
  (map vertex (Arrays/asList (.getCoordinates g))))

;;---------------------------------------------------------------------
(defmulti ^List constraints class)

(defmethod constraints LineString [^LineString g]
  [(LinearConstraint.
    (map vertex (Arrays/asList (.getCoordinates g))))])

(defmethod constraints LinearRing [^LinearRing g]
  ;; JTS repeats the starting vertex at the end
  ;; Tinfour doesn't
  [(PolygonConstraint.
    (map vertex (butlast (Arrays/asList (.getCoordinates g)))))])

(defmethod constraints Polygon [^Polygon g]
  (flatten
   (mapv constraints (iterator-seq (PolygonRingIterator/make g)))))

(defmethod constraints GeometryCollection [^GeometryCollection g]
  (let [i (GeometryCollectionIterator/make g)
        gs (iterator-seq i)]
    (flatten (mapv constraints gs))))
;;---------------------------------------------------------------------
;; Conforming Delaunay
;;---------------------------------------------------------------------
(defn ^IncrementalTin conformingDT [^List s
                                    ^List c
                                    ^double estimatedPointSpacing]
  (assert (not-empty s))

  (let [^IncrementalTin tin (IncrementalTin. estimatedPointSpacing)]
    ;; possible performance improvement:
    ;;(.sort (HilbertSort.) sites)
    (.add tin s nil)
    (when (not-empty c) (.addConstraints tin c true))
    tin))
;;---------------------------------------------------------------------
(defn ^Iterable cdt [^Geometry points
                     ^Geometry edges
                     ^double estimatedPointSpacing]
  (let [s (sites points)
        c (constraints edges)
        tin (conformingDT s c estimatedPointSpacing)]
    (.triangles tin)))
;;---------------------------------------------------------------------
#_(defn ^IncrementalTin checkCdt [^String name
                                  sites
                                  constraints
                                  estimatedPointSpacing
                                  expectedTriangles]
    (println)
    (println name ": " estimatedPointSpacing)
    (let [expectedTriangles (int expectedTriangles)
          tin (conformingDT sites constraints (double estimatedPointSpacing))
          ^TriangleCount stats (.countTriangles tin)
          nTriangles (.getCount stats)]
      #_(try
          (TinRenderingUtility/drawTin
           tin 1024 1024 (io/file "out/png/tinfour/" (str name ".png")))
          (catch (IOException e) (throw (RuntimeException. ^IOException e))))
      (if (== nTriangles expectedTriangles)
        (println "Success.")
        ;; else
        (println "expected != nTriangles: " expectedTriangles " != " nTriangles))
      (println "areas: [" (.getAreaMin stats) ", " (.getAreaMax stats) "]")
      (when (0.0 >= (.getAreaMin stats))
        (println "Singular!")
        (for [^SimpleTriangle triangle (seq (.triangles tin))]
          (when (<= 0.0 (.getArea triangle)) (println "Singular:" triangle))))
      tin))
;;---------------------------------------------------------------------
;; Tinfour to JavaFX
;;---------------------------------------------------------------------

(defn ^javafx.scene.shape.Polygon
  jfx-polygon [^SimpleTriangle triangle fill stroke]
  (let [^doubles coords (double-array 6)
        a (.getVertexA triangle)
        b (.getVertexB triangle)
        c (.getVertexC triangle)
        _ (aset coords 0 (.getX a))
        _ (aset coords 1 (.getY a))
        _ (aset coords 2 (.getX b))
        _ (aset coords 3 (.getY b))
        _ (aset coords 4 (.getX c))
        _ (aset coords 5 (.getY c))
        polygon (javafx.scene.shape.Polygon. coords)]
    (.setId polygon "tinfour")
    (when fill (.setFill polygon (jfx/color fill)))
    (.setStroke polygon (jfx/color stroke))
    (.setStrokeWidth polygon 1)
    (.setStrokeType polygon StrokeType/INSIDE)
    polygon))

(defn ^Group jfx-group [^Iterable triangles fill stroke]
  (let [group (Group.)
        children (.getChildren group)]
    (.setId group "tinfour")
    (let [fill (jfx/color fill)
          stroke (jfx/color stroke)]
      (dorun (map #(.add children (jfx-polygon % fill stroke))
                  (seq triangles)))
      ;; events handled by parents
      (.setFocusTraversable group false)
      group)))

;;---------------------------------------------------------------------

