(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.geom.mesh
  {:doc     "Embedded cell complexes."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-11-16"}
  (:require [mop.cmplx.complex :as cmplx]
            [mop.commons.debug :as debug]
            [mop.geom.rn :as rn]
            [mop.geom.s2 :as s2]
            [mop.geom.space :as space])
  (:import [clojure.lang IFn]
           [java.util List]
           [mop.cmplx.complex Cell CellComplex OneSimplex SimplicialComplex2D
                              TwoSimplex VertexPair ZeroSimplex]
           [mop.java.geom Point2U]
           [org.apache.commons.geometry.spherical.twod GreatArc Point2S]))

;;---------------------------------------------------------------

(definterface Mesh
  (^mop.cmplx.complex.CellComplex cmplx [])
  (^clojure.lang.IFn embedding []))

(defn cmplx ^CellComplex [^Mesh mesh] (.cmplx mesh))
(defn embedding ^IFn [^Mesh mesh] (.embedding mesh))
(defn faces [^Mesh mesh] (.faces (.cmplx mesh)))

;;---------------------------------------------------------------
;; TODO: move these to Java to get better control over construction?
;; TODO: require sorted map for embedding consistency?
;;---------------------------------------------------------------
;; Embedded quadrilateral cell complex.

(deftype TriangleMesh
  [^SimplicialComplex2D _cmplx
   ^IFn _embedding]
  :load-ns true

  Mesh
  (cmplx [this] (._cmplx this))
  (embedding [this] (._embedding this)))

;;---------------------------------------------------------------

(defn triangle-mesh ^TriangleMesh [^SimplicialComplex2D cmplx
                                   ^IFn embedding]
  (doall
   (map #(assert (not (nil? (embedding %)))
                 (println %))
        (.vertices cmplx)))
  (TriangleMesh. cmplx embedding))

;;---------------------------------------------------------------
;; TODO: defmulti depending on co-domain of embedding

(defn signed-area [^IFn embedding ^TwoSimplex face]
  (let [^Point2S a (embedding (.z0 face))
        ^Point2S b (embedding (.z1 face))
        ^Point2S c (embedding (.z2 face))]
    (s2/signed-area a b c)))

;;---------------------------------------------------------------
;; TODO: move these to Java to get better control over construction?
;; TODO: require sorted map for embedding consistency?
;;---------------------------------------------------------------

(defmulti mesh
          "Create a triangle or quad mesh, depending on the complex."
          (fn [cmplx _embedding] (class cmplx)))

(defmethod mesh SimplicialComplex2D [^SimplicialComplex2D complex
                                     ^IFn embedding]
  (triangle-mesh complex embedding))

;;---------------------------------------------------------------
;; just map the transform over the vals of the embedding.
;; TODO: require 1st arg of <code>transform</code> to be a function.
;; and then <code>transform</code> could just be <code>

