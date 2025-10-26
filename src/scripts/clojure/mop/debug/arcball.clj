(set! *warn-on-reflection* true)
;;(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\debug\arcball.clj
;;----------------------------------------------------------------
(ns mop.debug.arcball
  {:doc     "check arcball calculations"
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-10-23"}

  (:require
   [clojure.pprint :as pp]
   [mop.geom.arcball :as arcball]
   [mop.geom.util :as geom])
  (:import
   [org.apache.commons.geometry.euclidean.threed Vector3D Vector3D$Unit]
   [org.apache.commons.geometry.euclidean.threed.rotation QuaternionRotation]))

;;-------------------------------------------------------------

(defn rotate [^QuaternionRotation qr ^Vector3D v]
  (let [q (.getQuaternion qr)
        w (.getW q)
        xyz (Vector3D/of (.getX q) (.getY q) (.getZ q))
        v1 (.add (.cross v xyz) w v)
        v2 (.cross v1 xyz)]
    (.add v 2.0 (.cross v v2))))

;;-------------------------------------------------------------

(defmacro printer [name]
  `(do
     (println (quote ~name))
     (pp/pprint ~name)
     (println)))

;;-------------------------------------------------------------

(let [;;qxy (QuaternionRotation/createVectorRotation
      ;;     Vector3D$Unit/PLUS_X
      ;;     Vector3D$Unit/PLUS_Y)
      ;ball (arcball/ball
      ;      1024 512
      ;      Vector3D$Unit/PLUS_X
      ;      (QuaternionRotation/identity))
      ;wxy (geom/vector 512 -1)
      ;axy (arcball/window-to-arcball ball wxy)
      ;p (arcball/arcball-to-sphere-pt axy)
      ;q (arcball/current-q ball wxy)
      ball (arcball/ball
            1024 512
            Vector3D$Unit/PLUS_Z
            (QuaternionRotation/identity))
      wxy (geom/make-vector (+ 256 512) 256)
      axy (arcball/window-to-arcball ball wxy)
      p (arcball/arcball-to-sphere-pt axy)
      ;;q (arcball/current-q ball wxy)
      q (QuaternionRotation/createVectorRotation
           Vector3D$Unit/PLUS_Z
           Vector3D$Unit/PLUS_X)

      axis (.getAxis q)
      ;;radians (.getAngle q)
      degrees (Math/toDegrees (.getAngle q))
      rotated (.apply q Vector3D$Unit/PLUS_Z)
      ]
  (println)
  (printer ball)
  (printer wxy)
  (printer axy)
  (printer p)
  (printer q)
  (printer axis)
  ;;(printer radians)
  (printer degrees)
  (printer rotated)
  (println))