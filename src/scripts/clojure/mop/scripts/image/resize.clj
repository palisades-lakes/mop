(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\scripts\image\resize.clj
;;----------------------------------------------------------------
(ns mop.scripts.image.resize
  {:doc
   "Resize images."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-12-13"}

  (:require
   [clojure.java.io :as io]
   [mop.commons.debug :as debug]
   [mop.commons.io :as mci]
   [mop.image.imageio :as imageio]
   [mop.image.util :as image])
  (:import
   [javax.imageio IIOImage]))
;;----------------------------------------------------------------------
(defn wh [^IIOImage image]
  (if (.hasRaster image)
    [(.getWidth (.getRaster image)) (.getHeight (.getRaster image))]
    [(.getWidth (.getRenderedImage image)) (.getHeight (.getRenderedImage image))]))
;;---------------------------------------------------------------------
(defn resize [input]
  (println)
  (debug/echo input)
  (image/write-metadata-markdown input)
  (let [max-dim 2048
        [reader ^IIOImage image] (imageio/read input)
        resized (imageio/subsample image max-dim)]
    (when resized
      (let [[w h] (wh resized)
            output (mci/append-to-filename input (str "-" w "x" h))]
        (imageio/write reader resized output)
        (image/write-metadata-markdown output)
        (debug/echo output)))))
;;---------------------------------------------------------------------
(doseq [input
        (image/image-file-seq (io/file "images/moon"))
        #_[
         "images/imageio/eo_base_2020_clean_geo.tif"
         "images/imageio/gebco_08_rev_elev_21600x10800.png"
         "images/imageio/world.topo.bathy.200412.3x5400x2700.png"

         "images/imageio/USGS_13_n38w077_dir5.tiff"

         "images/imageio/ETOPO_2022_v1_60s_PNW_bed.tiff"
         "images/imageio/ETOPO_2022_v1_60s_N90W180_bed.tif"
         "images/imageio/ldem_4.tif"
         "images/imageio/ETOPO_2022_v1_60s_N90W180_bed-lzw.tif"
         ]
        ]
  (resize input))
;;---------------------------------------------------------------------
