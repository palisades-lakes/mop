(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.geom.mesh
  {:doc     "Embedded cell complexes."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-11-12"}
  (:require [mop.cmplx.complex :as cmplx]
            [mop.commons.debug :as debug]
            [mop.geom.rn :as rn]
            [mop.geom.s2 :as s2])
  (:import [clojure.lang IFn]
           [java.util List]
           [mop.cmplx.complex Cell CellComplex OneSimplex Quad QuadComplex SimplicialComplex2D
                              TwoSimplex ZeroSimplex]
           [mop.java.geom Point2U]
           [org.apache.commons.geometry.euclidean.threed Vector3D Vector3D$Sum]
           [org.apache.commons.geometry.euclidean.threed.rotation QuaternionRotation]
           [org.apache.commons.geometry.euclidean.twod Vector2D Vector2D$Sum]
           [org.apache.commons.geometry.spherical.twod GreatCircles Point2S]
           [org.apache.commons.numbers.core Precision]))

;;---------------------------------------------------------------

(definterface Mesh
  (^mop.cmplx.complex.CellComplex cmplx [])
  (^clojure.lang.IFn embedding []))

;;---------------------------------------------------------------
;; TODO: move these to Java to get better control over construction?
;; TODO: require sorted map for embedding consistency?
;;---------------------------------------------------------------
;; Embedded quadrilateral cell complex.

(deftype TriangleMesh
  [^SimplicialComplex2D _cmplx
   ^IFn _embedding]
  :load-ns true

  Mesh
  (cmplx [this] (._cmplx this))
  (embedding [this] (._embedding this)))

;;---------------------------------------------------------------

(defn triangle-mesh ^TriangleMesh [^SimplicialComplex2D cmplx
                                   ^IFn embedding]
  (doall
   (map #(assert (not (nil? (embedding %)))
                 (println %))
        (.vertices cmplx)))
  (TriangleMesh. cmplx embedding))

;;---------------------------------------------------------------
;; TODO: move these to Java to get better control over construction?
;; TODO: require sorted map for embedding consistency?
;;---------------------------------------------------------------
;; Embedded quadrilateral cell complex.

(deftype QuadMesh
  [^QuadComplex _cmplx
   ^IFn _embedding]
  :load-ns true

  Mesh
  (cmplx [this] (._cmplx this))
  (embedding [this] (._embedding this)))

;;---------------------------------------------------------------

(defmethod debug/simple-string QuadMesh [^QuadMesh this]
  (str "QuadMesh[" \newline " "
       (debug/simple-string (.cmplx this))
       \newline
       (debug/simple-string (.embedding this))
       "]"))

;;---------------------------------------------------------------

(defn quad-mesh ^QuadMesh [^QuadComplex cmplx
                           ^IFn embedding]
  (doall
   (map #(assert (not (nil? (embedding %))))
        (.vertices cmplx)))
  (QuadMesh. cmplx embedding))

;;---------------------------------------------------------------

(defmulti mesh
          "Create a triangle or quad mesh, depending on the complex."
          (fn [cmplx _embedding] (class cmplx)))

(defmethod mesh SimplicialComplex2D [^SimplicialComplex2D complex ^IFn embedding]
  (triangle-mesh complex embedding))

(defmethod mesh QuadComplex [^QuadComplex complex ^IFn embedding]
  (quad-mesh complex embedding))

;;---------------------------------------------------------------
;; just map the transform over the vals of the embedding.
;; TODO: require 1st arg of <code>transform</code> to be a function.
;; and then <code>transform</code> could just be <code>

