(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.cmplx.complex
  {:doc     "(Abstract) simplicial and cell complexes."
   :author  "palisades dot lakes at gmail dot com"
   :version "2026-02-16"}
  (:require [clojure.set :as set]
            [mop.commons.string :as mcs])
  (:import [java.util List]
           [mop.java.cmplx Cell ZeroSimplex OneSimplex TwoSimplex]))
;;---------------------------------------------------------------
;; TODO: move these to Java to get better control over construction?
;;---------------------------------------------------------------

#_(deftype ZeroSimplex
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
    (compareTo [_ that]
      (- counter (.counter ^ZeroSimplex that)))

    Cell
    (equivalent [this that] (identical? this that))
    (vertices [this] [this])
    ;; could also be false...
    (isOriented [_] true)
    )

;;---------------------------------------------------------------
;; AKA '(Abstract) Edge'.
;; An ordered pair of zero simplexes.

#_(deftype OneSimplex
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
    (isOriented [_] true)
    (equivalent [this that]
      (boolean
       (or (identical? this that)
           (and (instance? OneSimplex that)
                (identical? z0 (.z0 ^OneSimplex that))
                (identical? z1 (.z1 ^OneSimplex that))))))
    )

;;---------------------------------------------------------------
;; TODO: more efficient testing

(defn- minimal-circular-permutation
  "Return a circular permutation of the arguments that leads with
  the minimum."
  ([^Comparable c0 ^Comparable c1 ^Comparable c2]
   (cond
     (and (< (.compareTo c0 c1) 0) (< (.compareTo c0 c2) 0))
     [c0 c1 c2]
     (< (.compareTo c1 c2) 0)
     [c1 c2 c0]
     :else
     [c2 c0 c1]))
  ([^Comparable c0 ^Comparable c1 ^Comparable c2 ^Comparable c3]
   (cond
     (and (< (.compareTo c0 c1) 0)
          (< (.compareTo c0 c2) 0)
          (< (.compareTo c0 c3) 0))
     [c0 c1 c2 c3]
     (and (< (.compareTo c1 c2) 0)
          (< (.compareTo c1 c3) 0))
     [c1 c2 c3 c0]
     (< (.compareTo c2 c3) 0)
     [c2 c3 c0 c1]
     :else
     [c3 c0 c1 c2])))
;;---------------------------------------------------------------
;; AKA '(Abstract) Face/Triangle'.
;; An ordered triple of zero simplexes.

#_(deftype TwoSimplex
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
    (vertices [_] [z0 z1 z2])
    (isOriented [_] true)
    ;; Testing for circular permutation invariant equivalence.
    ;; TODO: move to java and enforce choice of circular permutation in
    ;; private constructor.
    (equivalent [this that]
      (boolean
       (or (identical? this that)
           (and (instance? TwoSimplex that)
                (identical? z0 (.z0 ^TwoSimplex that))
                (identical? z1 (.z1 ^TwoSimplex that)))))))

;;---------------------------------------------------------------

(let [counter (atom -1)]
  (defn simplex

    (^ZeroSimplex [^String name]
     (ZeroSimplex/make (swap! counter inc) name))

    (^OneSimplex [^ZeroSimplex z0
                  ^ZeroSimplex z1]
     (assert (not= z0 z1))
     (OneSimplex/make (swap! counter inc) z0 z1))

    (^TwoSimplex [^ZeroSimplex z0
                  ^ZeroSimplex z1
                  ^ZeroSimplex z2]
     "Enforce consistent choice of circular permutation of vertices,
     preserving orientation."
     (assert (not= z0 z1))
     (assert (not= z0 z2))
     (assert (not= z1 z2))
     (let [n (swap! counter inc)
           [v0 v1 v2] (minimal-circular-permutation z0 z1 z2)]
       (TwoSimplex/make n v0 v1 v2)))))

;;---------------------------------------------------------------

(defn equal-vertices [^TwoSimplex c0 ^TwoSimplex c1]
  "Do the 2 faces have the same vertices in the same order?"
  (and (identical? (.z0 c0) (.z0 c1))
       (identical? (.z1 c0) (.z1 c1))
       (identical? (.z2 c0) (.z2 c1))))

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

(defn vertices [^CellComplex complex] (.vertices complex))
(defn faces [^CellComplex complex] (.faces complex))
;;---------------------------------------------------------------

(defmethod mcs/simple-string SimplicialComplex2D [^SimplicialComplex2D this]
  (str "SimplicialCmplx2D["
       (apply print-str
              (map #(str \newline " " (mcs/simple-string %))
                   (.faces this)))
       "]"))

;;---------------------------------------------------------------
;; TODO: enforce orientedness?

