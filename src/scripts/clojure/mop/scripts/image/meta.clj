(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\scripts\image\meta.clj
;;----------------------------------------------------------------
(ns mop.scripts.image.meta
  {:doc
   "Explore image metadata."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-12-15"}
  (:require
   [clojure.java.io :as io]
   [clojure.pprint :as pp]
   [mop.commons.io :as mci]
   [mop.image.util :as image])
  (:import
   [it.geosolutions.imageioimpl.plugins.tiff TIFFFieldNode]
   [java.util Arrays]
   [javax.imageio ImageIO ImageReader]
   [javax.imageio.metadata IIOMetadataNode]
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
        (assoc! result (.getName item) (.getValue item))))
    (persistent! result)))

(defn- children-clj [^Node node]
  (let [children (.getChildNodes node)
        items (transient [])]
    (dotimes [i (.getLength children)]
      (conj! items (to-clj (.item children i))))
    (persistent! items)))

;; Does this always represent a short?
#_(defn ^Integer tiff-short-clj [^Node node]
    (assert (= :TIFFShort (keyword (.getNodeName node))))
    (let [attributes (attributes-clj node)]
      (Integer/parseInt (:value attributes))))

;; Does this represent a short[]?
#_(defn tiff-shorts-clj [^Node node]
    (assert (= :TIFFShorts (keyword (.getNodeName node))))
    (println (children-clj node))
    (children-clj node))

(defmethod to-clj Node [^Node node]
  (let [attributes (attributes-clj node)
        children (children-clj node)
        meta {:class (class node)
              :name  (.getNodeName node)
              :value (.getNodeValue node)}
        meta (if-not (empty? attributes) (assoc meta :attributes attributes) meta)
        meta (if-not (empty? children) (assoc meta :children children) meta)]
    meta))

#_(defmethod to-clj Attr [^Attr node]
    {(keyword (.getName node)) (.getValue node)})

#_(defmethod to-clj TIFFFieldNode [^TIFFFieldNode node]
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
(defn meta-to-clj [input]
  (println)
  (println input)
  (image/write-metadata-markdown input)
  (with-open [iis (ImageIO/createImageInputStream (io/file input))]
    (let [readers  (iterator-seq (ImageIO/getImageReaders iis))
          ^ImageReader reader (first readers)
          _(.setInput reader iis)
          metadata (.getImageMetadata reader 0)
          format-names (seq (Arrays/asList (.getMetadataFormatNames metadata)))
          extra-format-names (.getExtraMetadataFormatNames metadata)
          extra-format-names (if extra-format-names (Arrays/asList extra-format-names) [])
          format-names (concat format-names extra-format-names)
          trees (map #(.getAsTree metadata ^String %) format-names)]
      (with-open [w (io/writer (mci/replace-extension input "edn"))]
        (binding [*out* w]
          (binding [*print-level* false]
            (doseq [tree trees] (pp/pprint (to-clj tree)))))))))
;;---------------------------------------------------------------------
(doseq [input
        (image/image-file-seq (io/file "images/imageio-ext"))
        #_[
         "images/lroc/lroc_color_poles_2k.tif"
         "images/lroc/lroc_color_poles_2k-gtx.tif"
         ;"images/imageio/USGS_13_n38w077_dir5.tiff"
         ;"images/imageio/ETOPO_2022_v1_60s_PNW_bed.tiff"
         ;"images/imageio/ETOPO_2022_v1_60s_N90W180_bed.tif"
         ;"images/imageio/eo_base_2020_clean_geo.tif"
         ;"images/imageio/ldem_4.tif"
         ;"images/imageio/gebco_08_rev_elev_21600x10800.png"
         ;"images/imageio/world.topo.bathy.200412.3x5400x2700.png"
         ]]
  (meta-to-clj input))
;;---------------------------------------------------------------------
