(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\scripts\image\resize.clj
;;----------------------------------------------------------------
(ns mop.scripts.image.resize
  {:doc
   "Resize images."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-12-10"}

  (:require
   [clojure.java.io :as io]
   [mop.commons.debug :as debug]
   [mop.commons.io :as mci]
   [mop.image.imageio :as imageio]
   [mop.image.util :as image])
  (:import
   [javax.imageio IIOImage ImageReader]))
;;---------------------------------------------------------------------
(defn resize [input]
  (println)
  (debug/echo input)
  (let [max-dim 256
        [^ImageReader reader ^IIOImage image] (imageio/read input)
        r (.getRenderedImage image)]
    (when (or (< max-dim (.getWidth r)) (< max-dim (.getHeight r)))
      (image/write-metadata-markdown input)
      (let [^IIOImage resized (imageio/reduce-iioimage image max-dim)
            rendered (.getRenderedImage resized)
            w (.getWidth rendered)
            h (.getHeight rendered)
            output (mci/append-to-filename input (str "-" w "x" h))]
        (imageio/write reader resized output)
        (.dispose reader)
        (image/write-metadata-markdown output)))))
;;---------------------------------------------------------------------
(doseq [input [
               "images/imageio/USGS_13_n38w077_dir5.tiff"
               "images/imageio/ETOPO_2022_v1_60s_PNW_bed.tiff"
               #_"images/imageio/ETOPO_2022_v1_60s_N90W180_bed.tif"
               "images/imageio/eo_base_2020_clean_geo.tif"
               "images/imageio/gebco_08_rev_elev_21600x10800.png"
               "images/imageio/ldem_4.tif"
               "images/imageio/world.topo.bathy.200412.3x5400x2700.png"
               ]
  #_(image/image-file-seq (io/file "images/imageio"))]
  (resize input))
;;---------------------------------------------------------------------
