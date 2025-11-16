(ns mop.geom.quads
  {:doc     "Quadrilateral meshes."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-11-15"}
  (:require [clojure.set :as set]
            [mop.cmplx.complex :as cmplx]
            [mop.commons.debug :as debug]
            [mop.geom.mesh :as mesh]
            [mop.geom.rn :as rn]
            [mop.geom.s2 :as s2]
            [mop.geom.space :as space])
  (:import [clojure.lang IFn]
           [java.util List]
           [mop.cmplx.complex Cell CellComplex ZeroSimplex]
           [mop.geom.mesh Mesh]))

;;---------------------------------------------------------------
;; Abstract quadrilateral cell.
;; Not a simplex, so maybe should be elsewhere.
;; An ordered quadruple of zero simplexes.

(deftype Quad
  [^long counter
   ^ZeroSimplex z0
   ^ZeroSimplex z1
   ^ZeroSimplex z2
   ^ZeroSimplex z3]
  :load-ns true

  Object
  (toString [_] (str "Q[" counter ";"  z0 "," z1 "," z2 "," z3 ")"))
  (hashCode [_] counter)

  Comparable
  (compareTo [_ that] (- counter (.counter ^Quad that)))

  Cell
  (vertices [_] [z0 z1 z2 z3]) )

;;---------------------------------------------------------------

(let [counter (atom -1)]
  (defn quad ^Quad [^ZeroSimplex z0
                    ^ZeroSimplex z1
                    ^ZeroSimplex z2
                    ^ZeroSimplex z3]
    (assert (not= z0 z1))
    (assert (not= z0 z2))
    (assert (not= z0 z3))
    (assert (not= z1 z2))
    (assert (not= z1 z3))
    (assert (not= z2 z3))
    (Quad. (swap! counter inc) z0 z1 z2 z3)))

;;---------------------------------------------------------------
;; Abstract 2d quadrilateral cell complex.
;; <code>deftype</code over <code>defrecord</code> to avoid
;; clojure generated equals and hashCode --- want identity
;; based equality and corresponding hash codes.
;; TODO: check whether this is really necessary
;; TODO: immutable internal collections

(deftype QuadComplex
  [^List _vertices
   ^List _faces]
  :load-ns true

  #_Object
  #_(toString [_]
              (str "Quad(" counter "; "
                   (.counter z0) ","
                   (.counter z1) ","
                   (.counter z2) ","
                   (.counter z3) ")"))
  #_(hashCode [this] (System/identityHashCode this))
  #_(equals [this that] (identical? this that))

  CellComplex
  (vertices [this] (._vertices this))
  (faces [this] (._faces this))
  )

;;---------------------------------------------------------------

(defmethod debug/simple-string QuadComplex [^QuadComplex this]
  (str "QCmplx["
       (apply print-str
              (map #(str \newline " " (debug/simple-string %))
                   (.faces this)))
       "]"))

;;---------------------------------------------------------------
;; TODO: enforce orientedness?

