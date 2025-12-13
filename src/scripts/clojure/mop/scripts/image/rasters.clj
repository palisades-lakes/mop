(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\scripts\image\rasters.clj > rasters.txt
;;----------------------------------------------------------------
(ns mop.scripts.image.rasters
  {:doc
   "Prepare to Resize images."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-12-12"}

  (:require
   [clojure.pprint :as pp]
   [mop.image.imageio :as imageio])
  (:import
   [java.awt.image DataBuffer Raster SampleModel]
   [javax.imageio IIOImage]
   [mop.java.imageio SubSampleOp]))
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
  (pp/pprint raster)
  (pp/pprint (.getDataBuffer raster))
  (println "buffer type=" (buffer-type (.getDataBuffer raster)))
  (println "num bands=" (.getNumBands raster))
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
        subsample (SubSampleOp/make (/ (.getWidth raster) 256.0))]
    (examine raster)
    (examine (.createCompatibleDestRaster subsample raster))
    ))
;;---------------------------------------------------------------------
(doseq [input
        [
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
  (rasters input))
;;---------------------------------------------------------------------
