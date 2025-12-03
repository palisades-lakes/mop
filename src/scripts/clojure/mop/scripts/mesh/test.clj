;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\mesh\test.clj
;;----------------------------------------------------------------
(ns mop.scripts.mesh.test
  (:require
   [clojure.pprint :as pp]
   [mop.commons.debug :as mop]
   [mop.cmplx.complex :as cmplx]))

(let [z0 (cmplx/simplex "a")
      z1 (cmplx/simplex "b")
      z2 (cmplx/simplex "c")
      e0 (cmplx/simplex z0 z1)
      e1 (cmplx/simplex z1 z0)
      e2 (cmplx/simplex z0 z1)
      f0 (cmplx/simplex z0 z1 z2)
      c (cmplx/simplicial-complex-2d [f0])]
  (newline)
  (binding [*print-readably* false
            *print-dup* false
            pp/*print-pretty* true]
    (println (str e2))
    (println (str f0))
    (println (str [e2 e1 e0]))
    (println c))
  (newline)
  #_( mop/echo
     ;z0 z1 (identical? z0 z1)
     ;e0 e1 (identical? e0 e1)
     ;e2 (identical? e0 e2)
     (sort [e2 e1 e0])
     #_(try
         (.compareTo z0 e1)
         (catch ClassCastException e
           "Expected ClassCastException thrown.")))
  )