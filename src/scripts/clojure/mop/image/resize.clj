(set! *warn-on-reflection* true)
;;(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\image\resize.clj
;;----------------------------------------------------------------
(ns mop.image.resize
  {:doc
  "Resize image and save.
  Goal to get textures images small enough for
  <code>GL_MAX_TEXTURE_SIZE</code>"
   :author "palisades dot lakes at gmail dot com"
   :version "2025-11-21"}

  (:require
   [clojure.java.io :as io]
   [clojure.pprint :as pp]
   [mop.commons.debug :as debug]
   [mop.image.util :as image])
  (:import [java.awt Image]
           [java.awt.image BufferedImage]
           [javax.imageio ImageIO]))

;;-------------------------------------------------------------

(defn ^BufferedImage get-image
  ([path]
   (let [image (ImageIO/read (io/file path))]
     (pp/pprint path)
     (pp/pprint image)
     image))
  )

 ;;-------------------------------------------------------------

(defn resize-tif [^String filename]
  (let [folder "images/earth/"
        filetype "tif"
        ^BufferedImage image (get-image (str folder filename "." filetype))
        w (.getWidth image)
        h (.getHeight image)
        s (/ 16384 (Math/max w h))
        sw (* s w)
        sh (* s h)
        resized (image/resize image sw sh)]
    (assert (<= sw w))
    (assert (<= sh h))
    (debug/echo image w h (.getType image))
    (debug/echo s sw sh)
    (image/write-tif resized (io/file folder (str filename "-" sw "x" sh ".tif")))
    ))

(pp/pprint (vec (ImageIO/getWriterFormatNames)))

(resize-tif "ETOPO_2022_v1_60s_N90W180_bed")

(defn resize-png [^String filename]
  (let [folder "images/earth/"
        filetype "png"
        ^BufferedImage image (image/get-image (str folder filename "." filetype))
        w (.getWidth image)
        h (.getHeight image)
        s (/ 16384 (Math/max w h))
        sw (* s w)
        sh (* s h)
        resized (image/resize image sw sh)]
    (assert (<= sw w))
    (assert (<= sh h))
    (debug/echo image w h (.getType image))
    (debug/echo s sw sh)
    (image/write-png resized (io/file folder (str filename "-" sw "x" sh ".png")))
    ))
;
;(resize-png "world.topo.bathy.200412.3x21600x10800")
;(resize-png "gebco_08_rev_elev_21600x10800")
;(resize-png "gebco_08_rev_bath_21600x10800")


#_(let [folder "images/earth/"
      filename "world.topo.bathy.200412.3x21600x10800"
      filetype "png"
      ^BufferedImage image (image/get-image (str folder filename "." filetype))
      w (.getWidth image)
      h (.getHeight image)
      s (/ 3840 (Math/max w h))
      sw (* s w)
      sh (* s h)
      default (resize image sw sh Image/SCALE_DEFAULT)
      average (resize image sw sh Image/SCALE_AREA_AVERAGING)
      fast (resize image sw sh Image/SCALE_FAST)
      replicate (resize image sw sh Image/SCALE_REPLICATE)
      smooth (resize image sw sh Image/SCALE_SMOOTH)
      ]
  (assert (<= sw w))
  (assert (<= sh h))
  (debug/echo image w h (.getType image))
  (debug/echo s sw sh)
  ;(debug/echo default (.getWidth default) (.getHeight default) )
  (write-png default (io/file folder (str filename "-" sw "x" sh "-default" ".png")))
  (write-png average (io/file folder (str filename "-" sw "x" sh "-average" ".png")))
  (write-png fast (io/file folder (str filename "-" sw "x" sh "-fast" ".png")))
  (write-png replicate (io/file folder (str filename "-" sw "x" sh "-replicate" ".png")))
  (write-png smooth (io/file folder (str filename "-" sw "x" sh "-smooth" ".png")))
  ;(debug/echo average)
  ;(debug/echo fast)
  ;(debug/echo replicate)
  ;(debug/echo smooth)

  )