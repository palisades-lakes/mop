(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.image.util

  {:doc     "Image utilities."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-11-20"}

  (:require [clojure.java.io :as io])
  (:import [java.awt Image]
           [java.awt.image BufferedImage DataBufferByte WritableRaster]
           [java.io File]
           [java.nio ByteBuffer FloatBuffer IntBuffer]
           [javax.imageio ImageIO]
           [org.lwjgl BufferUtils]))
;;---------------------------------------------------------------

(defn download [url target]
  (with-open [in (io/input-stream url)
              out (io/output-stream target)]
    (print "Downloading" target "... ")
    (flush)
    (io/copy in out)
    (println "done")))

(defn ^BufferedImage get-image
  ([path]
   (ImageIO/read (io/file path)))
  ([path remote-url]
   (when (not (.exists (io/file path)))
     (download remote-url path))
   (get-image path)))

#_(defn ^WritableRaster get-writeable-raster [local-path remote-url]
    (when (not (.exists (io/file local-path)))
      (download remote-url local-path))
    (let [^BufferedImage image (ImageIO/read (io/file local-path)) ]
      (.getRaster image)))

;;-------------------------------------------------------------
;; TODO: some way to avoid creating intermediate image?

(defn ^BufferedImage to-buffered-image [^Image image ^long image-type]
  (if (instance? BufferedImage image)
    image
    ;; else
    (let [b (BufferedImage. (.getWidth image nil) (.getHeight image nil) image-type)
          g (.createGraphics b)]
      (.drawImage g image 0 0 nil)
      (.dispose g)
      b)))

;;-------------------------------------------------------------

(defn ^BufferedImage resize
  ([^BufferedImage image ^long w ^long h ^long hints]
   (to-buffered-image (.getScaledInstance image w h hints) (.getType image)))
  ([^BufferedImage image ^long w ^long h]
   (resize image w h Image/SCALE_DEFAULT)))

(defn ^Boolean write-png [^BufferedImage image ^File file ]
  (ImageIO/write image "png" file))

(defn ^Boolean write-tif [^BufferedImage image ^File file]
  (ImageIO/write image "tif" file))

;;-------------------------------------------------------------------

(defn ^FloatBuffer float-buffer [^floats data]
  (let [buffer (BufferUtils/createFloatBuffer (count data))]
    (.put buffer data)
    (.flip buffer)
    buffer))

(defn ^IntBuffer int-buffer [^ints data]
  (let [buffer (BufferUtils/createIntBuffer (count data))]
    (.put buffer data)
    (.flip buffer)
    buffer))

(defn ^ByteBuffer byte-buffer [^bytes data]
  (let [buffer (BufferUtils/createByteBuffer (count data))]
    (.put buffer data)
    (.flip buffer)
    buffer))

;;-------------------------------------------------------------------

(defn pixels-as-bytes
  ([^BufferedImage image]
   (let [w (.getWidth image)
         h (.getHeight image)
         pixels (.getData ^DataBufferByte (.getDataBuffer (.getRaster image)))]
     [(byte-buffer pixels) w h] ))
  ([^String local-path ^String remote-url]
   (pixels-as-bytes (get-image local-path remote-url))))

(defn pixels-as-ints
  ([^BufferedImage image]
   (let [w (.getWidth image)
         h (.getHeight image)
         d (.getNumComponents (.getColorModel image))
         pixels (int-array (* w h d))]
     (.getPixels (.getRaster image) 0 0 w h pixels)
     [(int-buffer pixels) w h] ))
  ([^String local-path ^String remote-url]
   (pixels-as-ints (get-image local-path remote-url))))

;; TODO: assuming only one band?
(defn pixels-as-floats
  ([^BufferedImage image]
   (let [w (.getWidth image)
         h (.getHeight image)
         pixels (float-array (* w h))]
     (.getPixels (.getRaster image) 0 0 w h pixels)
     [(float-buffer pixels) w h] ))
  ([^String local-path ^String remote-url]
   (pixels-as-floats (get-image local-path remote-url))))