(defmethod rn/transform [Object TriangleMesh] [^Object f ^TriangleMesh x]
  (triangle-mesh
   (.cmplx x)
   (update-vals (.embedding x) #(rn/transform f %))))

(defmethod rn/transform [Object QuadMesh] [^Object f ^QuadMesh x]
  (quad-mesh
   (.cmplx x)
   (update-vals (.embedding x) #(rn/transform f %))))

;;---------------------------------------------------------------
;; TODO: ensure that embedded cube has the desired orientation,
;; with right-handed normals pointing out.

(defn standard-quad-cube []
  "Return a quad mesh that is the boundary of [-1,1]^3.
  (As opposed to a unit cube which would be [0,1]^3.)"
  (let [cmplx (cmplx/quad-cube)
        ;; TODO: how do we know these are in the right order?
        ;; This isn't feasible for larger complexes!
        ;; Should be some way to walk the complex and get them in the right order.
        [z0 z1 z2 z3 z4 z5 z6 z7] (.vertices cmplx)]
    (mesh
     cmplx
     {z0 (rn/vector -1 -1 -1)
      z1 (rn/vector 1 -1 -1)
      z2 (rn/vector 1 1 -1)
      z3 (rn/vector -1 1 -1)
      z4 (rn/vector -1 -1 1)
      z5 (rn/vector 1 -1 1)
      z6 (rn/vector 1 1 1)
      z7 (rn/vector -1 1 1)})))

;;---------------------------------------------------------------
;; TODO: ensure that embedded cube has the desired orientation,
;; with right-handed normals pointing out.

(defn standard-quad-sphere []
  "Return a quad mesh that evenly subdivides the unit two-sphere S_2,
  for subdivision on the sphere and later transform to R^3,
  via a sphere with a give R^3 center and radius."
  (let [cmplx (cmplx/quad-cube)
        ;; TODO: how do we know these are in the right order?
        ;; This isn't feasible for larger complexes!
        ;; Should be some way to walk the complex and get them in the right order.
        #_qr #_(QuaternionRotation/createVectorRotation
                (rn/vector -1 -1 -1)
                (rn/vector 0 0 -1))]
    (quad-mesh
     cmplx
     (doall
      (into
       {}
       (map (fn [v p]
              ;;[v (s2/point (rn/transform qr p))])
              [v (s2/point p)])
            (.vertices cmplx)
            [(rn/vector -1 -1 -1)
             (rn/vector  1 -1 -1)
             (rn/vector  1  1 -1)
             (rn/vector -1  1 -1)
             (rn/vector -1 -1  1)
             (rn/vector  1 -1  1)
             (rn/vector  1  1  1)
             (rn/vector -1  1  1)]))))))

;;---------------------------------------------------------------
;; TODO: defmulti?

(defn- points [embedding x]
  "Use the <code<embedding</code> to convert an abstract simplicial
  object <code>x</code> to a list of points in some space,
  most likely R^3 or S^2."
  ;; just cover known cases for now
  (cond
    (instance? ZeroSimplex x)
    [(embedding x)]

    (instance? OneSimplex x)
    [(embedding (.z0 ^OneSimplex x))
     (embedding (.z1 ^OneSimplex x))]

    (instance? TwoSimplex x)
    [(embedding (.z0 ^TwoSimplex x))
     (embedding (.z1 ^TwoSimplex x))
     (embedding (.z2 ^TwoSimplex x))]

    (instance? Quad x)
    [(embedding (.z0 ^Quad x))
     (embedding (.z1 ^Quad x))
     (embedding (.z2 ^Quad x))
     (embedding (.z3 ^Quad x))]

    (instance? List x)
    (mapv embedding x)

    (instance? SimplicialComplex2D x)
    (points embedding (.vertices ^SimplicialComplex2D x))

    (instance? QuadComplex x)
    (points embedding (.vertices ^QuadComplex x))))

;;---------------------------------------------------------------

(defn- ^Point2U u2-midpoint
  ([^Point2U uv0 ^Point2U uv1]
   ;; midpoint of geodesic arc from uv0 to uv1
   ;; rotate about polar axis to avoid singularity
   (let [u0 (.getU uv0)
         u1 (.getU uv1)
         u (Math/min u0 u1)
         p0 (Point2S/of (- u0 u) (.getV uv0))
         p1 (Point2S/of (- u1 u) (.getV uv1))
         mid (.getMidPoint
    (GreatCircles/arcFromPoints
     p0 p1 (Precision/doubleEquivalenceOfEpsilon 1e-12)))]
     (Point2U/of (+ u (.getAzimuth mid)) (.getPolar mid))))

  ([^Point2U p0 ^Point2U p1 ^Point2U p2 ^Point2U p3]
   "As far as I know, this isn't exactly any common centroid definition."
   (let [m01 (u2-midpoint p0 p1)
         m23 (u2-midpoint p2 p3)
         m12 (u2-midpoint p1 p2)
         m30 (u2-midpoint p3 p0)
         ^Point2U m0123 (u2-midpoint m01 m23)
         ^Point2U m1230 (u2-midpoint m12 m30)]
     ;; should these be equal, ie, a singular arc?
     (if (<= (.distance m0123 m1230) 1.0e-6)
       m0123
       (u2-midpoint m0123 m1230))))
  )

;;---------------------------------------------------------------

(defn- ^Point2S spherical-midpoint
  ([^Point2S p0 ^Point2S p1]
   ;; midpoint of geodesic arc from p0 to p1
   (assert (> (.distance p0 p1) 1.0e-7)
           (str "distance=" (.distance p0 p1)))
   (.getMidPoint
    (GreatCircles/arcFromPoints
     p0 p1 (Precision/doubleEquivalenceOfEpsilon 1e-12))))

  ([^Point2S p0 ^Point2S p1 ^Point2S p2 ^Point2S p3]
   "As far as I know, this isn't exactly any common centroid definition."
   (let [m01 (spherical-midpoint p0 p1)
         m23 (spherical-midpoint p2 p3)
         m12 (spherical-midpoint p1 p2)
         m30 (spherical-midpoint p3 p0)
         ^Point2S m0123 (spherical-midpoint m01 m23)
         ^Point2S m1230 (spherical-midpoint m12 m30)]
     ;; should these be equal, ie, a singular arc?
     (if (<= (.distance m0123 m1230) 1.0e-6)
       m0123
       (spherical-midpoint m0123 m1230))))
  )

;;---------------------------------------------------------------

(defmulti midpoint
          "Return a 'center' of some kind for the collection of points.
          Mean for euclidean space, not so obvious for spherical space.
          Assume all points are the same type, throw an exceptions otherwise."
          (fn [p0 & _points] (class p0)))

(defmethod midpoint Point2U [^Point2U p0 & points]
  (case (count points)
    0 p0
    1 (u2-midpoint p0 (first points))
    3 (let [[^Point2S p1 ^Point2S p2 ^Point2S p3] points]
        (u2-midpoint p0 p1 p2 p3))
    ;; else
    (throw
     (UnsupportedOperationException.
      (str "Can't compute u2-midpoint of " (count points) " points.")))))

(defmethod midpoint Point2S [^Point2S p0 & points]
  (case (count points)
    0 p0
    1 (spherical-midpoint p0 (first points))
    3 (let [[^Point2S p1 ^Point2S p2 ^Point2S p3] points]
        (spherical-midpoint p0 p1 p2 p3))
    ;; else
    (throw
     (UnsupportedOperationException.
      (str "Can't compute spherical-midpoint of "
           (count points) " points.")))))

(defmethod midpoint Vector2D [^Vector2D p0 & points]
  (let [sum (Vector2D$Sum/of p0)]
    (dorun (map #(.add sum %) points))
    (.multiply (.get sum) (/ 1.0 (inc (count points))))))

(defmethod midpoint Vector3D [^Vector3D p0 & points]
  (let [sum (Vector3D$Sum/of p0)]
    (dorun (map #(.add sum %) points))
    (.multiply (.get sum) (/ 1.0 (inc (count points))))))

;;---------------------------------------------------------------
;; TODO: Incorporate alternate subdivision rules,
;; especially with regards to inherited embedding.
;; Initial version puts new vertex at some centroid of parent
;; edge/face --- reasonably straightforward in euclidean case,
;; not so much in spherical case.
;; TODO: should inherited vertices have themselves (or a parent vertex)
;; as parent,
;;

(defmethod cmplx/subdivide-4 TriangleMesh [^TriangleMesh m]
  (let [{^CellComplex child :child parent :parent} (cmplx/subdivide-4 (.cmplx m))
        embedding (.embedding m)]
    (mesh
     child
     (into
      {}
      (map
       (fn [v] [v (apply midpoint (points embedding (parent v)))])
       (.vertices child))))))

(defmethod cmplx/subdivide-4 QuadMesh [^QuadMesh qm]
  (let [{^CellComplex child :child
         parent :parent} (cmplx/subdivide-4 (.cmplx qm))
        embedding (.embedding qm) ]
    (mesh
     child
     (into
      {}
      (map
       (fn [v] [v (apply midpoint (points embedding (parent v)))])
       (.vertices child))))))

;;---------------------------------------------------------------

#_(defn- ^Double signed-area [^TwoSimplex face ^IFn txt]
    (let [^Vector2D p0 (txt (.z0 face))
          ^Vector2D p1 (txt (.z1 face))
          ^Vector2D p2 (txt (.z2 face))
          v0 (.subtract p1 p0)
          v1 (.subtract p1 p2)
          ]
      (.signedArea v0 v1)))

;;---------------------------------------------------------------

(defn coordinates-and-elements  [{:keys [^CellComplex cmplx
                                         _s2-embedding
                                         xyz-embedding
                                         rgba-embedding
                                         dual-embedding
                                         txt-embedding]}]
  "Return a float array and an int array suitable for passing to GLSL.
  Don't rely on any ordering of cells and vertices."
  (let [faces (.faces cmplx)
        zeros (sort (.vertices cmplx))
        zindex (into {} (map (fn [z i] [z i])
                             zeros (range (count zeros))))
        indices (flatten (map (fn [^Cell face]
                                (mapv #(zindex %) (.vertices face)))
                              faces))
        coordinates (flatten (map #(concat (rn/coordinates (xyz-embedding %))
                                           (rn/coordinates (rgba-embedding %))
                                           (rn/coordinates (dual-embedding %))
                                           (rn/coordinates (txt-embedding %)))
                                  zeros))]
    [coordinates indices]))
