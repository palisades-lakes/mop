(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\scripts\image\lookup.clj > lookup.txt
;;----------------------------------------------------------------
(ns mop.scripts.image.lookup
  {:doc
   "Explore image metadata."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-12-13"}
  (:require
   [clojure.java.io :as io]
   [clojure.pprint :as pp]
   [mop.commons.debug :as debug]
   [mop.image.util :as image])
  (:import
   [com.sun.imageio.plugins.png PNGMetadata]
   [it.geosolutions.imageio.plugins.tiff BaselineTIFFTagSet]
   [it.geosolutions.imageioimpl.plugins.tiff TIFFFieldNode TIFFImageMetadata]
   [java.util Arrays]
   [javax.imageio ImageIO ImageReader]
   [javax.imageio.metadata IIOMetadata IIOMetadataNode]
   [org.w3c.dom Attr NamedNodeMap Node NodeList]))
;;---------------------------------------------------------------------
(defmulti to-clj class)

(defmethod to-clj NamedNodeMap [^NamedNodeMap nnm]
  (let [result (transient [])]
    (dotimes [i (.getLength nnm)]
      (conj! result (to-clj (.item nnm i))))
    (persistent! result)))

(defmethod to-clj NodeList [^NodeList nl]
  (let [result (transient [])]
    (dotimes [i (.getLength nl)]
      (conj! result (to-clj (.item nl i))))
    (persistent! result)))

(defn- attributes-clj [^Node node]
  (let [attributes (.getAttributes node)
        result (transient {})]
    (dotimes [i (.getLength attributes)]
      (let [^Attr item (.item attributes i)]
        (assoc! result (keyword (.getName item)) (.getValue item))))
    (persistent! result)))

(defn- children-clj [^Node node]
  (let [children (.getChildNodes node)
        items (transient [])]
    (dotimes [i (.getLength children)]
      (conj! items (to-clj (.item children i))))
    (persistent! items)))

;; Does this always represent a short?
(defn ^Integer tiff-short-clj [^Node node]
  (assert (= :TIFFShort (keyword (.getNodeName node))))
  (let [attributes (attributes-clj node)]
    (Integer/parseInt (:value attributes))))

;; Does this represent a short[]?
(defn tiff-shorts-clj [^Node node]
  (assert (= :TIFFShorts (keyword (.getNodeName node))))
  (println (children-clj node))
  (children-clj node))

(defmethod to-clj Node [^Node node]
  (cond
    (= :TIFFShort (keyword (.getNodeName node)))
    (tiff-short-clj node)
    (= :TIFFShorts (keyword (.getNodeName node)))
    (tiff-shorts-clj node)
    :default
    (let [attributes (attributes-clj node)
          name (keyword (.getNodeName node))
          value (.getNodeValue node)
          initial (if value {name value} {:name name})
          initial (merge initial attributes)
          result (transient initial)
          children (children-clj node)]
      (assoc! result :class (class node))
      (when (< 0 (count children))
        (assoc! result :children children))
      (persistent! result))))

(defmethod to-clj Attr [^Attr node]
  {(keyword (.getName node)) (.getValue node)})

(defmethod to-clj TIFFFieldNode [^TIFFFieldNode node]
  (let [result (transient (attributes-clj node))
        children (children-clj node)]
    (when (< 0 (count children))
      (assoc! result :children children))
    (persistent! result)))

(prefer-method to-clj Node NodeList)

#_(defmethod to-clj IIOMetadataNode [^IIOMetadataNode n]
    (let [attributes (.getAttributes n)
          name (keyword (.getNodeName n))
          value (.getNodeValue n)
          initial (if value {name value} {:name name})
          result (transient initial)]
      (when (< 0 (.getLength attributes))
        (assoc! result :attributes (to-clj attributes)))
      #_(when (< 0 (.getLength n))
          (assoc! result :children  (let [items (transient [])]
                                      (dotimes [i (.getLength n)]
                                        (conj! items (to-clj (.item n i))))
                                      (persistent! items))))
      ;:children (to-clj (.getChildNodes n))
      (persistent! result)))
;;---------------------------------------------------------------------
(defn node-lookup [^IIOMetadataNode node ^String tag]
  (println)
  (debug/echo node)
  (debug/echo (.hasAttributes node))
  (debug/echo (.hasAttribute node tag))
  (debug/echo (.getAttribute node tag))
  (debug/echo (.getAttributeNode node tag))
  (debug/echo (.getElementsByTagName node tag)))
;;---------------------------------------------------------------------
(defmulti metadata-lookup class )

