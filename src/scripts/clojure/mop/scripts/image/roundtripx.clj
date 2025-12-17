(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\scripts\image\roundtripx.clj
;;----------------------------------------------------------------
(ns mop.scripts.image.roundtripx
  {:doc
   "Work out idempotent image read-write roundtrips."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-12-15"}
  (:refer-clojure :exclude [read reduce])
  (:require
   [clojure.java.io :as io]
   [clojure.string :as s]
   [mop.commons.io :as mci]
   [mop.image.imageiox :as imageio]
   [mop.image.util :as image])
  (:import
   [javax.imageio IIOImage]))
;;---------------------------------------------------------------------
(def suffix "-gtx")
(defn roundtrip [input]
  (println input)
  (image/write-metadata-markdown input)
  (let [input (io/file input)
        [reader-in ^IIOImage image-in] (imageio/read input)
        output (mci/append-to-filename input suffix)
        _ (imageio/write reader-in image-in output)
        rendered-in (.getRenderedImage image-in)
        [_reader-out ^IIOImage image-out] (imageio/read output)
        rendered-out (.getRenderedImage image-out)
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
        (s/ends-with? name "-iiox")
        (s/ends-with? name "-iioxx")
        (s/ends-with? name "-12m")
        (s/ends-with? name "-gt")
        (s/ends-with? name "-gtx"))))
(defn failure? [f]
  (println (mci/prefix f))
  (#{
     "notables"
     "sampleRGBIR"
     ;"ETOPO_2022_v1_60s_N90W180_bed"
     ;"ETOPO_2022_v1_60s_N90W180_geoid"
     ;"ETOPO_2022_v1_60s_N90W180_surface"
     ;;"USGS_13_n38w077_dir5"
     ;"lroc_color_poles"
     ;"lroc_color_poles_2k"
     ;"lroc_color_poles_4k"
     ;"lroc_color_poles_8k"
     ;"lroc_color_poles_16k"
     }
   (mci/prefix f)))
;;---------------------------------------------------------------------
(doseq [input
        (remove #(or (output? %) (failure? %))
                (image/image-file-seq (io/file "images/imageio-ext")))
        #_[
           "images/lroc/eo_base_2020_clean_geo.tif"
           "images/lroc/lroc_color_poles_2k.tif"
           ;"images/imageio/ldem_64_uint.tif"
           ;"images/imageio/ldem_64.tif"
           ;"images/imageio/lroc_color_poles.tif"
           ;"images/imageio/lroc_color_poles-iiox.tif"
           ;"images/imageio/USGS_13_n38w077_dir5.tiff"
           ;"images/imageio/ETOPO_2022_v1_60s_PNW_bed.tiff"
           ;"images/imageio/ETOPO_2022_v1_60s_N90W180_bed.tif"
           ;"images/imageio/eo_base_2020_clean_geo.tif"
           ;"images/imageio/gebco_08_rev_elev_21600x10800.png"
           ;"images/imageio/ldem_4.tif"
           ;"images/imageio/world.topo.bathy.200412.3x5400x2700.png"
           ]   #_(image/image-file-seq (io/file "images/moon"))
        ]
  (roundtrip input))
;;---------------------------------------------------------------------
