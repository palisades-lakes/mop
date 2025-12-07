(ns mop.test.imaging.tiff
  {:doc     "Image utilities related to Apache Commons Imaging."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-12-06"}
  (:require [clojure.java.io :as io]
            [clojure.test :as t]
            [mop.commons.io :as mci]
            [mop.image.imaging :as imaging]
            [mop.image.util :as image])
  (:import [java.nio ByteOrder]
           [java.util Arrays]
           [org.apache.commons.imaging ImageInfo Imaging]
           [org.apache.commons.imaging.formats.tiff
            AbstractTiffRasterData TiffImageMetadata]))
;;---------------------------------------------------------------------
;; TODO: add image files of differing contents
;; TODO: replace TIFF F32 specific IO with generic, adaptive.

#_(defn- check-read-write-read-tiff-f32 [input]
  (image/write-metadata-markdown input)
  (let [input (io/file input)
        output (mci/append-to-filename input "-imaging")
        {^AbstractTiffRasterData raster-in :raster
         ^ByteOrder byteOrder-in           :byteOrder
         ^TiffImageMetadata metadata-in    :metadata} (imaging/read-tiff-f32 input)
        w (.getWidth raster-in)
        h (.getHeight raster-in)
        ;nRowsInBlock (int 1)
        ;nColsInBlock w
        ^floats pixels-in (.getData raster-in)
        ^ImageInfo info-in (Imaging/getImageInfo input)]
    (imaging/write-tiff-f32-strips pixels-in w h byteOrder-in metadata-in output)
    (image/write-metadata-markdown output)
    (let [{^AbstractTiffRasterData raster-out :raster
           ^ByteOrder byteOrder-out           :byteOrder
           ^TiffImageMetadata metadata-out    :metadata}
          (imaging/read-tiff-f32 output)
          ^floats pixels-out (.getData raster-out)
          ^ImageInfo info-out (Imaging/getImageInfo output)]
      (t/is (= byteOrder-in byteOrder-out))
      (t/is (Arrays/equals pixels-in pixels-out))
      (t/is (imaging/equal-ImageInfo? info-in info-out))
      (t/is (imaging/equal-ImageMetadata? metadata-in metadata-out)))))
;;---------------------------------------------------------------------

#_(t/deftest read-write-read-tiff-f32
  (t/testing
   (doseq [input [
                  "src/test/resources/images/ldem_4.tif"
                  "src/test/resources/images/USGS_13_n38w077_dir5.tiff"
                  "src/test/resources/images/ETOPO_2022_v1_60s_PNW_bed.tiff"
                  ] ]
     (println input)
     (check-read-write-read-tiff-f32 input))))

;;---------------------------------------------------------------------
