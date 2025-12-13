(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\scripts\image\resize.clj
;;----------------------------------------------------------------
(ns mop.scripts.image.resize
  {:doc
   "Resize images."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-12-12"}

  (:require
   [mop.commons.debug :as debug]
   [mop.commons.io :as mci]
   [mop.image.imageio :as imageio]
   [mop.image.util :as image])
  (:import
   [java.awt RenderingHints]
   [java.awt.geom AffineTransform]
   [java.awt.image AffineTransformOp Raster]
   [javax.imageio IIOImage]))
;;----------------------------------------------------------------------
(defn ^Raster raster [^IIOImage image]
  (if (.hasRaster image)
    (.getRaster image)
    (.getData (.getRenderedImage image))))
;;----------------------------------------------------------------------
(defn ^IIOImage rescale-iioimage

  ([^IIOImage image ^double s ^RenderingHints hints]
   (let [r-in (raster image)
         a (AffineTransform/getScaleInstance s s)
         op (AffineTransformOp. a hints)
         r-out (.filter op r-in nil)]
     (debug/echo r-out)
     (IIOImage. r-out (.getThumbnails image) (.getMetadata image))))

  ([^IIOImage image ^double s]
   (rescale-iioimage
    image s
    (RenderingHints.
     RenderingHints/KEY_INTERPOLATION
     RenderingHints/VALUE_INTERPOLATION_BILINEAR))))

;;----------------------------------------------------------------------
(defn reduce-iioimage

  "Return an image that has no dimension larger that max-dimension,
  maintaining the aspect ratio.
  If small enough, return nil."

  (^IIOImage [^IIOImage image ^long max-dimension ^RenderingHints hints]
   (let [r (raster image)
         w (.getWidth r)
         h (.getHeight r)
         s (/ (double max-dimension) (Math/max w h))]
     (when (< s 1.0)
       (rescale-iioimage image s hints))))

  (^IIOImage [^IIOImage image ^long max-dimension]
   (reduce-iioimage
    image
    max-dimension
    (RenderingHints.
     RenderingHints/KEY_INTERPOLATION
     RenderingHints/VALUE_INTERPOLATION_BILINEAR))))
;;---------------------------------------------------------------------
(defn wh [^IIOImage image]
  (if (.hasRaster image)
    [(.getWidth (.getRaster image)) (.getHeight (.getRaster image))]
    [(.getWidth (.getRenderedImage image)) (.getHeight (.getRenderedImage image))]))
;;---------------------------------------------------------------------
(defn resize [input]
  (println input)
  (image/write-metadata-markdown input)
  (let [max-dim 256
        [reader ^IIOImage image] (imageio/read input)
        resized (reduce-iioimage image max-dim)]
    (when resized
      (let [[w h] (wh resized)
            output (mci/append-to-filename input (str "-" w "x" h))]
        (imageio/write reader resized output)
        (image/write-metadata-markdown output)))))
;;---------------------------------------------------------------------
(doseq [input
        [
         ;"images/imageio/eo_base_2020_clean_geo.tif"
         ;"images/imageio/gebco_08_rev_elev_21600x10800.png"
         ;"images/imageio/world.topo.bathy.200412.3x5400x2700.png"

         "images/imageio/USGS_13_n38w077_dir5.tiff"

         ;;"images/imageio/ETOPO_2022_v1_60s_PNW_bed.tiff"
         ;;"images/imageio/ETOPO_2022_v1_60s_N90W180_bed.tif"
         ;;"images/imageio/ldem_4.tif"
         ;;"images/imageio/ETOPO_2022_v1_60s_N90W180_bed-lzw.tif"
         ]
        ]
  (resize input))
;;---------------------------------------------------------------------
