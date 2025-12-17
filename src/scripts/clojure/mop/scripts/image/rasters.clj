(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\scripts\image\rasters.clj
;;----------------------------------------------------------------
(ns mop.scripts.image.rasters
  {:doc
   "Prepare to Resize images."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-12-14"}

  (:require
   [clojure.java.io :as io]
   [clojure.pprint :as pp]
   [mop.commons.io :as mci]
   [mop.image.imageio :as imageio]
   [mop.image.util :as image])
  (:import
   [java.awt.image DataBuffer Raster SampleModel]
   [javax.imageio IIOImage]
   [mop.java.imageio MaxDimensionOp]))
;;----------------------------------------------------------------------
(defn ^String data-buffer-type-name [^long code]
  (cond
    (== code DataBuffer/TYPE_BYTE) "BYTE"
    (== code DataBuffer/TYPE_USHORT) "USHORT"
    (== code DataBuffer/TYPE_SHORT) "SHORT"
    (== code DataBuffer/TYPE_INT) "INT"
    (== code DataBuffer/TYPE_FLOAT) "FLOAT"
    (== code DataBuffer/TYPE_DOUBLE) "DOUBLE"
    (== code DataBuffer/TYPE_UNDEFINED) "UNDEFINED"
    :else
    (throw (IllegalArgumentException.
            (str "Unrecognized DataBuffer data type code: " code)))))
;;----------------------------------------------------------------------
(defn ^String buffer-type [^DataBuffer buffer]
  (data-buffer-type-name (.getDataType buffer)))
(defn ^String data-type [^SampleModel sm]
  (data-buffer-type-name (.getDataType sm)))
(defn ^String transfer-type [^SampleModel sm]
  (data-buffer-type-name (.getTransferType sm)))
;;----------------------------------------------------------------------
(defn ^Raster get-raster [^IIOImage image]
  (if (.hasRaster image)
    (.getRaster image)
    (.getData (.getRenderedImage image))))
;;----------------------------------------------------------------------
(defn examine [^Raster raster]
  (println)
  (pp/pprint raster)
  (print "data buffer= " )
  (pp/pprint (.getDataBuffer raster))
  (println "buffer type=" (buffer-type (.getDataBuffer raster)))
  (println "num bands=" (.getNumBands raster))
  (print "sample model= " )
  (pp/pprint (.getSampleModel raster))
  (println "data type=" (data-type (.getSampleModel raster)))
  (println "transfer type="  (transfer-type (.getSampleModel raster)))
  )
;;----------------------------------------------------------------------
(defn rasters [input]
  (println)
  (println input)
  (let [[_reader ^IIOImage image] (imageio/read input)
        raster (get-raster image)
        subsample (MaxDimensionOp/make 4096)]
    (with-open [w (io/writer (mci/replace-extension input "rstr"))]
      (binding [*out* w]
        (examine raster)
        (examine (.createCompatibleDestRaster subsample raster))))))
;;---------------------------------------------------------------------
(doseq [input
        (image/image-file-seq (io/file "images/imageio-ext"))
        #_[
         "images/lroc/lroc_color_poles_2k.tif"
         "images/lroc/lroc_color_poles_2k-gtx.tif"
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
         ]   #_(reverse (image/image-file-seq (io/file "images/moon")))
        ]
  (rasters input))
;;---------------------------------------------------------------------
