(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\scripts\image\geotools\simple.clj
;;----------------------------------------------------------------
(ns mop.scripts.image.geotools.simple
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
   [javax.imageio ImageIO]
   [org.geotools.image.io ImageIOExt]))
;---------------------------------------------------------------------
(def suffix "-gts" )
(defn roundtrip [input]
  (println)
  (debug/echo input)
  #_(image/write-metadata-markdown input)
  (let [output (mci/append-to-filename input suffix)
        rendered-in (ImageIOExt/read (io/file input))
        _ (ImageIO/write rendered-in (mci/extension input) (io/file output))
        rendered-out  (ImageIO/read (io/file output))
        ]
    #_(image/write-metadata-markdown output)
    (assert (image/equals? rendered-in rendered-out))))
;;---------------------------------------------------------------------
(doseq [input [
               "images/geotools/eo_base_2020_clean_geo.tif"
               "images/geotools/ETOPO_2022_v1_60s_N90W180_bed.tif"
               "images/geotools/ETOPO_2022_v1_60s_PNW_bed.tiff"
               "images/geotools/gebco_08_rev_elev_21600x10800.png"
               "images/geotools/ldem_4.tif"
               "images/geotools/USGS_13_n38w077_dir5.tiff"
               "images/geotools/world.topo.bathy.200412.3x5400x2700.png"
               ]]
  (roundtrip input))