(defn simplicial-complex-2d [faces]
  "Accumulate the vertices from the faces, and sort."
  (let [vertices (sort
                  (into #{}
                        (flatten
                         (map #(vec (.vertices ^Cell %)) faces))))]
    (SimplicialComplex2D. vertices faces)))

;;---------------------------------------------------------------
;; icosahedral 2d simplicial complex with spherical topology

#_(defn ^SimplicialComplex2D icosahedron []
    (let [a (simplex "a") b (simplex "b") c (simplex "c") d (simplex "d")
          e (simplex "e") f (simplex "f") g (simplex "g") h (simplex "h")
          i (simplex "i") j (simplex "j") k (simplex "k") l (simplex "l")]
      (simplicial-complex-2d
       (map #(apply simplex %)
            [[a b c] [a d b] [a c f] [a e d] [a f e]
             [b d g] [b g h] [b h c] [c i f] [c h i]
             [d e j] [d j g] [e f k] [e k j] [f i k]
             [g j l] [g l h] [h l i] [k i l] [k l j]]))))

;; Cut icosahedron to simplify texture mapping and other
;; 2d projections.

#_(defn ^SimplicialComplex2D cut-icosahedron []
    (let [a (simplex "a") b (simplex "b") c (simplex "c") d (simplex "d")
          e (simplex "e") f (simplex "f") g (simplex "g") h (simplex "h")
          i (simplex "i") j (simplex "j") k (simplex "k") l (simplex "l")
          m (simplex "m") n (simplex "n") o (simplex "o")
          p (simplex "p") q (simplex "q") r (simplex "r") s (simplex "s") t (simplex "t")
          u (simplex "u") v (simplex "v")]
      (simplicial-complex-2d
       (map #(apply simplex %)
            [[a f g] [b g h] [c h i] [d i j] [e j k]
             [f l g] [g m h] [h n i] [i o j] [j p k]
             [g l m] [h m n] [i n o] [j o p] [k p q]
             [l r m] [m s n] [n t o] [o u p] [p v q]]))))

;;---------------------------------------------------------------
;; Not a simplex. No independent identity.
;; Used as a key in temp maps during subdivision and other
;; micro-topological operations on a complex.

(deftype VertexPair
  ;; require (.compareTo z0 z1) < 0
  [^ZeroSimplex z0
   ^ZeroSimplex z1]

  ;; TODO: Implement ISeq or something like that?

  Comparable
  (compareTo [_ that]
    (assert (instance? VertexPair that))
    (let [that ^VertexPair that
          c (.compareTo z0 (.z0 that))]
      (if (zero? c)
        (.compareTo z1 (.z1 that))
        c)))

  Object
  (equals [this that]
    (cond
      (identical? this that) true
      (not (instance? VertexPair that)) false
      :else (let [that ^VertexPair that]
              (and (identical? (.z0 this) (.z0 that))
                   (identical? (.z1 this) (.z1 that))))))
  (hashCode [_]
    (let [h (int 17)
          h (unchecked-multiply-int h (int 31))
          h (unchecked-add-int h (.hashCode z0))
          h (unchecked-multiply-int h (int 31))
          h (unchecked-add-int h (.hashCode z1))]
      h))
  (toString [_] (str z0 "->" z1)))


(defn ^VertexPair vertex-pair [^ZeroSimplex z0 ^ZeroSimplex z1]
  (assert (not (.equals z0 z1)))
  (if (< (.compareTo z0 z1) 0)
    (VertexPair. z0 z1)
    (VertexPair. z1 z0)))

;;---------------------------------------------------------------

(defmulti vertex-pairs "Unoriented vertex pairs" class)

(defmethod vertex-pairs TwoSimplex [^TwoSimplex ts]
  (let [a (.z0 ts) b (.z1 ts) c (.z2 ts)]
    #{(vertex-pair a b) (vertex-pair b c) (vertex-pair c a)}))

(defmethod vertex-pairs CellComplex [^CellComplex sc]
  (reduce set/union (map vertex-pairs (.faces sc))))

;;---------------------------------------------------------------
;; TODO: Incorporate alternate subdivision rules,
;; especially with regards to inherited embedding
;; TODO: Should all vertices in the child be new, or should
;; the existing vertices be reused? Currently reusing,
;; so the vertices of the parent complex appear in the child.

(defmulti midpoint-subdivide-4
          "Return a child cell complex with each face of the parent
          subdivided into 4, splitting faces and edges evenly.
          Also return a map from the new vertices to their parent edge or face,
          to permit computing the child embedding from an embedding of the parent,
          where the child vertex position is some centroid
          of the parent edge or face.
          Returned value looks like
          <code>
          {:child new-complex
           :parents {v_0 edge_0 ... v_i face_i ... v_j v_j ...}}"
          class)

;;---------------------------------------------------------------
;; TODO: transient collections?
;; TODO: what about multiple edges connecting same vertices?
;; TODO: prone to stack overflow, not clear why,
;; not so easy to use transients, maybe switch to local mutable java collections

(defmethod midpoint-subdivide-4 SimplicialComplex2D [^SimplicialComplex2D c]
  (loop [faces (.faces c)
         child-faces []
         children {}]
    (if (empty? faces)
      {:child  (simplicial-complex-2d child-faces)
       :parent (set/map-invert children)}
      ;; else
      (let [^TwoSimplex face (first faces)
            ^ZeroSimplex a (.z0 face)
            ^ZeroSimplex b (.z1 face)
            ^ZeroSimplex c (.z2 face)
            eab (vertex-pair a b)
            ebc (vertex-pair b c)
            eca (vertex-pair c a)
            ^ZeroSimplex ab (or (children eab)
                                (simplex (str (.toString (.z0 eab))
                                              (.toString (.z1 eab)))))
            ^ZeroSimplex bc (or (children ebc)
                                (simplex (str (.toString (.z0 ebc))
                                              (.toString (.z1 ebc)))))
            ^ZeroSimplex ca (or (children eca)
                                (simplex (str (.toString (.z0 eca))
                                              (.toString (.z1 eca)))))]
        (recur
         (rest faces)
         ;; overflow with concat instead of conj
         (conj child-faces
               (simplex a ab ca)
               (simplex b bc ab)
               (simplex c ca bc)
               (simplex ab bc ca))
         (merge children {a a, b b, c c, eab ab, ebc bc, eca ca}))))))

;;---------------------------------------------------------------