(defmethod rn/transform [Object TriangleMesh] [^Object f ^TriangleMesh x]
  (triangle-mesh
   (.cmplx x)
   (update-vals (.embedding x) #(rn/transform f %))))

;;---------------------------------------------------------------
;; TODO: force embedding to return points of some kind.

(defmulti
 points
 "Use the <code<embedding</code> to convert an abstract simplicial
object <code>x</code> to a list of points in some space,
most likely R^3 or S^2."
 (fn [_embedding x] (class x)))

(defmethod points List [^IFn embedding ^List x]
  (mapv embedding x))

(defmethod points ZeroSimplex [^IFn embedding ^ZeroSimplex x]
  [(embedding x)])

(defmethod points OneSimplex [^IFn embedding ^OneSimplex x]
  [(embedding (.z0 x))
   (embedding (.z1 x))])

(defmethod points VertexPair [^IFn embedding ^VertexPair x]
  [(embedding (.z0 x))
   (embedding (.z1 x))])

(defmethod points TwoSimplex [^IFn embedding ^TwoSimplex x]
  [(embedding (.z0 x))
   (embedding (.z1 x))
   (embedding (.z2 x))])

(defmethod points SimplicialComplex2D [^IFn embedding ^SimplicialComplex2D x]
  (points embedding (.vertices ^SimplicialComplex2D x)))

;;---------------------------------------------------------------
;; TODO: Incorporate alternate subdivision rules,
;; especially with regards to inherited embedding.
;; Initial version puts new vertex at some centroid of parent
;; edge/face --- reasonably straightforward in euclidean case,
;; not so much in spherical case.
;; TODO: should inherited vertices have themselves (or a parent vertex)
;; as parent,
;;

(defmethod cmplx/midpoint-subdivide-4 TriangleMesh [^TriangleMesh m]
  (let [{^CellComplex child :child parent :parent}
        (cmplx/midpoint-subdivide-4 (.cmplx m))
        embedding (.embedding m)]
    (mesh
     child
     (into
      {}
      (map
       (fn [v] [v (apply space/midpoint (points embedding (parent v)))])
       (.vertices child))))))

;;---------------------------------------------------------------

#_(defn ^GreatArc arc
    ([^IFn s2 ^ZeroSimplex a ^ZeroSimplex b]
     (s2/arc ^Point2S (s2 a) ^Point2S (s2 b)))
    ([s2 ^OneSimplex ab] (arc s2 (.z0 ab) (.z1 ab))))

;;---------------------------------------------------------------
;; TODO: generalize to other cutting rules.

(let [TWO_PI (* Math/PI 2)]

  ;; TODO: a bunch of unchecked assumptions hidden here?

  (defn unwrap-1 ^Point2U [^Point2S a ^Point2S b]
    "Update <code>a</code> so the path
  from <code>a</code> to <code>b</code> is continuous"
    (let [aa (.getAzimuth a)
          ba (.getAzimuth b)]
      (Point2U/of (if (> aa ba) (- aa TWO_PI) (+ aa TWO_PI))
                  (.getPolar a))))

  (defn unwrap-2 ^Point2U [^Point2S a ^Point2S b]
    "Update <code>a</code> so the path
    from <code>a</code> to <code>b</code> is continuous"
    (let [aa (.getAzimuth a)
          ba (.getAzimuth b)
          [aaa bba] (if (> aa ba)
                      [(- aa TWO_PI) (+ ba TWO_PI)]
                      [(+ ba TWO_PI) (- ba TWO_PI)])]
      [(Point2U/of aaa (.getPolar a))
       (Point2U/of bba (.getPolar b))]))

  (defn unwrap-3 ^Point2U [^Point2S a ^Point2S b ^Point2S c]
    "Azimuth a is zero, and b-c intersects dateline"
    (let [aa (.getAzimuth a)
          ba (.getAzimuth b)
          ca (.getAzimuth c)
          aaa TWO_PI
          [bba cca] (if (> ba ca)
                      [(- aa TWO_PI) (+ ca TWO_PI)]
                      [(+ ba TWO_PI) (- ca TWO_PI)])]
      (assert (== 0.0 aa))
      [(Point2U/of aaa (.getPolar a))
       (Point2U/of bba (.getPolar b))
       (Point2U/of cca (.getPolar c))
       ]))
  )

;; TODO: check orientation of new faces
(defn dateline-cut-face [^IFn embedding ^TwoSimplex face]
  "Take a face with an <code>PointS2</code> embedding,
  return new face(s) and vertices with a U2 embedding that is continuous
  across the dateline."
  ;; TODO: sort by azimuth to reduce assumptions
  (let [a (.z0 face)
        b (.z1 face)
        c (.z2 face)
        pa (embedding a)
        pb (embedding b)
        pc (embedding c)
        ab (s2/dateline-crossing pa pb)
        bc (s2/dateline-crossing pb pc)
        ca (s2/dateline-crossing pc pa)]
    (debug/echo ab)
    (debug/echo bc)
    (debug/echo ca)
    (cond
      ab
      (cond
        bc
        (if ca
          ;; 3 edges may interset dateline if a vertex lies on it
          ;; in that case we need unwrap all 3 vertices
          (let [aa (cmplx/simplex (str (.toString a) "*"))
                bb (cmplx/simplex (str (.toString b) "*"))
                cc (cmplx/simplex (str (.toString c) "*"))
                [paa pbb pcc] (unwrap-3 pa pb pc)]
            {:new-faces [(cmplx/simplex aa b cc)
                         (cmplx/simplex a bb c)]
             :u2 {aa paa bb pbb cc pcc}})
          ;; else
          (let [bb (cmplx/simplex (str (.toString b) "*"))]
            {:new-faces [(cmplx/simplex a bb c)]
             :u2 {bb (unwrap-1 pb pa)}}))
        ca
        (let [aa (cmplx/simplex (str (.toString a) "*"))]
          {:new-faces [(cmplx/simplex aa b c)]
           :u2 {aa (unwrap-1 pa pb)}})
        :else
        (let [aa (cmplx/simplex (str (.toString a) "*"))
              bb (cmplx/simplex (str (.toString b) "*"))
              [paa pbb] (unwrap-2 pa pb)]
          {:new-faces [(cmplx/simplex aa b c)
                       (cmplx/simplex a bb c)]
           :u2 {aa paa bb pbb}}))
      bc (if ca
           (let [cc (cmplx/simplex (str (.toString c) "*"))]
             {:new-faces [(cmplx/simplex a b cc)]
              :u2 {cc (unwrap-1 pc pb)}})
           ;else
           (let [cc (cmplx/simplex (str (.toString c) "*"))
                 bb (cmplx/simplex (str (.toString b) "*"))
                 [pcc pbb] (unwrap-2 pc pb)]
             {:new-faces [(cmplx/simplex a b cc)(cmplx/simplex a bb c)]
              :u2 {cc pcc bb pbb}}))
      ca (let [aa (cmplx/simplex (str (.toString a) "*"))
               cc (cmplx/simplex (str (.toString c) "*"))
               [pcc paa] (unwrap-2 pc pa)]
           {:new-faces [(cmplx/simplex aa b c)
                        (cmplx/simplex a b cc)]
            :u2 {aa paa cc pcc}})
      :else {})))

;;---------------------------------------------------------------

(defn dateline-cut [^TriangleMesh mesh]
  "Take a triangle mesh with an <code>PointS2</code> embedding,
  duplicate vertices and faces to enable, create, and return
  a continuous <code>PointU2</code> embedding."
  (let [s2-embedding (.embedding mesh)]
    (loop [faces (faces mesh)
           remaining faces
           u2-embedding (update-vals s2-embedding s2/s2-to-u2)]
      (if (empty? remaining)
        (triangle-mesh (cmplx/simplicial-complex-2d faces) u2-embedding)
        (let [{:keys [u2 new-faces]}
              (dateline-cut-face s2-embedding (first faces))]
          (debug/echo u2)
          (debug/echo new-faces)
          (recur (concat new-faces faces)
                 (rest remaining)
                 (merge u2 u2-embedding)))))))

;;---------------------------------------------------------------

(defn coordinates-and-elements  [{:keys [^CellComplex cmplx
                                         _s2-embedding
                                         xyz-embedding
                                         rgba-embedding
                                         dual-embedding
                                         txt-embedding]}]
  "Return a float array and an int array suitable for passing to GLSL.
  Don't rely on any ordering of cells and vertices."
  (let [faces (.faces cmplx)
        zeros (sort (.vertices cmplx))
        zindex (into {} (map (fn [z i] [z i])
                             zeros (range (count zeros))))
        indices (flatten (map (fn [^Cell face]
                                (mapv #(zindex %) (.vertices face)))
                              faces))
        coordinates (flatten (map #(concat (rn/coordinates (xyz-embedding %))
                                           (rn/coordinates (rgba-embedding %))
                                           (rn/coordinates (dual-embedding %))
                                           (rn/coordinates (txt-embedding %)))
                                  zeros))]
    [coordinates indices]))

;;---------------------------------------------------------------
