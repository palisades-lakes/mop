(ns mop.test.imageio.roundtrip
  {:doc     "Image utilities related to Apache Commons Imaging."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-12-05"}
  (:require
   [clojure.test :as t]
   [mop.commons.debug :as debug]
   [mop.commons.io :as mci]
   [mop.image.imageio :as imageio]
   [mop.image.util :as image])
  (:import
   [javax.imageio ImageIO]))
;;---------------------------------------------------------------------
; TODO: check metadata, thumbnails, etc.

(defn- check-read-write-read [input]
  (println)
  (debug/echo input)
  (let [output (mci/append-to-filename input (str "-imageio"))
        [reader image] (imageio/read input)
        [_writer _writeParam] (imageio/write reader image output)
        image-in (.getRenderedImage image)
        image-out (ImageIO/read output)]
    #_(image/write-metadata-markdown input)
    #_(image/write-metadata-markdown output)
    (t/is (image/equals? image-in image-out))))
;;---------------------------------------------------------------------
(t/deftest read-write-read
  (t/testing
   (doseq [input [
                  "src/test/resources/images/ldem_4.tif"
                  "src/test/resources/images/ETOPO_2022_v1_60s_PNW_bed.tiff"
                  "src/test/resources/images/gebco_08_rev_elev_21600x10800.png"
                  ] ]
     (println input)
     (check-read-write-read input))))
;;---------------------------------------------------------------------
