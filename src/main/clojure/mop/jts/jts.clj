;;----------------------------------------------------------------
(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.jts.jts
  {:doc     "JTS utilities: https://github.com/locationtech/jts"
   :author  "palisades dot lakes at gmail dot com"
   :version "2026-04-18"}

  (:require
   [clojure.java.io :as io]
   [mop.cmplx.complex :as cmplx]
   [mop.commons.time :as mct]
   [mop.jfx.jfx :as jfx])
  (:import
   [clojure.lang IFn]
   [java.util Arrays]
   [javafx.scene Group]
   [javafx.scene.paint Color]
   [mop.cmplx.complex VertexPair]
   [mop.java.cmplx TwoSimplex]
   [mop.java.geom.mesh TriangleMesh]
   [mop.java.jts MopConformingDelaunayTriangulationBuilder]
   [org.apache.commons.geometry.euclidean.twod Vector2D]
   [org.locationtech.jts.geom
    Coordinate CoordinateXY Geometry GeometryCollection GeometryCollectionIterator GeometryFactory
    LineString MultiPoint MultiPolygon Point Polygon Triangle]
   [org.locationtech.jts.geom.util
    GeometryFixer]
   [org.locationtech.jts.io
    WKTReader WKTWriter]
   [org.locationtech.jts.operation.valid
    IsSimpleOp IsValidOp]
   [org.locationtech.jts.triangulate
    ConstraintSplitPointFinder NonEncroachingSplitPointFinder]))
;;----------------------------------------------------------------
;; JTS utility functions
;;----------------------------------------------------------------
(defn debug-msg ^String [^Geometry g]
  (str (.getUserData g)
       "\nn geometries: " (.getNumGeometries g)
       "\nn points: " (.getNumPoints g)))
;;----------------------------------------------------------------
(defn assert-valid ^Geometry [^Geometry g]
  (let [op (IsValidOp. g)]
    (assert (.isValid op)
            (str (.getValidationError op) "\n" (.getUserData g))))
  g)
#_(defn assert-simple ^Geometry [^Geometry g]
    (let [op (IsSimpleOp. g)]
      (assert (.isSimple op)
              (str (.getNonSimpleLocations op) "\n" (.getUserData g))))
    g)
;;----------------------------------------------------------------
;; https://stackoverflow.com/questions/10289752/aspect-ratio-of-a-triangle-of-a-meshed-surface
;; TODO: is this the most accurate formula?

(defn assert-triangle [^Polygon polygon]
  (assert (zero? (.getNumInteriorRing polygon)))
  ;; TODO: check zeroth and last coordinates are the same?
  (assert (= 4 (.getNumPoints polygon))))

(defn ^double aspect-ratio

  ([^Coordinate p0
    ^Coordinate p1
    ^Coordinate p2]
   (let [a (.distance p0 p1)
         b (.distance p1 p2)
         c (.distance p2 p0)
         ;; https://mathworld.wolfram.com/Inradius.html
         inradius (* 0.5 (Math/sqrt (/ (* (- (+ b c) a)
                                          (- (+ c a) b)
                                          (- (+ a b) c))
                                       (+ a b c))))
         circumradius (Triangle/circumradius p0 p1 p2)]
     (/ circumradius (* 2 inradius))))

  ([^Polygon triangle]
   (assert-triangle triangle)
   (let [^Coordinate/1
         coords (.getCoordinates triangle)]
     (aspect-ratio (aget coords 0) (aget coords 1) (aget coords 2)))))

;;----------------------------------------------------------------
(defn print-aspect-ratios [^MultiPolygon triangles]
  (let [n (.getNumGeometries triangles)]
    (dotimes [i n]
      (let [^Polygon triangle (.getGeometryN triangles i)
            ^Coordinate/1
            coords (.getCoordinates triangle)
            p0 (aget coords 0)
            p1 (aget coords 1)
            p2 (aget coords 2)]
        (println (.getUserData triangle) " : "
                 (aspect-ratio p0 p1 p2) " : "
                 (Triangle/signedArea p0 p1 p2))

        (println (Arrays/toString (.getCoordinates triangle)))))))
;;----------------------------------------------------------------
(defn geometry-iterator [^GeometryCollection gc]
  (GeometryCollectionIterator. gc))
;;----------------------------------------------------------------
(defn ^LineString edge [^GeometryFactory factory
                        ^CoordinateXY p0
                        ^CoordinateXY p1]
  (let [^CoordinateXY/1
        coordinates (into-array CoordinateXY (sort [p0 p1]))]
    (assert-valid (.createLineString factory coordinates))))
;;----------------------------------------------------------------
(defn ^Polygon triangle

  ([^GeometryFactory factory
    ^CoordinateXY p0
    ^CoordinateXY p1
    ^CoordinateXY p2]
   (let [^CoordinateXY/1
         coordinates (into-array CoordinateXY [p0 p1 p2 p0])]
     (assert-valid (.createPolygon factory coordinates))))

  ([^GeometryFactory factory
    ^TwoSimplex face
    ^IFn embedding]
   (let [^Polygon t (triangle
                     factory
                     (embedding (.z0 face))
                     (embedding (.z1 face))
                     (embedding (.z2 face)))]
     (.setUserData t (.toString face))
     t)))
