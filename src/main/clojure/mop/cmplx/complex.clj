(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.cmplx.complex
  {:doc     "(Abstract) simplicial and cell complexes."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-11-13"}
  (:require [clojure.set :as set]
            [mop.commons.debug :as debug])
  (:import [java.util List]))
;;---------------------------------------------------------------
;; TODO: move these to Java to get better control over construction?
;;---------------------------------------------------------------
;; TODO: order matters for orientation, but also want to avoid
;; singularities: what class should <code>zeros</code> be?

(definterface Cell (vertices []))

;;---------------------------------------------------------------
;; AKA '(Abstract) Vertex'.
;; The basic unit of identity used to build
;; simplicial and quad complexes, and embedded meshes from them.
;; Most code uses <code>int</code>s as a low overhead substitute,
;; but this is dangerous with multiple meshes that may share
;;  vertices, edges, etc.

(deftype ZeroSimplex
  [^long counter
   ^String name]
  :load-ns true

  Object
  (toString [_] name)
  (hashCode [_] counter)
  (equals [this that]
    (cond
      (identical? this that) true
      (not (instance? ZeroSimplex that)) false
      :else (== (.counter this) (.counter ^ZeroSimplex that))))

  ;; ordering will be match order of creation within a thread.
  ;; will be used to identify which point goes with which zero simplex
  ;; in embeddings.
  Comparable
  (compareTo [_ that] (- counter (.counter ^ZeroSimplex that)))

  Cell
  (vertices [this] [this]))

;;---------------------------------------------------------------
;; AKA '(Abstract) Edge'.
;; An ordered pair of zero simplexes.

(deftype OneSimplex
  [^long counter
   ^ZeroSimplex z0
   ^ZeroSimplex z1]
  :load-ns true

  Object
  (toString [_] (str z0 "->" z1))
  (hashCode [_] counter)
  (equals [this that]
    (cond
      (identical? this that) true
      (not (instance? OneSimplex that)) false
      :else (== (.counter this) (.counter ^OneSimplex that))))

  Comparable
  (compareTo [_ that] (- counter (.counter ^OneSimplex that)))

  Cell
  (vertices [_] [z0 z1])
  )

;;---------------------------------------------------------------
;; AKA '(Abstract) Face/Triangle'.
;; An ordered triple of zero simplexes.

(deftype TwoSimplex
  [^long counter
   ^ZeroSimplex z0
   ^ZeroSimplex z1
   ^ZeroSimplex z2]
  :load-ns true

  Object
  (toString [_] (str z0 "->" z1 "->" z2))
  (hashCode [_] counter)
  (equals [this that]
    (cond
      (identical? this that) true
      (not (instance? TwoSimplex that)) false
      :else (== (.counter this) (.counter ^TwoSimplex that))))

  Comparable
  (compareTo [_ that] (- counter (.counter ^TwoSimplex that)))

  Cell
  (vertices [_] [z0 z1 z2]))

;;---------------------------------------------------------------

(let [counter (atom -1)]
  (defn simplex
    (^ZeroSimplex [^String name] (ZeroSimplex. (swap! counter inc) name))
    (^OneSimplex [^ZeroSimplex z0
                  ^ZeroSimplex z1]
     (assert (not= z0 z1))
     (OneSimplex. (swap! counter inc) z0 z1))
    (^TwoSimplex [^ZeroSimplex z0
                  ^ZeroSimplex z1
                  ^ZeroSimplex z2]
     (assert (not= z0 z1))
     (assert (not= z0 z2))
     (assert (not= z1 z2))
     (TwoSimplex. (swap! counter inc) z0 z1 z2))))

;;---------------------------------------------------------------

(definterface CellComplex
  (^java.util.List vertices [])
  ;; oriented faces
  (^java.util.List faces []))

;;---------------------------------------------------------------
;; Abstract 2d simplicial complex.
;; <code>deftype</code over <code>defrecord</code> to avoid
;; clojure generated equals and hashCode --- want identity
;; based equality and corresponding hash codes.
;; TODO: check whether this is really necessary
;; TODO: immutable internal collections

(deftype SimplicialComplex2D
  [^List _vertices
   ^List _faces]
  :load-ns true

  #_Object
  #_(toString [_]
              (str "SimplicialCmplx2D(" counter "; "
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

(defmulti vertex-pairs "Unoriented vertex pairs" class)

(defmethod vertex-pairs TwoSimplex [^TwoSimplex ts]
  (let [a (.z0 ts) b (.z1 ts) c (.z2 ts)]
    #{(sort [a b]) (sort [b c]) (sort [c a])}))

(defmethod vertex-pairs SimplicialComplex2D [^SimplicialComplex2D sc]
  (reduce set/union (map vertex-pairs (.faces sc))))

;;---------------------------------------------------------------

(defmethod debug/simple-string SimplicialComplex2D [^SimplicialComplex2D this]
  (str "SimplicialCmplx2D["
       (apply print-str
              (map #(str \newline " " (debug/simple-string %))
                   (.faces this)))
       "]"))

;;---------------------------------------------------------------
;; TODO: enforce orientedness?

(defn simplicial-complex-2d [faces]
  "Accumulate the vertices from the faces, and sort."
  (let [vertices (sort (into #{} (flatten (map #(.vertices ^Cell %) faces))))]
    (SimplicialComplex2D. vertices faces)))

;;---------------------------------------------------------------
;; icosahedral 2d simplicial complex with spherical topology

(defn ^SimplicialComplex2D icosahedron []
  (let [a (simplex "a") b (simplex"b") c (simplex"c") d (simplex"d")
        e (simplex"e") f (simplex"f") g (simplex"g") h (simplex"h")
        i (simplex"i") j (simplex"j") k (simplex"k") l (simplex"l")]
    (simplicial-complex-2d
     (map #(apply simplex %)
          [[a b c] [a d b] [a c f] [a e d] [a f e]
           [b d g] [b g h] [b h c] [c i f] [c h i]
           [d e j] [d j g] [e f k] [e k j] [f i k]
           [g j l] [g l h] [h l i] [k i l] [k l j]]))))

;; Cut icosahedron to simplify texture mapping and other
;; 2d projections.

(defn ^SimplicialComplex2D cut-icosahedron []
  (let [a (simplex "a") b (simplex"b") c (simplex"c") d (simplex"d")
        e (simplex"e") f (simplex"f") g (simplex"g") h (simplex"h")
        i (simplex"i") j (simplex"j") k (simplex"k") l (simplex"l")
        m (simplex"m") n (simplex"n") o (simplex"o")
        p (simplex"p") q (simplex"q") r (simplex"r") s (simplex"s") t (simplex"t")
        u (simplex"u") v (simplex"v")]
    (simplicial-complex-2d
     (map #(apply simplex %)
          [[a f g] [b g h] [c h i] [d i j] [e j k]
           [f l g] [g m h] [h n i] [i o j] [j p k]
           [g l m] [h m n] [i n o] [j o p] [k p q]
           [l r m] [m s n] [n t o] [o u p] [p v q]]))))

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
;; TODO: how to ensure that embeddings don't turn the cube inside out?

(defn ^QuadComplex quad-cube []
  "Return an oriented quad complex with 6 faces, topologically
  equivalent to a sphere or cube surface."
  (let [z0 (simplex"a")
        z1 (simplex"b")
        z2 (simplex"c")
        z3 (simplex"d")
        z4 (simplex"e")
        z5 (simplex"f")
        z6 (simplex"g")
        z7 (simplex"h")
        q0321 (quad z0 z3 z2 z1)
        q4567 (quad z4 z5 z6 z7)
        q0473 (quad z0 z4 z7 z3)
        q5126 (quad z5 z1 z2 z6)
        q2376 (quad z2 z3 z7 z6)
        q0154 (quad z0 z1 z5 z4)]
    (quad-complex [q0321 q4567 q0473 q5126 q2376 q0154])))

;;---------------------------------------------------------------
;; TODO: Incorporate alternate subdivision rules,
;; especially with regards to inherited embedding
;; TODO: Should all vertices in the child be new, or should
;; the existing vertices be reused? Currently reusing,
;; so the vertices of the parent complex appear in the child.

(defmulti subdivide-4
          "Return a child cell complex with each face of the parent
          subdivided into 4, splitting faces and edges evenly.
          Also return a map from the new vertices to their parent edge or face,
          to permit computing the child embedding from an embedding of the parent,
          where the child vertex position is some centroid
          of the parent edge or face.
          Returned value looks like
          <code>
          {:child new-complex
           :parents {v0 edge0 ... vi facei ... vj vj ...}}"
          class)

;;---------------------------------------------------------------
;; TODO: transient collections?
;; TODO: what about multiple edges connecting same vertices?
;; TODO: prone to stack overflow, not clear why,
;; not so easy to use transients, maybe switch to local mutable java collections

(defmethod subdivide-4 SimplicialComplex2D [^SimplicialComplex2D c]
  (loop [faces (.faces c)
         child-faces []
         children {}]
    (if (empty? faces)
      {:child (simplicial-complex-2d child-faces)
       :parent (set/map-invert children)}
      ;; else
      (let [^TwoSimplex face (first faces)
            ^ZeroSimplex a (.z0 face)
            ^ZeroSimplex b (.z1 face)
            ^ZeroSimplex c (.z2 face)
            eab (sort [a b])
            ebc (sort [b c])
            eca (sort [c a])
            ^ZeroSimplex ab (or (children eab) (simplex"ab"))
            ^ZeroSimplex bc (or (children ebc) (simplex"bc"))
            ^ZeroSimplex ca (or (children eca) (simplex"ca"))]
        (recur
         (rest faces)
         ;; overflow with concat instead of conj
         (conj child-faces (simplex a ab ca) (simplex b bc ab) (simplex c ca bc) (simplex ab bc ca))
         (merge children {a a, b b, c c, eab ab, ebc bc, eca ca}))))))

;;---------------------------------------------------------------

(defmethod subdivide-4 QuadComplex [^QuadComplex qc]
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
            ^ZeroSimplex z01 (or (children e01) (simplex"ab"))
            ^ZeroSimplex z12 (or (children e12) (simplex"bc"))
            ^ZeroSimplex z23 (or (children e23) (simplex"cd"))
            ^ZeroSimplex z30 (or (children e30) (simplex"da"))
            ^ZeroSimplex z0123 (simplex"abcd")]
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
