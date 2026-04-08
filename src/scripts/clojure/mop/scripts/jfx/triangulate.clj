(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.scripts.jfx.triangulate
  {:doc     "Use JavaFX to display a conformal delaunay triangulation of
  natural earth boundaries."
   :author  "palisades dot lakes at gmail dot com"
   :version "2026-04-08"}

  (:require
   [mop.cmplx.complex :as cmplx]
   [mop.commons.time :as mct]
   [mop.geom.icosahedron :as icosahedron]
   [mop.geom.s2 :as s2]
   [mop.jfx.jfx :as jfx]
   [mop.io.shapefile :as miosh])
  (:import
   [java.util Collection]
   [javafx.geometry Insets]
   [javafx.scene Group Scene]
   [javafx.scene.layout BorderPane]
   [javafx.scene.paint Color Paint]
   [mop.cmplx.complex VertexPair]
   [mop.java.cmplx TwoSimplex ZeroSimplex]
   [mop.java.geom.mesh TriangleMesh]
   [mop.java.jfx JfxApplication WorldPane]
   [org.apache.commons.geometry.euclidean.twod Vector2D]
   [org.locationtech.jts.geom
    CoordinateXY Geometry GeometryCollection GeometryFactory LineString Point Polygon]
   [org.locationtech.jts.geom.util GeometryCombiner]
   [org.locationtech.jts.operation.valid IsValidOp]
   [org.locationtech.jts.triangulate
    ConformingDelaunayTriangulationBuilder]))
;;----------------------------------------------------------------
;; mvn -q install & cljfx src\scripts\clojure\mop\scripts\jfx\triangulate.clj
;;----------------------------------------------------------------
;; TODO: move to JTS namespace and defmethod
(defn ^CoordinateXY jts-coordinate [^Vector2D xy]
  (CoordinateXY. (.getX xy) (.getY xy)))
;;----------------------------------------------------------------
(defn ^Point jts-point [^GeometryFactory factory ^Vector2D v]
  (.createPoint factory (jts-coordinate v)))
;;----------------------------------------------------------------
(defn ^LineString jts-edge [^GeometryFactory factory
                            ^Vector2D p0
                            ^Vector2D p1]
  (let [c0 (jts-coordinate p0)
        c1 (jts-coordinate p1)
        ^"[Lorg.locationtech.jts.geom.CoordinateXY;"
        coordinates (into-array CoordinateXY [c0 c1])
        edge (.createLineString factory coordinates)
        is-valid (IsValidOp. edge)]
    (assert (.isValid is-valid) (str (.getValidationError is-valid)))
    edge))
;;----------------------------------------------------------------
(defn ^Polygon jts-triangle [^GeometryFactory factory
                             ^Vector2D p0
                             ^Vector2D p1
                             ^Vector2D p2]
  (let [c0 (jts-coordinate p0)
        c1 (jts-coordinate p1)
        c2 (jts-coordinate p2)
        ^"[Lorg.locationtech.jts.geom.CoordinateXY;"
        coordinates (into-array CoordinateXY [c0 c1 c2 c0])
        polygon (.createPolygon factory coordinates)
        is-valid (IsValidOp. polygon)]
    (assert (.isValid is-valid) (str (.getValidationError is-valid)))
    polygon))
