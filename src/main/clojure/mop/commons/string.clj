(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.commons.string

  "Things that ought to be in <code>clojure.string</code>, and don't have an
  obvious place elsewhere in Mop."

  {:author  "palisades dot lakes at gmail dot com"
   :version "2025-11-24"}

  (:import [clojure.lang MapEntry]
           [java.util Map]))

;;----------------------------------------------------------------

(defn ^String truncate-string
  ([^String s ^long maxlen]
   (if (and s (> (.length s) maxlen))
     (str (.substring s 0 maxlen) "...")
     s))
  ([^String s] (truncate-string s 1024)))

;;----------------------------------------------------------------

#_(defn starts-with? [^String s ^String prefix]
  "null safe."
  (when (and s prefix) (.startsWith s prefix)))

;;----------------------------------------------------------------
;; TODO: formatting like pprint

(defmulti simple-string
          "Return a string for debug logging, with just enough info."
          class)

(defmethod simple-string nil [_]
  "nil")

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
