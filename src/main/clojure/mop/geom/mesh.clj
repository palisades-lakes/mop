(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.geom.mesh
  {:doc     "Embedded cell complexes."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-11-09"}
  (:require [mop.cmplx.complex :as cmplx]
            [mop.commons.debug :as debug]
            [mop.geom.rn :as rn]
            [mop.geom.s2 :as s2])
  (:import [clojure.lang IFn]
           [java.awt.geom Point2D]
           [java.util List]
           [mop.cmplx.complex Cell CellComplex OneSimplex Quad QuadComplex SimplicialComplex2D
                              TwoSimplex ZeroSimplex]
           [org.apache.commons.geometry.euclidean.threed Vector3D Vector3D$Sum]
           [org.apache.commons.geometry.euclidean.threed.rotation QuaternionRotation]
           [org.apache.commons.geometry.euclidean.twod Vector2D]
           [org.apache.commons.geometry.spherical.twod GreatArcPath GreatCircles Point2S]
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
;; Create abstract complex and r3 embedding together
;; to avoid orientation problems, etc.

(let [r (/ (+ 1.0 (Math/sqrt 5.0)) 2.0)
      -r (- r)]
  (defn ^TriangleMesh regular-icosahedron []
    (let [a (cmplx/simplex) b (cmplx/simplex) c (cmplx/simplex) d (cmplx/simplex)
          e (cmplx/simplex) f (cmplx/simplex) g (cmplx/simplex) h (cmplx/simplex)
          i (cmplx/simplex) j (cmplx/simplex) k (cmplx/simplex) l (cmplx/simplex)
          cmplx (cmplx/simplicial-complex-2d
                 (map #(apply cmplx/simplex %)
                      [;; texture interpolation issues
                       [a d b] [b d g] [b g h] [g l h]
                       [a e d]   [h l i] [k i l]
                       ;; interpolation ok
                       [a b c] [a c f] [a f e] [b h c] [c i f] [c h i]
                       [d e j] [d j g] [e f k] [e k j] [f i k] [g j l] [k l j]]))
          ;; put a at top
          qr (QuaternionRotation/createVectorRotation (rn/vector -1  r  0) (rn/vector 0 0 1))
          embedding {a (rn/vector -1  r  0) b (rn/vector  1  r  0)
                     c (rn/vector  0  1 -r) d (rn/vector  0  1  r)
                     e (rn/vector -r  0  1) f (rn/vector -r  0 -1)
                     g (rn/vector  r  0  1) h (rn/vector  r  0 -1)
                     i (rn/vector  0 -1 -r) j (rn/vector  0 -1  r)
                     k (rn/vector -1 -r  0) l (rn/vector  1 -r  0)}]
      (rn/transform qr (TriangleMesh. cmplx embedding)))))

(defn ^TriangleMesh spherical-icosahedron []
  (let [regular (regular-icosahedron)
        s2 (update-vals (.embedding regular) #(s2/point %))]
    (TriangleMesh. (.cmplx regular) s2)))

;; Cut icosahedron to simplify texture mapping and other
;; 2d projections. Return R3 embedding.
;; TODO: automate the cut. Key question: what to do when it's not spherical?
;; TODO: check if this is a regular icosahedron

(let [TWO_PI (* 2.0 Math/PI)
      delta (/ TWO_PI 5.0)
      rho (/ Math/PI 3.0)]
  (defn ^TriangleMesh cut-icosahedron-s2 []
    (let [a (cmplx/simplex) b (cmplx/simplex) c (cmplx/simplex) d (cmplx/simplex) e (cmplx/simplex)
          f (cmplx/simplex) g (cmplx/simplex) h (cmplx/simplex) i (cmplx/simplex) j (cmplx/simplex)
          k (cmplx/simplex) l (cmplx/simplex) m (cmplx/simplex) n (cmplx/simplex) o (cmplx/simplex)
          p (cmplx/simplex) q (cmplx/simplex) r (cmplx/simplex) s (cmplx/simplex) t (cmplx/simplex)
          u (cmplx/simplex) v (cmplx/simplex)
          cmplx (cmplx/simplicial-complex-2d
                 (map #(apply cmplx/simplex %)
                      [[a f g] [b g h] [c h i] [d i j] [e j k]
                       [f l g] [g m h] [h n i] [i o j] [j p k]
                       [g l m] [h m n] [i n o] [j o p] [k p q]
                       [l r m] [m s n] [n t o] [o u p] [p v q]]))
          s2-embedding {a (Point2S/of (* 0.0 delta) 0)
                        b (Point2S/of (* 1.0 delta) 0)
                        c (Point2S/of (* 2.0 delta) 0)
                        d (Point2S/of (* 3.0 delta) 0)
                        e (Point2S/of (* 4.0 delta) 0)

                        f (Point2S/of (* -0.5 delta) rho)
                        g (Point2S/of (* 0.5 delta) rho)
                        h (Point2S/of (* 1.5 delta) rho)
                        i (Point2S/of (* 2.5 delta) rho)
                        j (Point2S/of (* 3.5 delta) rho)
                        k (Point2S/of (* 4.5 delta) rho)

                        l (Point2S/of (* 0.0 delta) (* 2.0 rho))
                        m (Point2S/of (* 1.0 delta) (* 2.0 rho))
                        n (Point2S/of (* 2.0 delta) (* 2.0 rho))
                        o (Point2S/of (* 3.0 delta) (* 2.0 rho))
                        p (Point2S/of (* 4.0 delta) (* 2.0 rho))
                        q (Point2S/of (* 5.0 delta) (* 2.0 rho))

                        r (Point2S/of (* 0.5 delta) Math/PI)
                        s (Point2S/of (* 1.5 delta) Math/PI)
                        t (Point2S/of (* 2.5 delta) Math/PI)
                        u (Point2S/of (* 3.5 delta) Math/PI)
                        v (Point2S/of (* 4.5 delta) Math/PI)}
          r3-embedding (update-vals s2-embedding #(.getVector ^Point2S %))
          txt-embedding (let [dx (/ 1.0 9.0)
                              dy (/ 1.0 3.0)]
                          {a (Vector2D/of (* 0 dx) 1)
                           b (Vector2D/of (* 2 dx) 1)
                           c (Vector2D/of (* 4 dx) 1)
                           d (Vector2D/of (* 6 dx) 1)
                           e (Vector2D/of (* 8 dx) 1)

                           f (Vector2D/of (* -1 dx) (* 2 dy))
                           g (Vector2D/of (* 1 dx) (* 2 dy))
                           h (Vector2D/of (* 3 dx) (* 2 dy))
                           i (Vector2D/of (* 5 dx) (* 2 dy))
                           j (Vector2D/of (* 7 dx) (* 2 dy))
                           k (Vector2D/of (* 9 dx) (* 2 dy))

                           l (Vector2D/of (* 0 dx) dy)
                           m (Vector2D/of (* 2 dx) dy)
                           n (Vector2D/of (* 4 dx) dy)
                           o (Vector2D/of (* 6 dx) dy)
                           p (Vector2D/of (* 8 dx) dy)
                           q (Vector2D/of (* 10 dx) dy)

                           r (Vector2D/of (* 1 dx) 0)
                           s (Vector2D/of (* 3 dx) 0)
                           t (Vector2D/of (* 5 dx) 0)
                           u (Vector2D/of (* 7 dx) 0)
                           v (Vector2D/of (* 9 dx) 0)})
          ]
      {:s2-mesh (triangle-mesh cmplx s2-embedding)
       :xyz-mesh (triangle-mesh cmplx r3-embedding)
       :txt-mesh (triangle-mesh cmplx txt-embedding)})))

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
  "Use the <code<embedding</code> to convert an abstract simplcial
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

(defn- ^Vector3D euclidean-midpoint [points]
  (let [sum (Vector3D$Sum/create)]
    (dorun (map #(.add sum %) points))
    (.multiply (.get sum) (/ 1.0 (count points)))))

(defn- ^Point2S spherical-midpoint
  ([^Point2S p0 ^Point2S p1]
   ;; midpoint of geodesic arc from p0 to p1
   ;; can't just create an arc?
   (assert (> (.distance p0 p1) 1.0e-7)
           (str "distance=" (.distance p0 p1)))
   (.getMidPoint
    (GreatCircles/arcFromPoints
     p0 p1 (Precision/doubleEquivalenceOfEpsilon 1e-12))))

  ([^Point2S p0 ^Point2S p1 ^Point2S p2 ^Point2S p3]
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

  ([points]
   (case (count points)
     1 (first points)
     2 (apply spherical-midpoint points)
     4 (apply spherical-midpoint points)
     ;; else
     (throw
      (UnsupportedOperationException.
       (str "Can't compute spherical-midpoint of "
            (count points) " points."))))))

(defn- midpoint [points]
  "Return a 'center' of some kind for the collection of points.
  Mean for euclidean space, not so obvious for spherical space."
  ;; Assume all points are same class. Should fail otherwise.
  (let [p0 (first points)]
    (cond (instance? Vector3D p0) (euclidean-midpoint points)
          (instance? Point2S p0) (spherical-midpoint points)
          :else (throw (UnsupportedOperationException.
                        (str "No midpoint method for "
                             (.getSimpleName (class p0))))))))

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
       (fn [v] [v (midpoint (points embedding (parent v)))])
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
       (fn [v] [v (midpoint (points embedding (parent v)))])
       (.vertices child))))))

;;---------------------------------------------------------------

(defn- ^Double signed-area [^TwoSimplex face ^IFn txt]
  (let [^Vector2D p0 (txt (.z0 face))
        ^Vector2D p1 (txt (.z1 face))
        ^Vector2D p2 (txt (.z2 face))
        v0 (.subtract p1 p0)
        v1 (.subtract p1 p2)
        ]
    (.signedArea v0 v1)))

;;---------------------------------------------------------------

(defn coordinates-and-elements  [{:keys [^Mesh s2-mesh
                                         ^Mesh txt-mesh
                                         xyz-embedding
                                         rgba-embedding
                                         dual-embedding
                                         txt-embedding]}]
  "Return a float array and an int array suitable for passing to GLSL.
  Don't rely on any ordering of cells and vertices."
  (assert (identical? (.cmplx s2-mesh) (.cmplx txt-mesh)))
  (let [xyz (.embedding ^Mesh (rn/transform xyz-embedding s2-mesh))
        rgba (.embedding ^Mesh (rn/transform rgba-embedding s2-mesh))
        dual (.embedding ^Mesh (rn/transform dual-embedding s2-mesh))
        txt (.embedding txt-mesh)
        ^CellComplex cmplx (.cmplx s2-mesh)
        faces (.faces cmplx)
        zeros (sort (.vertices cmplx))
        zindex (into {} (map (fn [z i] [z i])
                             zeros (range (count zeros))))
        indices (flatten (map (fn [^Cell face]
                                (mapv #(zindex %) (.vertices face)))
                              faces))
        coordinates (flatten (map #(concat (rn/coordinates (xyz %))
                                           (rn/coordinates (rgba %))
                                           (rn/coordinates (dual %))
                                           (rn/coordinates (txt %)))
                                  zeros))]
    (doall
     (let [s2 (.embedding s2-mesh)]
       (map (fn [^TwoSimplex face]
              (println (.toString face))
              (println "signed area: " (signed-area face txt))
              (doall (map (fn [^ZeroSimplex v]
                            ;(println (.toString v))
                            ;(println (rn/coordinates (xyz v)))
                            (println (s2 v))
                            (println (rn/coordinates (txt v))))
                          (.vertices face)))
              (newline))
            faces)))
    [coordinates indices]))
