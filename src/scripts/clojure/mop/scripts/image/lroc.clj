(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\scripts\image\lroc.clj
;;----------------------------------------------------------------
(ns mop.scripts.image.lroc
  {:doc
   "Work out idempotent image read-write roundtrips."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-12-15"}
  (:refer-clojure :exclude [read reduce])
  (:require
   [clojure.java.io :as io]
   [mop.commons.io :as mci]
   [mop.image.imageio :as imageio]
   [mop.image.util :as image])
  (:import
   [javax.imageio IIOImage]))
;;---------------------------------------------------------------------
(def suffix "-iio")
(defn roundtrip [input]
  (println input)
  ;(image/write-metadata-markdown input)
  (let [input (io/file input)
        [reader-in ^IIOImage image-in] (imageio/read input)
        output (mci/append-to-filename input suffix)
        _ (imageio/write reader-in image-in output)
        rendered-in (.getRenderedImage image-in)
        [_reader-out ^IIOImage image-out] (imageio/read output)
        rendered-out (.getRenderedImage image-out)
        ]
    ;(image/write-metadata-markdown output)
    (assert (image/equals? rendered-in rendered-out))
    ;(.delete output)
    ;(.delete (mci/replace-extension output ".md"))
    ))
;;---------------------------------------------------------------------
(doseq [input
        [
         ;"images/lroc/eo_base_2020_clean_geo.tif"
         "images/lroc/lroc_color_poles_2k.tif"
         ]
        ]
  (roundtrip input))
;;---------------------------------------------------------------------
