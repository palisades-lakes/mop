(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\scripts\image\imageio\simple.clj
;;----------------------------------------------------------------
(ns mop.scripts.image.imageio.simple
  {:doc
   "Work out idempotent image read-write roundtrips."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-12-06"}

  (:require
   [clojure.java.io :as io]
   [mop.commons.debug :as debug]
   [mop.commons.io :as mci]
   [mop.image.util :as image])
  (:import
   [javax.imageio ImageIO]))
;---------------------------------------------------------------------
(def suffix "-simple" )
(defn roundtrip [input]
  (println)
  (debug/echo input)
  (image/write-metadata-markdown input)
  (let [output (mci/append-to-filename input suffix)
        rendered-in (ImageIO/read (io/file input))
        _ (ImageIO/write rendered-in (mci/extension input) (io/file output))
        rendered-out  (ImageIO/read (io/file output))
        ]
    (image/write-metadata-markdown output)
    (assert (image/equals? rendered-in rendered-out))))
;;---------------------------------------------------------------------
(roundtrip "images/imageio/ETOPO_2022_v1_60s_PNW_bed.tiff")
(roundtrip "images/imageio/ETOPO_2022_v1_60s_N90W180_bed.tif")
;;---------------------------------------------------------------------
