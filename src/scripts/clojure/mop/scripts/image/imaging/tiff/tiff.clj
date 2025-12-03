(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\image\tiff.clj
;;----------------------------------------------------------------
(ns mop.scripts.image.imaging.tiff.tiff
  {:doc
   "Experiment with tiffs."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-12-02"}

  (:require
   [clojure.java.io :as io]
   [mop.commons.debug :as debug])
  (:import [java.awt.image BufferedImage]
           [javax.imageio ImageIO]
           [org.apache.commons.imaging Imaging]))

;;-------------------------------------------------------------

(defn read2 [path]
  (let [file (io/file path)
        ^BufferedImage imageio (ImageIO/read file)
        ^BufferedImage imaging (Imaging/getBufferedImage file)]
    (debug/echo imageio)
    (ImageIO/write imageio "TIF" (io/file "images/imageio.tif"))
    (debug/echo imaging)
    (Imaging/writeImage imaging (io/file "images/imaging.tif") (Imaging/guessFormat file))))

(read2 "images/ldem_4.tif")



;;-------------------------------------------------------------