(defn quad-complex ^QuadComplex [faces]
  "Accumulate the vertices from the faces, and sort."
  (let [vertices (sort (into #{} (flatten (map #(.vertices ^Cell %) faces))))]
    (QuadComplex. vertices faces)))

;;---------------------------------------------------------------

(defmethod cmplx/vertex-pairs Quad [^Quad q]
  (let [a (.z0 q) b (.z1 q) c (.z2 q) d (.z3 q)]
    #{(cmplx/vertex-pair a b)
      (cmplx/vertex-pair b c)
      (cmplx/vertex-pair c d)
      (cmplx/vertex-pair d a)}))

;;---------------------------------------------------------------

(defmethod cmplx/subdivide-4 QuadComplex [^QuadComplex qc]
  (loop [faces (.faces qc)
         child-faces []
         children {}]
    (if (empty? faces)
      {:child (quad-complex child-faces)
       :parent (set/map-invert children)}
      ;; else
      (let [^Quad face (first faces)
            ^ZeroSimplex z0 (.z0 face)
            ^ZeroSimplex z1 (.z1 face)
            ^ZeroSimplex z2 (.z2 face)
            ^ZeroSimplex z3 (.z3 face)
            ;; TODO: what about multiple edges connecting same vertices?
            e01 (sort [z0 z1])
            e12 (sort [z1 z2])
            e23 (sort [z2 z3])
            e30 (sort [z3 z0])
            ^ZeroSimplex z01 (or (children e01) (cmplx/simplex"ab"))
            ^ZeroSimplex z12 (or (children e12) (cmplx/simplex"bc"))
            ^ZeroSimplex z23 (or (children e23) (cmplx/simplex"cd"))
            ^ZeroSimplex z30 (or (children e30) (cmplx/simplex"da"))
            ^ZeroSimplex z0123 (cmplx/simplex"abcd")]
        (recur
         (rest faces)
         (concat child-faces
                 [(quad z30 z0 z01 z0123)
                  (quad z01 z1 z12 z0123)
                  (quad z12 z2 z23 z0123)
                  (quad z23 z3 z30 z0123)])
         (merge children
                {z0 z0 z1 z1 z2 z2 z3 z3
                 e01 z01 e12 z12 e23 z23 e30 z30
                 face z0123}))))))

;;---------------------------------------------------------------
;; TODO: how to ensure that embeddings don't turn the cube inside out?

(defn ^QuadComplex quad-cube []
  "Return an oriented quad complex with 6 faces, topologically
  equivalent to a sphere or cube surface."
  (let [z0 (cmplx/simplex"a")
        z1 (cmplx/simplex"b")
        z2 (cmplx/simplex"c")
        z3 (cmplx/simplex"d")
        z4 (cmplx/simplex"e")
        z5 (cmplx/simplex"f")
        z6 (cmplx/simplex"g")
        z7 (cmplx/simplex"h")
        q0321 (quad z0 z3 z2 z1)
        q4567 (quad z4 z5 z6 z7)
        q0473 (quad z0 z4 z7 z3)
        q5126 (quad z5 z1 z2 z6)
        q2376 (quad z2 z3 z7 z6)
        q0154 (quad z0 z1 z5 z4)]
    (quad-complex [q0321 q4567 q0473 q5126 q2376 q0154])))

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

(defn- quad-mesh ^QuadMesh [^QuadComplex cmplx
                            ^IFn embedding]
  (doall
   (map #(assert (not (nil? (embedding %))))
        (.vertices cmplx)))
  (QuadMesh. cmplx embedding))

;;---------------------------------------------------------------

(defmethod mesh/mesh QuadComplex [^QuadComplex complex ^IFn embedding]
  (quad-mesh complex embedding))

;;---------------------------------------------------------------

(defmethod mesh/points Quad [^IFn embedding ^Quad x]
  [(embedding (.z0 ^Quad x))
   (embedding (.z1 ^Quad x))
   (embedding (.z2 ^Quad x))
   (embedding (.z3 ^Quad x))])

(defmethod mesh/points QuadComplex [^IFn embedding ^QuadComplex x]
  (mesh/points embedding (.vertices ^QuadComplex x)))

;;---------------------------------------------------------------

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
  (let [cmplx (quad-cube)
        ;; TODO: how do we know these are in the right order?
        ;; This isn't feasible for larger complexes!
        ;; Should be some way to walk the complex and get them in the right order.
        [z0 z1 z2 z3 z4 z5 z6 z7] (.vertices cmplx)]
    (mesh/mesh
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
  (let [cmplx (quad-cube)
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

(defmethod cmplx/subdivide-4 QuadMesh [^QuadMesh qm]
  (let [{^CellComplex child :child
         parent             :parent} (cmplx/subdivide-4 (.cmplx qm))
        embedding (.embedding qm) ]
    (mesh/mesh
     child
     (into
      {}
      (map
       (fn [v]
         [v (apply space/midpoint (mesh/points embedding (parent v)))])
       (.vertices child))))))

;;---------------------------------------------------------------
