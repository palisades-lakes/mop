(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\scripts\image\imageio\readWrite.clj
;;----------------------------------------------------------------
(ns mop.scripts.image.imageio.readWrite
  {:doc
   "Work out idempotent image read-write roundtrips, for at least NASA and NOAA tiffs and pngs."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-12-05"}

  (:require
   [clojure.java.io :as io]
   [mop.commons.debug :as debug]
   [mop.commons.io :as mci]
   [mop.commons.string :as mcs]
   [mop.image.util :as image])
  (:import
   [java.awt.image
    BufferedImage DataBuffer DataBufferByte DataBufferDouble DataBufferFloat DataBufferInt
    DataBufferShort DataBufferUShort Raster]
   [java.util Arrays]
   [javax.imageio IIOImage ImageIO ImageReader]
   [javax.imageio.metadata IIOMetadata]
   [mop.java.imageio Metadata]))
;;-------------------------------------------------------------
;; TODO: equals? defmulti?
(defn assert-equal-buffers? [^DataBuffer a ^DataBuffer b]
  (assert (= (class a) (class b)))
  (cond (instance? DataBufferByte a)
        (assert (Arrays/equals (.getData ^DataBufferByte a) (.getData ^DataBufferByte b)))
        (instance? DataBufferFloat a)
        (assert (Arrays/equals (.getData ^DataBufferFloat a) (.getData ^DataBufferFloat b)))
        (instance? DataBufferInt a)
        (assert (Arrays/equals (.getData ^DataBufferInt a) (.getData ^DataBufferInt b)))
        (instance? DataBufferShort a)
        (assert (Arrays/equals (.getData ^DataBufferShort a) (.getData ^DataBufferShort b)))
        (instance? DataBufferUShort a)
        (assert (Arrays/equals (.getData ^DataBufferUShort a) (.getData ^DataBufferUShort b)))
        (instance? DataBufferDouble a)
        (assert (Arrays/equals (.getData ^DataBufferDouble a) (.getData ^DataBufferDouble b)))
        :else
        (assert false)))

(defn assert-equal-rasters? [^Raster a ^Raster b]
  (assert (== (.getWidth a) (.getWidth b)))
  (assert (== (.getHeight a) (.getHeight b)))
  (assert-equal-buffers? (.getDataBuffer a) (.getDataBuffer b)))
;;;-------------------------------------------------------------
(defn assert-equal-images? [^BufferedImage a ^BufferedImage b]
  (assert-equal-rasters? (.getData a) (.getData b)))
;;-------------------------------------------------------------
(defn roundtrip [input]
  (println)
  (debug/echo input)
  (let [input (io/file input)
        ;extension (mci/extension input)
        inputStream (ImageIO/createImageInputStream input)
        readers (iterator-seq (ImageIO/getImageReaders inputStream))
        ;output (mci/append-to-filename input "-imageio")
        ;_ (image/write-metadata-markdown input)
        ;image-in (ImageIO/read input)
       ; _ (debug/echo (image/buffered-image-type-name image-in))
        ;_ (debug/echo (image/raster-data-type-name (.getRaster image-in)))
        ;_ (ImageIO/write image-in extension output)
        ;_ (image/write-metadata-markdown output)
        ;image-out (ImageIO/read output)
        ]
    (doseq [^ImageReader reader readers]
      (.setInput reader inputStream)
      #_(debug/echo reader)
      (let [ ^IIOImage iio-image (.readAll reader 0 nil)
            ^IIOMetadata metadata (.getMetadata iio-image)
            ^"[Ljava.lang.String;" meta-formats (.getMetadataFormatNames metadata)]
        #_(debug/echo (mcs/description reader))
        #_(debug/echo (mcs/description (.getDefaultReadParam reader)))
        #_(debug/echo (mcs/description iio-image))
        (dotimes [i (alength meta-formats)]
          (let [^String format (aget meta-formats i)]
            (println)
            (debug/echo format)
            (Metadata/print (.getAsTree metadata format))))))))

#_(assert-equal-images? image-in image-out)

;-------------------------------------------------------------
#_ [
    ;"images/imageio/ETOPO_2022_v1_60s_PNW_bed.tiff"
    "images/imageio/USGS_13_n38w077_dir5.tiff"
    ;"images/imageio/ldem_4.tif"
    ]
(doseq [input
        (take 1
              (remove #(or (.startsWith (mci/prefix %) "USGS")
                           (.endsWith (mci/prefix %) "-imageio"))
                      (image/image-file-seq (io/file "images/imageio")))
              )
        ]
  (roundtrip input))
;;-------------------------------------------------------------
