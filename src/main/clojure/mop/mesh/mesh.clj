(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.mesh.mesh
  {:doc     "Embedded cell complexes."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-10-30"}
  (:require [mop.commons.debug :as debug]
            [mop.geom.util :as geom]
            [mop.mesh.complex :as cmplx])
  (:import [clojure.lang IFn]
           [mop.mesh.complex Quad QuadComplex]))

;;---------------------------------------------------------------
;; TODO: move these to Java to get better control over construction?
;;---------------------------------------------------------------
;; Embedded quadrilateral cell complex.

(deftype QuadMesh
  [^QuadComplex cmplx
   ^IFn embedding]
  {:load-ns true}
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
        coordinates (flatten (map #(geom/coordinates (embedding %)) zeros))]
    [coordinates indices]))

