(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.commons.debug

  "Things that ought to be in clojure.core, and don't have an
  obvious place elsewhere in Mop."

  {:author  "palisades dot lakes at gmail dot com"
   :version "2025-10-30"}

  (:import [clojure.lang MapEntry]
           [java.util Map]))

;;----------------------------------------------------------------
;; TODO: formatting like pprint

(defmulti simple-string
          "Return a string for debug logging, with just enough info."
          class)

(defmethod simple-string Object [^Object this]
  (.toString this))

(defmethod simple-string Iterable [^Iterable this]
  (str (.getSimpleName (class this)) "["
       (apply str (map simple-string (sequence this)))
       "]"))

(defmethod simple-string Map [^Map this]
  (str "{"
       (apply str (map simple-string (sequence this)))
       "}"))

(prefer-method simple-string Map Iterable)

(defmethod simple-string MapEntry [^MapEntry this]
  (str " ["
       (simple-string (key this))
       " "
       (simple-string (val this))
       "]" \newline))

;;----------------------------------------------------------------

(defmacro echo
  "Print the expressions followed by their values.
   Useful for quick logging."
  [& exps]
  (let [ppe (map print-str exps)]
    `(let [strings# (mapv (fn [e# v#]
                           (str e# \newline (simple-string v#) \newline ))
                         [~@ppe]
                         [~@exps])]
       (doall (map println strings#)))))

;;----------------------------------------------------------------
