(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.cmplx.complex
  {:doc     "(Abstract) simplicial and cell complexes."
   :author  "palisades dot lakes at gmail dot com"
   :version "2026-02-18"}
  (:require
   [clojure.set :as set]
   [mop.commons.string :as mcs])
  (:import
   [java.util List]
   [mop.java.cmplx
    Cell CellComplex
    SimplicialComplex2D ZeroSimplex OneSimplex TwoSimplex]))

;;---------------------------------------------------------------

(defn simplex
  (^ZeroSimplex [^String name]
   (ZeroSimplex/make name))
  (^OneSimplex [^ZeroSimplex z0 ^ZeroSimplex z1]
   (OneSimplex/make z0 z1))
  (^TwoSimplex [^ZeroSimplex z0 ^ZeroSimplex z1 ^ZeroSimplex z2]
   (TwoSimplex/make z0 z1 z2)))

;;---------------------------------------------------------------

(defn equivalent [^Cell c0 ^Cell c1]
  "Do the 2 cells have the same vertices in the same order?"
  (.equivalent c0 c1))

;;---------------------------------------------------------------
;; Abstract 2d simplicial complex.
;; <code>deftype</code over <code>defrecord</code> to avoid
;; clojure generated equals and hashCode --- want identity
;; based equality and corresponding hash codes.
;; TODO: check whether this is really necessary
;; TODO: immutable internal collections

#_(deftype SimplicialComplex2D
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
    (SimplicialComplex2D/make faces))

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
          Also return a map from the new vertices
          to their parent edge or face,
          to permit computing the child embedding
          from an embedding of the parent,
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
