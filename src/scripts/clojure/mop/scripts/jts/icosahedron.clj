(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.scripts.jts.icosahedron
  {:doc     "ConformaL delaunay triangulation of a cut, rojected icosahedron."
   :author  "palisades dot lakes at gmail dot com"
   :version "2026-04-07"}

  (:require
   [mop.cmplx.complex :as cmplx]
   [mop.commons.time :as mct]
   [mop.geom.icosahedron :as icosahedron]
   [mop.geom.mesh :as mesh]
   [mop.geom.s2 :as s2]
   [mop.jfx.jfx :as jfx])
  (:import
   [java.util Collection]
   [javafx.geometry Insets]
   [javafx.scene Group Scene]
   [javafx.scene.layout BorderPane]
   [javafx.scene.paint Color]
   [mop.cmplx.complex VertexPair]
   [mop.java.cmplx TwoSimplex]
   [mop.java.geom.mesh TriangleMesh]
   [mop.java.jfx JfxApplication WorldPane]
   [org.apache.commons.geometry.euclidean.twod Vector2D]
   [org.locationtech.jts.geom
    CoordinateXY Geometry GeometryCollection GeometryFactory LineString
    Point Polygon]
   [org.locationtech.jts.operation.valid IsSimpleOp IsValidOp]
   [org.locationtech.jts.triangulate
    ConformingDelaunayTriangulationBuilder]))
;;----------------------------------------------------------------
;; mvn -q install & cljfx src\scripts\clojure\mop\scripts\jts\icosahedron.clj
;;----------------------------------------------------------------
(defn debug-msg ^String [^Geometry g]
  (str (.getUserData g)
       "\nn points: " (.getNumPoints g)
       "\nn geometries: " (.getNumGeometries g)))
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
;; TODO: move to JTS namespace and defmethod
(defn ^CoordinateXY jts-coordinate [^Vector2D xy]
  (CoordinateXY. (.getX xy) (.getY xy)))
;;----------------------------------------------------------------
(defn ^LineString jts-edge [^GeometryFactory factory
                            ^CoordinateXY p0
                            ^CoordinateXY p1]
  (let [^"[Lorg.locationtech.jts.geom.CoordinateXY;"
        coordinates (into-array CoordinateXY [p0 p1])]
    (assert-valid (.createLineString factory coordinates))))
;;----------------------------------------------------------------
(defn ^Polygon jts-triangle [^GeometryFactory factory
                             ^CoordinateXY p0
                             ^CoordinateXY p1
                             ^CoordinateXY p2]
  (let [^"[Lorg.locationtech.jts.geom.CoordinateXY;"
        coordinates (into-array CoordinateXY [p0 p1 p2 p0])]
    (assert-valid (.createPolygon factory coordinates))))
;;----------------------------------------------------------------
(defn ^GeometryCollection centroids [^GeometryCollection g]
  (let [n (.getNumGeometries g)
        ^"[Lorg.locationtech.jts.geom.Point;" points (make-array Point n)]
    (dotimes [i n]
      (aset points i (.getCentroid (.getGeometryN g i))))
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
                           (jts-triangle
                            factory
                            (embedding (.z0 face))
                            (embedding (.z1 face))
                            (embedding (.z2 face))))
                         faces))
        g (assert-valid (.createGeometryCollection factory triangles))]
    (.setUserData g "icosahedron-polygons")
    (println (debug-msg g))
    g))
;;----------------------------------------------------------------
(defn ^GeometryCollection mesh-edges [^TriangleMesh mesh
                                      ^GeometryFactory factory]
  (let [pairs (cmplx/vertex-pairs (.cmplx mesh))
        embedding (.embedding mesh)
        edges (into-array
               Geometry
               (mapv (fn [^VertexPair pair]
                       (jts-edge factory
                                 (embedding (.z0 pair))
                                 (embedding (.z1 pair))))
                     pairs))
        g (assert-valid (.createGeometryCollection factory edges))]
    (.setUserData g "icosahedron-edges")
    (println (debug-msg g))
    g))
;;----------------------------------------------------------------
(defn ^GeometryCollection mesh-points [^TriangleMesh mesh
                                       ^GeometryFactory factory]
  (let [vertices (cmplx/vertices (.cmplx mesh))
        embedding (.embedding mesh)
        points (into-array
                Geometry
                (mapv #(.createPoint factory ^CoordinateXY (embedding %))
                      vertices))
        g (assert-valid (.createGeometryCollection factory points))]
    (.setUserData g "icosahedron-points")
    (println (debug-msg g))
    g))
;;----------------------------------------------------------------
(defn ^Group to-jfx [^GeometryCollection g ^String fill ^String stroke]
  (let [group (jfx/node g (Color/web fill) (Color/web stroke))]
    (.setId group (.getUserData g))
    ;; events handled by parents
    (.setFocusTraversable group false)
    group))
;;----------------------------------------------------------------
(defn make-world []
  (let [factory (GeometryFactory.)
        ^TriangleMesh u2-mesh ((comp
                                #_cmplx/midpoint-subdivide-4
                                cmplx/midpoint-subdivide-4)
                               (icosahedron/u2-cut-icosahedron))
        embedding (into {}
                        (map
                         (fn [[k v]] [k (jts-coordinate (s2/to-ll v))])
                         (.embedding u2-mesh)))
        mesh (mesh/triangle-mesh (.cmplx u2-mesh) embedding)
        polygons (mesh-polygons mesh factory)
        polygons-group (to-jfx polygons "#22990044" "#FF0000FF")
        edges (mesh-edges mesh factory)
        points (mesh-points mesh factory)
        ;;points (centroids polygons)
        cdtb (ConformingDelaunayTriangulationBuilder.)
        _ (.setTolerance cdtb 1.0)
        _ (.setSites cdtb points)
        _ (.setConstraints cdtb edges)
        triangles (mct/seconds "triangulate" (.getTriangles cdtb factory))
        _ (.setUserData triangles "triangulation")
        _ (assert-valid triangles)
        triangulation-group (to-jfx triangles "#FFFFFF00" "#0000FF88")
        ;; 'children' binding with type hint seems necessary to avoid
        ;; reflection warnings; inline type hint gives warning?
        ^Collection children [polygons-group triangulation-group]
        world (Group. children)]
    (.setId world "world")
    ;; parent Pane handles events
    ;; TODO: is this necessary or useful?
    (.setFocusTraversable world false)
    (.setMouseTransparent world true)
    world))
;;----------------------------------------------------------------
(defn make-scene ^Scene [^double w ^double h]
  (let [pane (WorldPane/make (make-world))
        _ (BorderPane/setMargin pane (Insets. 32))
        wrapper (BorderPane. pane)
        scene (Scene. wrapper w h)]
    (.setUserData scene "cut icosahedronS2 scene")
    scene))
;;----------------------------------------------------------------
;;(println (System/getProperty "glass.win.uiScale"))
(System/setProperty "glass.win.uiScale" "1")
;;(println (System/getProperty "glass.win.uiScale"))
;;(System/setProperty "javafx.pulseLogger" "true")
;;(System/setProperty "prism.verbose" "true")
;;(System/setProperty "prism.order" "d3d")
(JfxApplication/setSceneBuilder make-scene)
(JfxApplication/launch JfxApplication (make-array String 0))