;;----------------------------------------------------------------
(defn ^MultiPoint centroids [^GeometryCollection g]
  (let [n (.getNumGeometries g)
        ^Point/1 points (make-array Point n)]
    (dotimes [i n] (aset points i (.getCentroid (.getGeometryN g i))))
    (.createMultiPoint (.getFactory g) points)))
;;----------------------------------------------------------------
(defn ^Geometry parse-wkt-string [^GeometryFactory f ^String src]
  (.read (WKTReader. f) src))
;;----------------------------------------------------------------
(defn ^Geometry read-wkt [^GeometryFactory f dest]
  (with-open [r (io/reader dest)]
    (.read (WKTReader. f) r)))
;;----------------------------------------------------------------
(defn write-wkt [^Geometry g dest]
  (let [f (io/file dest)]
    (io/make-parents f)
    (with-open [w (io/writer dest)]
      (.writeFormatted (WKTWriter.) g w))))
;;----------------------------------------------------------------
;; allow custom split point finder
;;----------------------------------------------------------------
(defn ^Geometry cdt

  (^Geometry [^Geometry sites
              ^Geometry constraints
              ^double tolerance
              ^ConstraintSplitPointFinder cspf]
   (let [cdtb (MopConformingDelaunayTriangulationBuilder. cspf)]
     (.setTolerance cdtb tolerance)
     (.setSites cdtb sites)
     (when constraints (.setConstraints cdtb constraints))
     (mct/seconds "triangulate"
                  (.getTriangles cdtb (.getFactory sites)))))

  (^Geometry [^Geometry sites
              ^Geometry constraints
              ^double tolerance]
   (cdt sites constraints tolerance (NonEncroachingSplitPointFinder.)))

  (^Geometry [^Geometry sites
              ^Geometry constraints]
   (cdt sites constraints 0.0 (NonEncroachingSplitPointFinder.))))
;;----------------------------------------------------------------
;; commons geometry to JTS
;;----------------------------------------------------------------
(defn ^CoordinateXY coordinate [^Vector2D xy]
  (CoordinateXY. (.getX xy) (.getY xy)))
;;----------------------------------------------------------------
;; mop mesh to JTS
;;----------------------------------------------------------------
(defn ^GeometryCollection mesh-polygons [^TriangleMesh mesh
                                         ^GeometryFactory factory]
  (let [faces (.faces (.cmplx mesh))
        _ (println "n mesh faces: " (.size faces))
        _ (println "n mesh vertices: " (.size (.vertices (.cmplx mesh))))
        embedding (.embedding mesh)
        triangles (into-array
                   Polygon (mapv #(triangle factory % embedding) faces))
        g (assert-valid (.createGeometryCollection factory triangles))]
    (.setUserData g "mesh-polygons")
    (println (debug-msg g))
    g))
;;----------------------------------------------------------------
(defn ^GeometryCollection mesh-linestrings [^TriangleMesh mesh
                                            ^GeometryFactory factory]
  (let [pairs (cmplx/vertex-pairs (.cmplx mesh))
        embedding (.embedding mesh)
        ^LineString/1
        edges (into-array
               LineString
               (mapv (fn [^VertexPair pair]
                       (edge factory
                             (embedding (.z0 pair))
                             (embedding (.z1 pair))))
                     pairs))
        _ (Arrays/sort edges)
        g (assert-valid (.createGeometryCollection factory edges))
        g (assert-valid (GeometryFixer/fix g))]
    (.setUserData g "mesh-edges")
    (println (debug-msg g))
    g))
;;----------------------------------------------------------------
(defn ^GeometryCollection mesh-points [^TriangleMesh mesh
                                       ^GeometryFactory factory]
  (let [vertices (cmplx/vertices (.cmplx mesh))
        embedding (.embedding mesh)
        ^Point/1
        points (into-array
                Point
                (mapv #(.createPoint factory ^CoordinateXY (embedding %))
                      vertices))
        _ (Arrays/sort points)
        ;;g (assert-valid (.createGeometryCollection factory points))
        g (assert-valid (.createGeometryCollection factory points))
        g (assert-valid (GeometryFixer/fix g))]
    (.setUserData g "mesh-points")
    (println (debug-msg g))
    g))
;;----------------------------------------------------------------
;; JTS to JavaFX
;;----------------------------------------------------------------
(defn ^Group jfx [^GeometryCollection g ^String fill ^String stroke]
  (let [group (jfx/node g (Color/web fill) (Color/web stroke))]
    (.setId group (.getUserData g))
    ;; events handled by parents
    (.setFocusTraversable group false)
    group))
;;----------------------------------------------------------------
