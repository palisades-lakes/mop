(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.commons.string

  "Things that ought to be in <code>clojure.string</code>, and don't have an
  obvious place elsewhere in Mop."

  {:author  "palisades dot lakes at gmail dot com"
   :version "2025-12-05"}

  (:import [clojure.lang MapEntry]
           [java.util Map]
           [mop.java Description]))

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
       (apply str (interpose " " (map simple-string (sequence this))))
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
;;---------------------------------------------------------------------

(defn ^String description [x] (Description/description x))

;(defmulti ^String description
;          "Return a useful (multiline) string, for debugging."
;          class)
;
;(defmethod description nil [_]
;  "nil")
;
;(let [c (class (byte-array 0))]
;  (defmethod description c [this]
;    (Arrays/toString ^bytes this)))
;
;(defmethod description Object [this]
;  (if (.isArray (class this))
;    (Arrays/toString this)
;    (.toString this)))
;
;(defmethod description Iterable [^Iterable this]
;  (str (.getSimpleName (class this)) "["
;       (apply str (map description (sequence this)))
;       "]"))
;
;(defmethod description Map [^Map this]
;  (str "{"
;       (apply str (map description (sequence this)))
;       "}"))
;
;(prefer-method description Map Iterable)
;
;(defmethod description MapEntry [^MapEntry this]
;  (str " ["
;       (description (key this))
;       " "
;       (description (val this))
;       "]" \newline))
;
;;---------------------------------------------------------------------
