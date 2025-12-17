(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\scripts\image\rwr.clj
;;----------------------------------------------------------------
(ns mop.scripts.image.rwr
  {:doc
   "Work out idempotent image read-write roundtrips."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-12-15"}
  (:refer-clojure :exclude [read reduce])
  (:require
   [clojure.java.io :as io]
   [clojure.string :as s]
   [mop.commons.io :as mci]
   [mop.image.util :as image])
  (:import
   [javax.imageio ImageIO]))
;;---------------------------------------------------------------------
(def suffix "-rwr")
(defn roundtrip [input]
  (println input)
  (image/write-metadata-markdown input)
  (let [input (io/file input)
        rendered-in (ImageIO/read input)
        output (mci/append-to-filename input suffix)
        _ (ImageIO/write rendered-in "TIFF" output)
        rendered-out (ImageIO/read output)
        ]
    (image/write-metadata-markdown output)
    (assert (image/equals? rendered-in rendered-out))
    ;(.delete output)
    ;(.delete (mci/replace-extension output ".md"))
    ))
;;---------------------------------------------------------------------
(defn output? [f]
  (let [name (mci/prefix f)]
    (or (s/ends-with? name "-rwr")
        (s/ends-with? name "-iio")
        (s/ends-with? name "-ii0x"))))
(defn failure? [f]
  (println (mci/prefix f))
  (#{"ETOPO_2022_v1_60s_N90W180_bed"
     "ETOPO_2022_v1_60s_N90W180_geoid"
     "ETOPO_2022_v1_60s_N90W180_surface"
     "USGS_13_n38w077_dir5"
     "world.200412.3x21600x10800"
     "world.topo.bathy.200412.3x21600x10800"
     "world.topo.bathy.200412.3x5400x2700"}
   (mci/prefix f)))
;;---------------------------------------------------------------------
(doseq [input

        (remove #(or (output? %) (failure? %))
                (image/image-file-seq (io/file "images")))
        #_[
           "images/lroc/lroc_color_poles_2k.tif"
           "images/lroc/lroc_color_poles.tif"
           ]
        ]
  (roundtrip input))
;;---------------------------------------------------------------------
