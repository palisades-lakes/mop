(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.commons.debug

  "Things that ought to be in <code>clojure.core</code>, and don't have an
  obvious place elsewhere in Mop."

  {:author  "palisades dot lakes at gmail dot com"
   :version "2025-11-24"}

  (:require [mop.commons.string :as mcs]))

;;----------------------------------------------------------------

(defmacro echo
  "Print the expressions followed by their values.
   Useful for quick logging."
  [& exps]
  (let [ppe (map print-str exps)
        s (gensym "s")
        e (gensym "e")
        v (gensym "v")]
    `(let [~s (mapv (fn [~e ~v]
                      (str ~e \newline
                           (mcs/simple-string ~v)))
                    [~@ppe]
                    [~@exps])]
       (doall (map println ~s)))))

;;----------------------------------------------------------------