(defmethod metadata-lookup IIOMetadata [^IIOMetadata metadata]
  (let [format-names (seq (Arrays/asList (.getMetadataFormatNames metadata)))
        extra-format-names (.getExtraMetadataFormatNames metadata)
        extra-format-names (if extra-format-names (Arrays/asList extra-format-names) [])
        format-names (concat format-names extra-format-names)
        roots (map #(.getAsTree metadata ^String %) format-names)]
    (doall (map #(node-lookup % "Color Type") roots))))

;;---------------------------------------------------------------------

(defn- tiff-predictor-name ^String [^long code]
  (cond
    (== code BaselineTIFFTagSet/PREDICTOR_NONE) "none"
    (== code BaselineTIFFTagSet/PREDICTOR_HORIZONTAL_DIFFERENCING) "HORIZONTAL_DIFFERENCING"
    (== code BaselineTIFFTagSet/PREDICTOR_FLOATING_POINT) "FLOATING_POINT"
    :else
    (throw (IllegalArgumentException.
            (str "Unrecognized predictor code: " code)))))

(defn- tiff-compression-name ^String [^long code]
  (cond
    (== code BaselineTIFFTagSet/COMPRESSION_NONE) "none"
    (== code BaselineTIFFTagSet/COMPRESSION_CCITT_RLE) "CCITT_RLE"
    (== code BaselineTIFFTagSet/COMPRESSION_CCITT_T_4) "CCITT_T_4"
    (== code BaselineTIFFTagSet/COMPRESSION_CCITT_T_6) "CCITT_T_6"
    (== code BaselineTIFFTagSet/COMPRESSION_LZW) "LZW"
    (== code BaselineTIFFTagSet/COMPRESSION_OLD_JPEG) "OLD_JPEG"
    (== code BaselineTIFFTagSet/COMPRESSION_JPEG) "JPEG"
    (== code BaselineTIFFTagSet/COMPRESSION_ZLIB) "ZLIB"
    (== code BaselineTIFFTagSet/COMPRESSION_PACKBITS) "PACKBITS"
    (== code BaselineTIFFTagSet/COMPRESSION_DEFLATE)"DEFLATE"
    :else
    (throw (IllegalArgumentException.
            (str "Unrecognized compression code: " code)))))

(defmethod metadata-lookup TIFFImageMetadata [^TIFFImageMetadata metadata]
  (let [rootIFD (.getRootIFD metadata)
        predictorField (.getTIFFField rootIFD BaselineTIFFTagSet/TAG_PREDICTOR)
        predictor (if predictorField
                    (.getAsLong predictorField 0)
                    BaselineTIFFTagSet/PREDICTOR_NONE)
        compressionField (.getTIFFField rootIFD BaselineTIFFTagSet/TAG_COMPRESSION)
        compression (if compressionField
                      (.getAsLong compressionField 0)
                      BaselineTIFFTagSet/COMPRESSION_NONE)]
    (debug/echo rootIFD)
    (debug/echo predictorField)
    (debug/echo (tiff-predictor-name predictor))
    (debug/echo compressionField)
    (debug/echo (tiff-compression-name compression))))
;;---------------------------------------------------------------------
;; TODO: extract useful info from pngs
#_(defmethod metadata-lookup PNGMetadata [^PNGMetadata metadata]
    (let [rootIFD (.getRootIFD metadata)
          predictorField (.getTIFFField rootIFD BaselineTIFFTagSet/TAG_PREDICTOR)
          predictor (if predictorField
                      (.getAsLong predictorField 0)
                      BaselineTIFFTagSet/PREDICTOR_NONE)
          compressionField (.getTIFFField rootIFD BaselineTIFFTagSet/TAG_COMPRESSION)
          compression (if compressionField
                        (.getAsLong compressionField 0)
                        BaselineTIFFTagSet/COMPRESSION_NONE)]
      (debug/echo rootIFD)
      (debug/echo predictorField)
      (debug/echo (tiff-predictor-name predictor))
      (debug/echo compressionField)
      (debug/echo (tiff-compression-name compression))))
;;---------------------------------------------------------------------
(defn lookup [input]
  (println)
  (println input)
  (with-open [iis (ImageIO/createImageInputStream (io/file input))]
    (let [readers  (iterator-seq (ImageIO/getImageReaders iis))
          ^ImageReader reader (first readers)
          _(.setInput reader iis)
          metadata (.getImageMetadata reader 0)
          writer (ImageIO/getImageWriter reader)]
      (pp/pprint writer)
      (pp/pprint metadata)
      (metadata-lookup metadata)
      )))
;;---------------------------------------------------------------------
(doseq [input
        (image/image-file-seq (io/file "images"))
        #_[
           "images/imageio/USGS_13_n38w077_dir5.tiff"
           "images/imageio/ETOPO_2022_v1_60s_PNW_bed.tiff"
           "images/imageio/ETOPO_2022_v1_60s_N90W180_bed.tif"
           "images/imageio/eo_base_2020_clean_geo.tif"
           "images/imageio/ldem_4.tif"
           "images/imageio/gebco_08_rev_elev_21600x10800.png"
           "images/imageio/world.topo.bathy.200412.3x5400x2700.png"
           ]]
  (lookup input))
;;---------------------------------------------------------------------
