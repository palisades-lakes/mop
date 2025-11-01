(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.geom.mesh
  {:doc     "Embedded cell complexes."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-11-01"}
  (:require [mop.commons.debug :as debug]
            [mop.geom.rn :as rn]
            [mop.geom.s2 :as s2]
            [mop.cmplx.complex :as cmplx])
  (:import [clojure.lang IFn]
           [mop.cmplx.complex Quad QuadComplex]))

;;---------------------------------------------------------------
;; TODO: move these to Java to get better control over construction?
;; TODO: require sorted map for embedding consistency?
;;---------------------------------------------------------------
;; Embedded quadrilateral cell complex.

(deftype QuadMesh
  [^QuadComplex cmplx
   ^IFn embedding]
  :load-ns true
  )

;;---------------------------------------------------------------

(defmethod debug/simple-string QuadMesh [^QuadMesh this]
  (str "QuadMesh[" \newline " "
       (debug/simple-string (.cmplx this))
       \newline
       (debug/simple-string (.-embedding this))
       "]"))

;;---------------------------------------------------------------

(defn make-quad-mesh ^QuadMesh [^QuadComplex cmplx
                                ^IFn embedding]
  (doall
   (map #(assert (not (nil? (embedding %))))
        (.zeros cmplx)))
  (QuadMesh. cmplx embedding))

;;---------------------------------------------------------------
;; just map the transform over the vals of the embedding.
;; TODO: require 1st arg of <code>transform</code> to be a function.
;; and then <code>transform</code> could just be <code>

(defmethod rn/transform [Object QuadMesh] [^Object f ^QuadMesh x]
  (make-quad-mesh
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
        [z0 z1 z2 z3 z4 z5 z6 z7] (.zeros cmplx)]
    (make-quad-mesh
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
        [z0 z1 z2 z3 z4 z5 z6 z7] (.zeros cmplx)]
    (make-quad-mesh
     cmplx
     {z0 (s2/point (rn/vector -1 -1 -1))
      z1 (s2/point (rn/vector 1 -1 -1))
      z2 (s2/point (rn/vector 1 1 -1))
      z3 (s2/point (rn/vector -1 1 -1))
      z4 (s2/point (rn/vector -1 -1 1))
      z5 (s2/point (rn/vector 1 -1 1))
      z6 (s2/point (rn/vector 1 1 1))
      z7 (s2/point (rn/vector -1 1 1))})))

;;---------------------------------------------------------------

(defn coordinates-and-elements [^QuadMesh mesh]
  "Return a float array and an int array suitable for passing to GLSL.
  Don't rely on any ordering of cells and vertices."
  (let [^QuadComplex cmplx (.cmplx mesh)
        embedding (.embedding mesh)
        quads (.quads cmplx)
        ;; assuming iteration over zeros is always in the same order
        zeros (.zeros cmplx)
        zindex (into {} (map (fn [z i] [z i])
                             zeros
                             (range (count zeros))))
        indices (flatten (map (fn [^Quad q] (mapv #(zindex %) (.zeros q))) quads))
        coordinates (flatten (map #(rn/coordinates (embedding %)) zeros))]
    [coordinates indices]))

