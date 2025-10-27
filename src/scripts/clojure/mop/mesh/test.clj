;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\mesh\test.clj
;;----------------------------------------------------------------
(ns mop.mesh.test
  (:require
   [clojure.pprint :as pp]
   [mop.mesh.simplex :as simplex]))

(let [z0 (simplex/make)
      z1 (simplex/make)
      z2 (simplex/make)
      e0 (simplex/make z0 z1)
      e1 (simplex/make z1 z0)
      e2 (simplex/make z0 z1)
      f0 (simplex/make z0 z1 z2)]
  (pp/pprint (str "z0: " z0))
  (pp/pprint (str "z1: " z1))
  (pp/pprint (str "(identical? z0 z1): " (identical? z0 z1)))
  (pp/pprint (str "e0: " e0))
  (pp/pprint (str "e1: " e1))
  (pp/pprint (str "(identical? e0 e1): " (identical? e0 e1)))
  (pp/pprint (str "e2: " e2))
  (pp/pprint (str "(identical? e0 e2): " (identical? e0 e2)))
  (pp/pprint (sort [e2 e1 e0]))
  (pp/pprint (str "f0: " f0))
  (pp/pprint
  (try
    (.compareTo z0 e1)
    (catch ClassCastException e
      "Expected ClassCastException thrown."))))

