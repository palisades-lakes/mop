;;----------------------------------------------------------------
(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.jts.jts
  {:doc     "JTS utilities."
   :author  "palisades dot lakes at gmail dot com"
   :version "2026-04-08"}

  (:require
   [clojure.java.io :as io]
   [mop.cmplx.complex :as cmplx]
   [mop.commons.time :as mct]
   [mop.jfx.jfx :as jfx])
  (:import
   [java.util Arrays]
   [javafx.scene Group]
   [javafx.scene.paint Color]
   [mop.cmplx.complex VertexPair]
   [mop.java.cmplx TwoSimplex]
   [mop.java.geom.mesh TriangleMesh]
   [mop.java.jts MopConformingDelaunayTriangulationBuilder]
   [org.apache.commons.geometry.euclidean.twod Vector2D]
   [org.locationtech.jts.geom
    CoordinateXY Geometry GeometryCollection GeometryFactory LineString
    MultiLineString MultiPoint Point Polygon]
   [org.locationtech.jts.geom.util GeometryFixer]
   [org.locationtech.jts.io WKTWriter]
   [org.locationtech.jts.operation.valid IsSimpleOp IsValidOp]
   [org.locationtech.jts.triangulate ConstraintSplitPointFinder NonEncroachingSplitPointFinder]))
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
(defn ^CoordinateXY coordinate [^Vector2D xy]
  (CoordinateXY. (.getX xy) (.getY xy)))
;;----------------------------------------------------------------
(defn ^LineString edge [^GeometryFactory factory
                        ^CoordinateXY p0
                        ^CoordinateXY p1]
  (let [^"[Lorg.locationtech.jts.geom.CoordinateXY;"
        coordinates (into-array CoordinateXY (sort [p0 p1]))]
    (assert-valid (.createLineString factory coordinates))))
;;----------------------------------------------------------------
(defn ^Polygon triangle [^GeometryFactory factory
                         ^CoordinateXY p0
                         ^CoordinateXY p1
                         ^CoordinateXY p2]
  (let [^"[Lorg.locationtech.jts.geom.CoordinateXY;"
        coordinates (into-array CoordinateXY [p0 p1 p2 p0])]
    (assert-valid (.createPolygon factory coordinates))))
;;----------------------------------------------------------------
(defn ^MultiPoint centroids [^GeometryCollection g]
  (let [n (.getNumGeometries g)
        ^"[Lorg.locationtech.jts.geom.Point;" points (make-array Point n)]
    (dotimes [i n] (aset points i (.getCentroid (.getGeometryN g i))))
    (.createMultiPoint (.getFactory g) points)))
;;----------------------------------------------------------------
(defn ^GeometryCollection mesh-polygons [^TriangleMesh mesh
                                         ^GeometryFactory factory]
  (let [faces (.faces (.cmplx mesh))
        _ (println "n mesh faces: " (.size faces))
        _ (println "n mesh vertices: " (.size (.vertices (.cmplx mesh))))
        embedding (.embedding mesh)
        triangles (into-array
                   Geometry
                   (mapv (fn [^TwoSimplex face]
                           (triangle
                            factory
                            (embedding (.z0 face))
                            (embedding (.z1 face))
                            (embedding (.z2 face))))
                         faces))
        g (assert-valid (.createGeometryCollection factory triangles))]
    (.setUserData g "mesh-polygons")
    (println (debug-msg g))
    g))
;;----------------------------------------------------------------
(defn ^MultiLineString mesh-linestrings [^TriangleMesh mesh
                                         ^GeometryFactory factory]
  (let [pairs (cmplx/vertex-pairs (.cmplx mesh))
        embedding (.embedding mesh)
        ^"[Lorg.locationtech.jts.geom.LineString;"
        edges (into-array
               LineString
               (mapv (fn [^VertexPair pair]
                       (edge factory
                             (embedding (.z0 pair))
                             (embedding (.z1 pair))))
                     pairs))
        _ (Arrays/sort edges)
        g (assert-valid (.createMultiLineString factory edges))
        g (assert-valid (GeometryFixer/fix g))]
    (.setUserData g "mesh-edges")
    (println (debug-msg g))
    g))
;;----------------------------------------------------------------
(defn ^MultiPoint mesh-points [^TriangleMesh mesh
                               ^GeometryFactory factory]
  (let [vertices (cmplx/vertices (.cmplx mesh))
        embedding (.embedding mesh)
        ^"[Lorg.locationtech.jts.geom.Point;"
        points (into-array
                Point
                (mapv #(.createPoint factory ^CoordinateXY (embedding %))
                      vertices))
        _ (Arrays/sort points)
        ;;g (assert-valid (.createGeometryCollection factory points))
        g (assert-valid (.createMultiPoint factory points))
        g (assert-valid (GeometryFixer/fix g))]
    (.setUserData g "mesh-points")
    (println (debug-msg g))
    g))
;;----------------------------------------------------------------
(defn write-wkt [^Geometry g dest]
  (with-open [w (io/writer dest)]
    (.writeFormatted (WKTWriter.) g w)))
;;----------------------------------------------------------------
(defn ^Group jfx [^GeometryCollection g ^String fill ^String stroke]
  (let [group (jfx/node g (Color/web fill) (Color/web stroke))]
    (.setId group (.getUserData g))
    ;; events handled by parents
    (.setFocusTraversable group false)
    group))
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
     (.setConstraints cdtb constraints)
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
