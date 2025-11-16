(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\debug\subdivide.clj
;;----------------------------------------------------------------
(ns mop.debug.subdivide
  {:doc     "check subdivision"
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-11-13"}

  (:require
   [mop.commons.debug :as debug]
   [mop.geom.mesh :as mesh]
   [mop.geom.quads :as quads]
   [mop.geom.rn :as rn]
   [mop.geom.s2 :as s2])
  (:import [org.apache.commons.geometry.euclidean.threed Vector3D]))

;;-------------------------------------------------------------
;; TODO: convert to unit test, check singular cases.

(let [
      ;z0 (cmplx/simplex "a")
      ;z1 (cmplx/simplex "b")
      ;z2 (cmplx/simplex "c")
      ;z3 (cmplx/simplex "d")
      ;z4 (cmplx/simplex "e")
      ;z5 (cmplx/simplex "f")
      ;q0321 (cmplx/quad z0 z1 z2 z3)
      ;q1452 (cmplx/quad z1 z4 z5 z2)
      ;pair (cmplx/quad-complex [q0321 q1452])
      ;{child :child parent :parent} (cmplx/subdivide-4 pair)
      mesh (quads/standard-quad-sphere)
      embedding-ll (s2/equirectangular-embedding 360 180)
      embedding-r3 (s2/r3-embedding (Vector3D/of 0 0 0) 100)
      mesh-ll (rn/transform embedding-ll mesh)
      mesh-r3 (rn/transform embedding-r3 mesh)
      ]
  ;(debug/echo pair)
  ;(debug/echo child)
  ;(debug/echo parent)
  ;(debug/echo mesh)
  ;(debug/echo (cmplx/subdivide-4 mesh))
  (debug/echo (mesh/coordinates-and-elements mesh-ll))
  (debug/echo (mesh/coordinates-and-elements mesh-r3))
  )


