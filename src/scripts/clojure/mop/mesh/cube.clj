;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\mesh\cube.clj
;;----------------------------------------------------------------
(ns mop.mesh.cube
  (:require
   [mop.commons.debug :as debug]
   [mop.geom.util :as geom]
   [mop.mesh.complex :as cmplx]
   [mop.mesh.mesh :as mesh]
   ))

(let [z0 (cmplx/make-simplex)
      z1 (cmplx/make-simplex)
      z2 (cmplx/make-simplex)
      z3 (cmplx/make-simplex)
      z4 (cmplx/make-simplex)
      z5 (cmplx/make-simplex)
      z6 (cmplx/make-simplex)
      z7 (cmplx/make-simplex)
      q0321 (cmplx/make-quad z0 z3 z2 z1)
      q4567 (cmplx/make-quad z4 z5 z6 z7)
      q0473 (cmplx/make-quad z0 z4 z7 z3)
      q5126 (cmplx/make-quad z5 z1 z2 z6)
      q2376 (cmplx/make-quad z2 z3 z7 z6)
      q0154 (cmplx/make-quad z0 z1 z5 z4)
      qcmplx (cmplx/make-quad-complex
              [q0321 q4567 q0473 q5126 q2376 q0154])
      p0 (geom/make-vector -1.0 -1.0 -1.0)
      p1 (geom/make-vector 1.0 -1.0 -1.0)
      p2 (geom/make-vector 1.0  1.0 -1.0)
      p3 (geom/make-vector -1.0  1.0 -1.0)
      p4 (geom/make-vector -1.0 -1.0  1.0)
      p5 (geom/make-vector 1.0 -1.0  1.0)
      p6 (geom/make-vector 1.0  1.0  1.0)
      p7 (geom/make-vector -1.0  1.0  1.0)
      embedding {z0 p0
                 z1 p1
                 z2 p2
                 z3 p3
                 z4 p4
                 z5 p5
                 z6 p6
                 z7 p7}
      mesh (mesh/make-quad-mesh qcmplx embedding)
      ]
  (debug/echo
   ;;z0 z1 (identical? z0 z1)
   ;;q0321
   ;;embedding
   ;;
   ;; qcmplx
   mesh)
   (mesh/coordinates-and-elements mesh)
  )

