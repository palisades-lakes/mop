(ns mop.image.util

  {:doc     "Image utilities."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-10-07"}

  (:require [clojure.java.io :as io])
  (:import [java.awt.image BufferedImage WritableRaster]
           [javax.imageio ImageIO]))

(defn download [url target]
  (with-open [in (io/input-stream url)
              out (io/output-stream target)]
    (print "Downloading" target "... ")
    (flush)
    (io/copy in out)
    (println "done")))

(defn ^WritableRaster get-writeable-raster [local-path remote-url]
  (when (not (.exists (io/file local-path)))
    (download remote-url local-path))
  (let [^BufferedImage image (ImageIO/read (io/file local-path)) ]
     (.getRaster image)))

(defn pixels-as-ints [^WritableRaster raster]
  (let [w (.getWidth raster)
        h (.getHeight raster)
        d (.getNumBands raster)
        pixels (int-array (* w h d))]
    (.getPixels raster 0 0 w h pixels)
    [pixels w h] ))

;; TODO: assuming only one band?
(defn pixels-as-floats [^WritableRaster raster]
  (let [w (.getWidth raster)
        h (.getHeight raster)
        pixels (float-array (* w h))]
    (.getPixels raster 0 0 w h pixels)
    [pixels w h] ))