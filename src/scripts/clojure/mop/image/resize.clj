(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\image\resize.clj
;;----------------------------------------------------------------
(ns mop.image.resize
  {:doc
   "Resize image and save.
   Goal to get textures images small enough for
   <code>GL_MAX_TEXTURE_SIZE</code>."
   :author "palisades dot lakes at gmail dot com"
   :version "2025-11-22"}

  (:require
   [clojure.java.io :as io]
   [clojure.pprint :as pp]
   [mop.commons.debug :as debug]
   [mop.commons.io :as mci])
  (:import [java.awt Graphics2D RenderingHints Transparency]
           [java.awt.image BufferedImage]
           [java.io File]
           [javax.imageio ImageIO]
           [org.apache.commons.imaging ImageFormat Imaging]))

;;-------------------------------------------------------------

(defn ^BufferedImage read-image

  ([^File file] (Imaging/getBufferedImage file))

  #_([^File file] (ImageIO/read file))
  )


(defn ^Boolean write-image [^BufferedImage image ^ImageFormat format ^File file]
  (Imaging/writeImage image file format)
  )

;;-------------------------------------------------------------
;; see https://web.archive.org/web/20070515094604/https://today.java.net/pub/a/today/2007/04/03/perils-of-image-getscaledinstance.html

(defn ^BufferedImage resize-image

  "Resize <code>image</code>.
  <dl>
  <dt>targetWidth</dt><dd>resized width</dd>
  <dt>targetHeight</dt><dd>resized height</dd>
  <dt>hint</dt>\n<dd> value for RenderingHints.KEY_INTERPOLATION:\n<ul>
  <li>RenderingHints/VALUE_INTERPOLATION_NEAREST_NEIGHBOR
  <li>RenderingHints/VALUE_INTERPOLATION_BILINEAR
  <li>RenderingHints/VALUE_INTERPOLATION_BICUBIC
  </ul>
  <dt>higherQuality<</dt>
  <dd> if true, this method will use a multi-step
  scaling technique that provides higher quality than the usual
  one-step technique (only useful in downscaling cases, where
  <code>w</code> or <code>h</code> is
  smaller than the original dimensions, and generally only when
  the BILINEAR hint is specified)
  </dd>
  </dl>
  <br>
  Return a resized version of the original image.
  "

  ([^BufferedImage image
    ^Long targetWidth
    ^Long targetHeight
    ^Object hint
    ^Boolean higherQuality]
   (debug/echo image)
   ;; TODO: get type from original image?
   ;; TODO preserve aspect ratio?
   (let [type (.getType image)
         #_(if (== (.getTransparency image) Transparency/OPAQUE)
             BufferedImage/TYPE_INT_RGB
             BufferedImage/TYPE_INT_ARGB)
         ]
     (loop [^BufferedImage ret image
            ;; TODO: is this really a good idea?
            ;; If higherQuality
            ;; Use multistep technique: start with original size, then
            ;; scale down in multiple passes with drawImage()
            ;; until the target size is reached
            ;; else
            ;; Use one-step technique: scale directly from original
            ;; size to target size with a single drawImage() call
             w (if higherQuality (.getWidth image) targetWidth)
             h (if higherQuality (.getHeight image) targetHeight)]
       (let [w (int w)
             h (int h)
             targetWidth (int targetWidth)
             targetHeight (int targetHeight)
             ^int w (when (and higherQuality (>  w targetWidth)) (quot w 2))
             w (if (< w targetWidth) targetWidth w)
             ^int h (when (and higherQuality (> h targetHeight)) (quot h 2))
             h (if (< h targetHeight) targetHeight h)
             ^BufferedImage tmp (BufferedImage. w h type)
             ^Graphics2D g2 (.createGraphics tmp)]
         (pp/pprint [w h])
         (.setRenderingHint g2 RenderingHints/KEY_INTERPOLATION hint)
         (.drawImage g2 ret 0 0 w h nil)
         (.dispose g2)
         (if (and (== w targetWidth) (== h targetHeight))
           ret
           (recur tmp w h))
         ))))

  ([^BufferedImage image ^long w ^long h ^Object hint]
   (resize-image image w h hint true))

  ([^BufferedImage image ^long w ^long h]
   (resize-image image w h RenderingHints/VALUE_INTERPOLATION_BILINEAR)))

(defn resize [source
              ^Integer max-dimension]
  (println)
  (debug/echo source)
  (let [file (io/file source)
        folder (.getParentFile file)
        filename (mci/prefix file)
        extension (mci/extension file)
        ^BufferedImage image (read-image file)
        w (.getWidth image)
        h (.getHeight image)
        s (/ (double max-dimension) (Math/max w h))
        sw (long (* s w))
        sh (long (* s h))
        resized (resize-image image sw sh)
        outfile (io/file folder (str filename "-" sw "x" sh "." extension)) ]
    (debug/echo image)
    (debug/echo resized)
    (assert (<= sw w))
    (assert (<= sh h))
    (write-image resized (Imaging/guessFormat file) outfile)
    ))

;;-------------------------------------------------------------
(doall
 (map #(resize % 16384)
      [
       ;"images/earth/eo_base_2020_clean_geo.tif"
       ;"images/earth/ETOPO_2022_v1_60s_N90W180_bed.tif"
       ;"images/earth/ETOPO_2022_v1_60s_N90W180_geoid.tif"
       ;"images/earth/ETOPO_2022_v1_60s_N90W180_surface.tif"
       "images/earth/gebco_08_rev_bath_21600x10800.png"
       "images/earth/gebco_08_rev_elev_21600x10800.png"
       ;"images/earth/world.topo.bathy.200412.3x21600x10800.png"
       ;"images/earth/world.200412.3x21600x10800.png"
       ]))