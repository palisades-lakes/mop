(ns mop.geometry.r3.matrix

  {:doc     "R3 (double) matrices."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-10-17"})

;;--------------------------------------------------------------
;; TODO: array, row vector, column vector representations?

(defrecord Matrix
  [^double m00 ^double m01 ^double m02
   ^double m10 ^double m11 ^double m12
   ^double m20 ^double m21 ^double m22])

(defn ^Matrix compose [^Matrix m0
                       ^Matrix m1]
  (Matrix.
   (+ (* (:m00 m0) (:m00 m1))
      (* (:m01 m0) (:m10 m1))
      (* (:m02 m0) (:m20 m1)))
   (+ (* (:m00 m0) (:m01 m1))
      (* (:m01 m0) (:m11 m1))
      (* (:m02 m0) (:m21 m1)))
   (+ (* (:m00 m0) (:m02 m1))
      (* (:m01 m0) (:m12 m1))
      (* (:m02 m0) (:m22 m1)))

   (+ (* (:m10 m0) (:m00 m1))
      (* (:m11 m0) (:m10 m1))
      (* (:m12 m0) (:m20 m1)))
   (+ (* (:m10 m0) (:m01 m1))
      (* (:m11 m0) (:m11 m1))
      (* (:m12 m0) (:m21 m1)))
   (+ (* (:m10 m0) (:m02 m1))
      (* (:m11 m0) (:m12 m1))
      (* (:m12 m0) (:m22 m1)))

   (+ (* (:m20 m0) (:m00 m1))
      (* (:m21 m0) (:m10 m1))
      (* (:m22 m0) (:m20 m1)))
   (+ (* (:m20 m0) (:m01 m1))
      (* (:m21 m0) (:m11 m1))
      (* (:m22 m0) (:m21 m1)))
   (+ (* (:m20 m0) (:m02 m1))
      (* (:m21 m0) (:m12 m1))
      (* (:m22 m0) (:m22 m1)))))

