(ns mop.test.imageio.roundtrip
  {:doc     "Image utilities related to Apache Commons Imaging."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-12-18"}
  (:require
   [clojure.java.io :as io]
   [clojure.test :as t]
   [mop.commons.io :as mci]
   [mop.image.imageiox :as imagiox]
   [mop.image.util :as image])
  (:import
   [javax.imageio IIOImage ImageIO ImageReader]))
;;---------------------------------------------------------------------
(defn- check-simple-roundtrip [input]
  (let [output (mci/append-to-filename input "-siio")
        rendered-in (ImageIO/read (io/file input))]
    (try
      (ImageIO/write rendered-in (mci/extension input) (io/file output))
      (image/equals? rendered-in  (ImageIO/read (io/file output)))
      (io/delete-file output false)
      (finally #_(io/delete-file output true)
        ))))
;;---------------------------------------------------------------------
(defn- check-roundtrip [input]
  (let [output (mci/append-to-filename input "-iio")
        [reader image-in] (imagiox/read input)]
    (try
      (imagiox/write reader image-in output)
      (let [[_reader image-out] (imagiox/read output)]
        (image/equals? image-in image-out))
      (finally (io/delete-file output true)))))
;;---------------------------------------------------------------------
(t/deftest roundtrip
  (t/testing "read-write-read roundtrips produce identical images, if not identical files."
    (doseq [input ["src/test/resources/images/USGS_13_n38w077_dir5.tiff"
                   "src/test/resources/images/ETOPO_2022_v1_60s_PNW_bed.tiff"
                   "src/test/resources/images/ldem_4.tif"
                   "src/test/resources/images/gebco_08_rev_elev_21600x10800.png"
                   ] ]
      (t/is (check-simple-roundtrip input))
      (t/is (check-roundtrip input)))))
;;----------------------------------------------------------------------
(defn- write-without-zlib-workaround [^ImageReader reader ^IIOImage image output]
  (let [output (io/file output)
        writer (ImageIO/getImageWriter reader)
        write-param (.getDefaultWriteParam writer);
        metadata (.getMetadata image)]
    (try
      (with-open [ios (ImageIO/createImageOutputStream output)]
        (.setOutput writer ios)
        (.write writer metadata image write-param))
      (finally
        (.dispose writer)))))
;;;---------------------------------------------------------------------
(defn- check-roundtrip-without-zlib-workaround [input]
  (let [output (mci/append-to-filename input "-iio")
        [reader image-in] (imagiox/read input)]
    (try
      (write-without-zlib-workaround reader image-in output)
      (let [[_ image-out] (imagiox/read output) ]
        (image/equals? image-in image-out))
      (finally
        (.dispose reader)
        (io/delete-file output true)))))
;;---------------------------------------------------------------------
;; dummy fn to workaround intellij warnings
(defn- thrown? [& _args])
;;---------------------------------------------------------------------
;; Test for fixed imageio/imageio-ext problems in future releases,
;; so we can eliminate workarounds.

(t/deftest roundtrip-expected-failures
  (t/testing "Are imageio-ext workarounds still needed?"
    ;; TODO: better way to handle test files too large to include in test jar
    (doseq [input [
                   "images/imageio/ETOPO_2022_v1_60s_N90W180_bed.tif"
                   ] ]
      (let [input (io/file input)]
        (when (.canRead input)
          (t/is (thrown?
                 NullPointerException
                 (check-roundtrip-without-zlib-workaround input))))))))
;;---------------------------------------------------------------------
