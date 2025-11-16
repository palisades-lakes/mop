(ns mop.geom.space
  {:doc     "Embedded cell complexes."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-11-15"})

;;---------------------------------------------------------------

(defmulti midpoint
          "Return a 'center' of some kind for the collection of points.
          Mean for euclidean space, not so obvious for spherical space.
          Assume all points are the same type, throw an exceptions otherwise."
          (fn [p0 & _points] (class p0)))

