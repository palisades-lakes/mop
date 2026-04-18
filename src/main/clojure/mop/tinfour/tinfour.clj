;;----------------------------------------------------------------
(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.tinfour.tinfour
  {:doc     "Tinfour utilities: https://github.com/gwlucastrig/Tinfour"
   :author  "palisades dot lakes at gmail dot com"
   :version "2026-04-18"}

  (:require [clojure.java.io :as io])
  (:import
   [java.io IOException]
   [java.util Arrays List]
   [mop.java.jts PolygonRingIterator]
   [org.locationtech.jts.geom
    Coordinate Geometry GeometryCollection GeometryCollectionIterator LineString Point Polygon]
   [org.tinfour.common LinearConstraint SimpleTriangle TriangleCount Vertex]
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
  (vec
   (LinearConstraint.
    (map vertex (Arrays/asList (.getCoordinates g))))))

(defmethod constraints Polygon [^Polygon g]
  (map constraints (iterator-seq (PolygonRingIterator/make g))))

(defmethod constraints GeometryCollection [^GeometryCollection g]
  (flatten
   (map constraints (iterator-seq (GeometryCollectionIterator. g)))))
;;---------------------------------------------------------------------
(defn ^IncrementalTin conformingDT [^List sites
                                    ^List constraints
                                    ^double estimatedPointSpacing]
  (assert (not-empty sites))
  (let [;;^HilbertSort hs (HilbertSort.)
        ^IncrementalTin tin (IncrementalTin. estimatedPointSpacing)]
    ;; possible performance improvement:
    ;;(.sort hs sites)
    (.add tin sites nil)
    (when (not-empty constraints) (.addConstraints tin constraints true))
    tin))
;;---------------------------------------------------------------------
(defn checkCdt [^String name
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
        (when (<= 0.0 (.getArea triangle)) (println "Singular:" triangle))))))

;;---------------------------------------------------------------------

