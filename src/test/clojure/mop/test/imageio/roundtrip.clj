(ns mop.test.imageio.roundtrip
  {:doc     "Image utilities related to Apache Commons Imaging."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-12-06"}
  (:require
   [clojure.java.io :as io]
   [clojure.test :as t]
   [mop.commons.io :as mci]
   [mop.image.imageio :as imageio]
   [mop.image.util :as image])
  (:import [javax.imageio ImageIO]))
;;---------------------------------------------------------------------
; TODO: check metadata, thumbnails, etc.

(defn- check-simple-roundtrip [input]
  (println "simple " input)
  (image/write-metadata-markdown input)
  (let [output (mci/append-to-filename input "-siio")
        rendered-in (ImageIO/read (io/file input))
        _ (ImageIO/write rendered-in (mci/extension input) (io/file output))
        rendered-out  (ImageIO/read (io/file output))
        ]
    (image/write-metadata-markdown output)
    (t/is (image/equals? rendered-in rendered-out))))
;;---------------------------------------------------------------------
(defn- check-roundtrip [input]
  (println "roundtrip " input)
  (image/write-metadata-markdown input)
  (let [output (mci/append-to-filename input "-iio")
        [reader image] (imageio/read input)
        [_writer _writeParam] (imageio/write reader image output)
        rendered-in (.getRenderedImage image)
        [_reader image-out] (imageio/read output)
        rendered-out (.getRenderedImage image-out)]
    (image/write-metadata-markdown output)
    (t/is (image/equals? rendered-in rendered-out))))
;;---------------------------------------------------------------------
(t/deftest roundtrip
  (t/testing
   (doseq [input [#_"src/test/resources/images/USGS_13_n38w077_dir5.tiff"
                  #_"src/test/resources/images/ETOPO_2022_v1_60s_N90W180_bed.tif"
                  "src/test/resources/images/ETOPO_2022_v1_60s_PNW_bed.tiff"
                  "src/test/resources/images/ldem_4.tif"
                  "src/test/resources/images/gebco_08_rev_elev_21600x10800.png"
                  ] ]
     (check-simple-roundtrip input)
     (check-roundtrip input))))
;;---------------------------------------------------------------------