;;----------------------------------------------------------------
(defn ^GeometryCollection icosahedron-polygons [^GeometryFactory factory]
  (let [^TriangleMesh mesh ((comp
                             #_cmplx/midpoint-subdivide-4
                             #_cmplx/midpoint-subdivide-4)
                            (icosahedron/u2-cut-icosahedron))
        faces (.faces (.cmplx mesh))
        _ (println "n mesh faces: " (.size faces))
        _ (println "n mesh vertices: " (.size (.vertices (.cmplx mesh))))
        embedding (.embedding mesh)
        triangles (into-array
                   Geometry
                   (mapv (fn [^TwoSimplex face]
                           (jts-triangle
                            factory
                            (s2/to-ll (embedding (.z0 face)))
                            (s2/to-ll (embedding (.z1 face)))
                            (s2/to-ll (embedding (.z2 face)))))
                         faces))
        geometries (.createGeometryCollection factory triangles)
        ;geometries (if (.isValid geometries)
        ;               geometries
        ;               (GeometryFixer/fix geometries))
        is-valid (IsValidOp. geometries)
        ]
    (assert (.isValid is-valid) (str (.getValidationError is-valid)))
    geometries))
;;----------------------------------------------------------------
(defn ^GeometryCollection icosahedron-edges [^GeometryFactory factory]
  (let [^TriangleMesh mesh ((comp
                             #_cmplx/midpoint-subdivide-4
                             #_cmplx/midpoint-subdivide-4)
                            (icosahedron/u2-cut-icosahedron))
        pairs (cmplx/vertex-pairs (.cmplx mesh))
        embedding (.embedding mesh)
        edges (into-array
               Geometry
               (mapv (fn [^VertexPair pair]
                       (jts-edge factory
                                 (s2/to-ll (embedding (.z0 pair)))
                                 (s2/to-ll (embedding (.z1 pair)))))
                     pairs))
        geometries (.createGeometryCollection factory edges)
        is-valid (IsValidOp. geometries)
        ]
    (println (alength edges))
    (assert (.isValid is-valid) (str (.getValidationError is-valid)))
    (println "n geometries: " (.getNumGeometries geometries))
    (println "n points: " (.getNumPoints geometries))
    geometries))
;;----------------------------------------------------------------
(defn ^GeometryCollection icosahedron-points [^GeometryFactory factory]
  (let [^TriangleMesh mesh ((comp
                             #_cmplx/midpoint-subdivide-4
                             #_cmplx/midpoint-subdivide-4)
                            (icosahedron/u2-cut-icosahedron))
        vertices (cmplx/vertices (.cmplx mesh))
        embedding (.embedding mesh)
        points (into-array
               Geometry
               (mapv (fn [^ZeroSimplex z]
                       (jts-point factory (s2/to-ll (embedding z))))
                     vertices))
        geometries (.createGeometryCollection factory points)
        is-valid (IsValidOp. geometries)]
    (println (alength points))
    (assert (.isValid is-valid) (str (.getValidationError is-valid)))
    (println "n geometries: " (.getNumGeometries geometries))
    (println "n points: " (.getNumPoints geometries))
    geometries))
;;----------------------------------------------------------------
(defn ^GeometryCollection land-polygons [^GeometryFactory factory]
  (let [geometries (miosh/read-jts-geometries
                    #_"data/natural-earth/10m_physical/ne_10m_land.shp"
                    #_"data/natural-earth/50m_physical/ne_50m_land.shp"
                    "data/natural-earth/ne_110m_land.shp"
                    factory)
        ;is-valid (IsValidOp. geometries)
        ;geometries (if (.isValid is-valid)
        ;               geometries
        ;               (GeometryFixer/fix geometries))
        ;is-valid (IsValidOp. geometries)
        ]
    ;(assert (.isSimple geometries))
    ;(assert (.isValid is-valid) (str (.getValidationError is-valid)))
    geometries))
;;----------------------------------------------------------------
(defn ^GeometryCollection triangulate [^GeometryCollection geometries
                                       ^GeometryFactory factory]
  (let [tolerance 1.0
        cdtb (ConformingDelaunayTriangulationBuilder.)
        _ (.setTolerance cdtb tolerance)
        _ (.setSites cdtb geometries)
        _ (.setConstraints cdtb geometries)
        triangles (mct/seconds "triangulate" (.getTriangles cdtb factory))]
    (assert (.isSimple triangles))
    (assert (.isValid triangles))
    triangles))
;;----------------------------------------------------------------
(defn ^Group to-jfx [^GeometryCollection polygons
                     ^Paint fill
                     ^Paint stroke
                     ^String id]
  (println id " geometries: " (.getNumGeometries polygons))
  (println id " points: " (.getNumGeometries polygons))
  (let [group (jfx/node polygons fill stroke)]
    (.setId group id)
    ;; events handled by parents
    (.setFocusTraversable group false)
    group))
;;----------------------------------------------------------------
(defn make-world []
  (let [factory (GeometryFactory.)
        land (land-polygons factory)
        ;combined land
        icosahedron (icosahedron-polygons factory)
        icosahedron-edges (icosahedron-edges factory)
        icosahedron-points (icosahedron-points factory)
        ;;combined icosahedron-points
        ;combined-group (to-jfx combined
        ;                       (Color/web "#22990044")
        ;                       (Color/web "#a66100FF")
        ;                       "combined")
        combined (GeometryCombiner/combine land icosahedron)
        triangulation (triangulate combined factory)
        ;triangulation combined
        triangulation-group (to-jfx triangulation
                                    (Color/web "#FFFFFF00")
                                    (Color/web "#000088FF")
                                    "cdt")
        ;; 'children' binding with type hint seems necessary to avoid
        ;; reflection warnings; inline type hint gives warning?
        ^Collection children [#_combined-group triangulation-group]
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
