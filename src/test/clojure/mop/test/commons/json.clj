(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;------------------------------------------------------------------------------
(ns mop.test.commons.json
  ^{:author "palisades dot lakes at gmail dot com"
    :date   "2026-03-03"
    :doc    "Tests for mop.commons.json."}
  (:require [clojure.java.io :as io]
            [clojure.test :as t]
            [mop.commons.io :as mci]
            [mop.commons.json :as json]))
;;------------------------------------------------------------------------------
;; mvn -Dtest=mop.test.commons.json compile clojure:test
;;------------------------------------------------------------------------------
;; TODO: test edn2json2edn roundtrips

(defn- roundtrip-json2edn2json [json]
  (let [tmp (mci/append-to-filename json "-tmp")
        edn0 (json/read-json json)
        _ (json/write-json edn0 tmp)
        edn1 (json/read-json tmp)]
    (try
      (t/is (= edn0 edn1))
      (finally (io/delete-file tmp true)))))

(t/deftest json2edn2json
  (t/testing "json->edn->json roundtrip"
    (roundtrip-json2edn2json "src/test/resources/json/cars.json")
    ;; skip large file for now
    #_(roundtrip-json2edn2json "src/test/resources/json/flights-200k.json")
    (roundtrip-json2edn2json "src/test/resources/json/geo_layer_line_london.vl.json")
    (roundtrip-json2edn2json "src/test/resources/json/interactive_splom.vl.json")
    (roundtrip-json2edn2json "src/test/resources/json/londonBoroughs.json")
    (roundtrip-json2edn2json "src/test/resources/json/londonCentroids.json")
    (roundtrip-json2edn2json "src/test/resources/json/londonTubeLines.json")))
