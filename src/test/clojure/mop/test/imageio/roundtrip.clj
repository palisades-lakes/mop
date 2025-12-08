(ns mop.test.imageio.roundtrip
  {:doc     "Image utilities related to Apache Commons Imaging."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-12-08"}
  (:require
   [clojure.java.io :as io]
   [clojure.test :as t]
   [mop.commons.io :as mci]
   [mop.image.imageio :as imageio]
   [mop.image.util :as image])
  (:import [javax.imageio ImageIO]))
;;---------------------------------------------------------------------
(defn- check-simple-roundtrip [input]
  (let [output (mci/append-to-filename input "-siio")
        rendered-in (ImageIO/read (io/file input))]
    (try
      (ImageIO/write rendered-in (mci/extension input) (io/file output))
      (t/is (image/equals? rendered-in  (ImageIO/read (io/file output))))
      (io/delete-file output false)
      (catch Throwable t
        (io/delete-file output true)
        (throw t)))))
;;---------------------------------------------------------------------
(defn- check-roundtrip [input]
  (let [output (mci/append-to-filename input "-iio")
        [reader image-in] (imageio/read input)]
    (try
      (imageio/write reader image-in output)
      (let [[_reader image-out] (imageio/read output) ]
        (t/is (image/equals? image-in image-out)))
      (io/delete-file output false)
      (catch Throwable t
        (io/delete-file output true)
        (throw t)))))
;;---------------------------------------------------------------------
(t/deftest roundtrip
  (t/testing
   (doseq [input ["src/test/resources/images/USGS_13_n38w077_dir5.tiff"
                  ;;"src/test/resources/images/ETOPO_2022_v1_60s_N90W180_bed.tif"
                  "src/test/resources/images/ETOPO_2022_v1_60s_PNW_bed.tiff"
                  "src/test/resources/images/ldem_4.tif"
                  "src/test/resources/images/gebco_08_rev_elev_21600x10800.png"
                  ] ]
     (check-simple-roundtrip input)
     (check-roundtrip input))))
;;---------------------------------------------------------------------
