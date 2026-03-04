(set! *warn-on-reflection* true)
;;-------------------------------------------------------------------
(ns mop.commons.json
  "Hide the choice of underlying json library.
  Current options are
  <a href=\"https://github.com/clojure/data.json\">
  clojure.data.json</a>
  and
  <a href=\"https://github.com/dakrone/cheshire\">
  cheshire</a>.
  <br>
  Cheshire is roughly twice as fast as c.data.json on a few
  test examples, but brings in largish dependencies,
  on fasterxml.jackson 2.20 (3.0 released Oct 2025).
  <br>
  So, using clojure.data.json for now."
  {:author  "palisades dot lakes at gmail dot com"
   :version "2026-03-04"}
  (:require [clojure.java.io :as io]
            [clojure.data.json :as cdj]))
;;-------------------------------------------------------------------
(set! *unchecked-math* :warn-on-boxed)
;;-------------------------------------------------------------------
(defn read-json [json]
  "Return clojure (edn) data ~equivalent~ to the json text."
  (with-open [json (io/reader json)]
    (cdj/read json)))
;;-------------------------------------------------------------------
(defn write-json [edn json]
  "Write json text ~equivalent~ to the clojure (edn) data."
  (let [json (io/file json)]
    (io/make-parents json)
    (with-open [json (io/writer json)]
      (cdj/write edn json :indent true :escape-slash false))))
;;-------------------------------------------------------------------
