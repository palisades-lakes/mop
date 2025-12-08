(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\scripts\image\imageio\roundtrip.clj
;;----------------------------------------------------------------
(ns mop.scripts.image.imageio.roundtrip
  {:doc
   "Work out idempotent image read-write roundtrips."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-12-07"}

  (:require
   [clojure.java.io :as io]
   [mop.commons.io :as mci]
   [mop.image.imageio :as imageio]
   [mop.image.util :as image])
  (:import
   [javax.imageio IIOImage]))
;;---------------------------------------------------------------------
;; TODO: check metadata
(def suffix "-iio" )
(defn roundtrip [input]
  (println input)
  (image/write-metadata-markdown input)
  (let [input (io/file input)
        output (mci/append-to-filename input suffix)
        [reader-in ^IIOImage image-in] (imageio/read input)
        _(imageio/write reader-in image-in output)
        rendered-in (.getRenderedImage image-in)
        [_reader-out ^IIOImage image-out] (imageio/read output)
        rendered-out (.getRenderedImage image-out)
        ]
    (image/write-metadata-markdown output)
    (assert (image/equals? rendered-in rendered-out))))
;;---------------------------------------------------------------------
(doseq [input [
               "images/imageio/ETOPO_2022_v1_60s_N90W180_bed.tif"
               "images/imageio/eo_base_2020_clean_geo.tif"
               "images/imageio/ETOPO_2022_v1_60s_PNW_bed.tiff"
               "images/imageio/gebco_08_rev_elev_21600x10800.png"
               "images/imageio/ldem_4.tif"
               "images/imageio/USGS_13_n38w077_dir5.tiff"
               "images/imageio/world.topo.bathy.200412.3x5400x2700.png"
               ]]
  (roundtrip input))
;;---------------------------------------------------------------------
