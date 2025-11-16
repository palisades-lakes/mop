(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.geom.mesh
  {:doc     "Embedded cell complexes."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-11-15"}
  (:require [mop.cmplx.complex :as cmplx]
            [mop.geom.rn :as rn]
            [mop.geom.s2 :as s2]
            [mop.geom.space :as space])
  (:import [clojure.lang IFn]
           [java.util List]
           [mop.cmplx.complex Cell CellComplex OneSimplex SimplicialComplex2D
                              TwoSimplex ZeroSimplex]
           [org.apache.commons.geometry.spherical.twod GreatArc Point2S]))

;;---------------------------------------------------------------

(definterface Mesh
  (^mop.cmplx.complex.CellComplex cmplx [])
  (^clojure.lang.IFn embedding []))

(defn faces [^Mesh mesh] (.faces (.cmplx mesh)))

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

(defmulti mesh
          "Create a triangle or quad mesh, depending on the complex."
          (fn [cmplx _embedding] (class cmplx)))

(defmethod mesh SimplicialComplex2D [^SimplicialComplex2D complex ^IFn embedding]
  (triangle-mesh complex embedding))

;;---------------------------------------------------------------
;; just map the transform over the vals of the embedding.
;; TODO: require 1st arg of <code>transform</code> to be a function.
;; and then <code>transform</code> could just be <code>

(defmethod rn/transform [Object TriangleMesh] [^Object f ^TriangleMesh x]
  (triangle-mesh
   (.cmplx x)
   (update-vals (.embedding x) #(rn/transform f %))))

;;---------------------------------------------------------------
;; TODO: force embedding to return points of some kind.

(defmulti
 points
 "Use the <code<embedding</code> to convert an abstract simplicial
object <code>x</code> to a list of points in some space,
most likely R^3 or S^2."
 (fn [_embedding x] (class x)))

(defmethod points List [^IFn embedding ^List x]
  (mapv embedding x))

(defmethod points ZeroSimplex [^IFn embedding ^ZeroSimplex x]
  [(embedding x)])

(defmethod points OneSimplex [^IFn embedding ^OneSimplex x]
  [(embedding (.z0 ^OneSimplex x))
   (embedding (.z1 ^OneSimplex x))])

(defmethod points TwoSimplex [^IFn embedding ^TwoSimplex x]
  [(embedding (.z0 ^TwoSimplex x))
   (embedding (.z1 ^TwoSimplex x))
   (embedding (.z2 ^TwoSimplex x))])

(defmethod points SimplicialComplex2D [^IFn embedding ^SimplicialComplex2D x]
  (points embedding (.vertices ^SimplicialComplex2D x)))


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
  (let [{^CellComplex child :child parent :parent}
        (cmplx/subdivide-4 (.cmplx m))
        embedding (.embedding m)]
    (mesh
     child
     (into
      {}
      (map
       (fn [v] [v (apply space/midpoint (points embedding (parent v)))])
       (.vertices child))))))

;;---------------------------------------------------------------

(defn ^GreatArc arc
  ([^IFn s2 ^ZeroSimplex a ^ZeroSimplex b]
   (s2/arc ^Point2S (s2 a) ^Point2S (s2 b)))
  ([s2 ^OneSimplex ab] (arc s2 (.z0 ab) (.z1 ab))))

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

;;---------------------------------------------------------------
