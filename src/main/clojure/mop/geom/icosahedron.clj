(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.geom.icosahedron

  {:doc     "Embedded cell complexes."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-11-13"}
  (:require [mop.cmplx.complex :as cmplx]
            [mop.geom.mesh :as mesh]
            [mop.geom.rn :as rn]
            [mop.geom.s2 :as s2])
  (:import [mop.geom.mesh TriangleMesh]
           [mop.java.geom Point2U]
           [org.apache.commons.geometry.euclidean.twod Vector2D]
           [org.apache.commons.geometry.spherical.twod Point2S]))

;;---------------------------------------------------------------
;; Create abstract complex and r3 embedding together
;; to avoid orientation problems, etc.

(let [r (/ (+ 1.0 (Math/sqrt 5.0)) 2.0)
      -r (- r)]
  (defn ^TriangleMesh r3-icosahedron []
    (let [a (cmplx/simplex "a") b (cmplx/simplex "b") c (cmplx/simplex "c") d (cmplx/simplex "d")
          e (cmplx/simplex "e") f (cmplx/simplex "f") g (cmplx/simplex "g") h (cmplx/simplex "h")
          i (cmplx/simplex "i") j (cmplx/simplex "j") k (cmplx/simplex "k") l (cmplx/simplex "l")
          cmplx (cmplx/simplicial-complex-2d
                 (map #(apply cmplx/simplex %)
                      [;; texture interpolation issues
                       [a d b] [b d g] [b g h] [g l h]
                       [a e d]   [h l i] [k i l]
                       ;; interpolation ok
                       [a b c] [a c f] [a f e] [b h c] [c i f] [c h i]
                       [d e j] [d j g] [e f k] [e k j] [f i k] [g j l] [k l j]]))
          ;; put a at top
          ;;qr (QuaternionRotation/createVectorRotation (rn/vector -1  r  0) (rn/vector 0 0 1))
          embedding {a (rn/vector -1  r  0) b (rn/vector  1  r  0)
                     c (rn/vector  0  1 -r) d (rn/vector  0  1  r)
                     e (rn/vector -r  0  1) f (rn/vector -r  0 -1)
                     g (rn/vector  r  0  1) h (rn/vector  r  0 -1)
                     i (rn/vector  0 -1 -r) j (rn/vector  0 -1  r)
                     k (rn/vector -1 -r  0) l (rn/vector  1 -r  0)}]
      #_(rn/transform qr (TriangleMesh. cmplx embedding))
      (TriangleMesh. cmplx embedding))))

(defn ^TriangleMesh s2-icosahedron []
  (let [da (double (* 0.2 Math/PI))
        p1 (double (/ Math/PI 3.0))
        p2 (double (* 2 p1))
        a (cmplx/simplex "a") b (cmplx/simplex "b") c (cmplx/simplex "c") d (cmplx/simplex "d")
        e (cmplx/simplex "e") f (cmplx/simplex "f") g (cmplx/simplex "g") h (cmplx/simplex "h")
        i (cmplx/simplex "i") j (cmplx/simplex "j") k (cmplx/simplex "k") l (cmplx/simplex "l")
        cmplx (cmplx/simplicial-complex-2d
               (map #(apply cmplx/simplex %)
                    [[a b c]
                     [a c d] [a d e] [a e f] [a f b]
                     [b g c] [c h d] [d i e] [e j f] [f k b]
                     [c g h] [d h i] [e i j] [f j k] [b k g]
                     [l h g] [l i h] [l j i] [l k j] [l g k]
                    ]
                    ))
        embedding {a (s2/point 0.0 0.0)

                   b (s2/point (* -1 da) p1)
                   c (s2/point (*  1 da) p1)
                   d (s2/point (*  3 da) p1)
                   e (s2/point (*  5 da) p1)
                   f (s2/point (*  7 da) p1)

                   g (s2/point (*  0 da) p2)
                   h (s2/point (*  2 da) p2)
                   i (s2/point (*  4 da) p2)
                   j (s2/point (*  6 da) p2)
                   k (s2/point (*  8 da) p2)

                   l (s2/point 0.0 Math/PI)}]
    #_(rn/transform qr (TriangleMesh. cmplx embedding))
    (TriangleMesh. cmplx embedding)))

;;------------------------------------------------------------------------------
;; Cut icosahedron to simplify texture mapping and other
;; 2d projections. Return unwrapped txt coordinate embedding.
;; TODO: automate the cut. Key question: what to do when it's not spherical?
;; TODO: check if this is a regular icosahedron

(let [dx (double 0.1)
      y1 (double (/ 1.0 3))
      y2 (double (/ 2.0 3))]
  (defn ^TriangleMesh txt-cut-icosahedron []
    (let [a (cmplx/simplex "a")
          b (cmplx/simplex "b")
          c (cmplx/simplex "c")
          d (cmplx/simplex "d")
          e (cmplx/simplex "e")
          f (cmplx/simplex "f") g (cmplx/simplex"g") h (cmplx/simplex"h") i (cmplx/simplex"i") j (cmplx/simplex"j")
          k (cmplx/simplex "k")
          l (cmplx/simplex"l") m (cmplx/simplex"m") n (cmplx/simplex"n") o (cmplx/simplex"o")
          p (cmplx/simplex"p") q (cmplx/simplex"q") r (cmplx/simplex"r") s (cmplx/simplex"s") t (cmplx/simplex"t")
          u (cmplx/simplex"u") v (cmplx/simplex"v") w (cmplx/simplex"w") x (cmplx/simplex"x")
          txt-embedding {a (Vector2D/of (* 0 dx) 1)
                         b (Vector2D/of (* 2 dx) 1)
                         c (Vector2D/of (* 4 dx) 1)
                         d (Vector2D/of (* 6 dx) 1)
                         e (Vector2D/of (* 8 dx) 1)

                         f (Vector2D/of (* -1 dx) y2)
                         g (Vector2D/of (*  1 dx) y2)
                         h (Vector2D/of (*  3 dx) y2)
                         i (Vector2D/of (*  5 dx) y2)
                         j (Vector2D/of (*  7 dx) y2)
                         k (Vector2D/of (*  9 dx) y2)

                         l (Vector2D/of (*  0 dx) y1)
                         m (Vector2D/of (*  2 dx) y1)
                         n (Vector2D/of (*  4 dx) y1)
                         o (Vector2D/of (*  6 dx) y1)
                         p (Vector2D/of (*  8 dx) y1)
                         q (Vector2D/of (* 10 dx) y1)

                         r (Vector2D/of (* 1 dx) 0)
                         s (Vector2D/of (* 3 dx) 0)
                         t (Vector2D/of (* 5 dx) 0)
                         u (Vector2D/of (* 7 dx) 0)
                         v (Vector2D/of (* 9 dx) 0)

                         w (Vector2D/of (* 10 dx) 1)
                         x (Vector2D/of (* -1 dx) 0)}
          cmplx (cmplx/simplicial-complex-2d
                 (map #(apply cmplx/simplex %)
                      [[a f g] [b g h] [c h i] [d i j] [e j k]
                       [a g b]
                       [b h c]
                       [c i d]
                       [d j e]
                       [f l g] [g m h] [h n i] [i o j] [j p k]
                       [g l m] [h m n] [i n o] [j o p] [k p q]
                       [l r m] [m s n] [n t o] [o u p] [p v q]
                       [r s m] [s t n] [t u o] [u v p]
                       [e k w] [l x r]
                       ]))
          ]
      (mesh/triangle-mesh cmplx txt-embedding)
      )))

;;------------------------------------------------------------------------------
;; Cut icosahedron to simplify texture mapping and other
;; 2d projections. Return wrapped s2 embedding.
;; TODO: automate the cut. Key question: what to do when it's not spherical?
;; TODO: check if this is a regular icosahedron

(let [da (double (* Math/PI 0.2))
      p1 (double (/ Math/PI 3.0))
      p2 (double (* 2 p1))]
  (defn ^TriangleMesh s2-cut-icosahedron []
    (let [a (cmplx/simplex"a")
          b (cmplx/simplex"b") c (cmplx/simplex"c") d (cmplx/simplex"d")
          e (cmplx/simplex"e") f (cmplx/simplex"f") g (cmplx/simplex"g")
          h (cmplx/simplex"h") i (cmplx/simplex"i") j (cmplx/simplex"j")
          k (cmplx/simplex"k") l (cmplx/simplex"l") m (cmplx/simplex"m")
          n (cmplx/simplex"n")
          s2-embedding {a Point2S/PLUS_K

                        b (s2/point (*  0 da) p1)
                        c (s2/point (*  2 da) p1)
                        d (s2/point (*  4 da) p1)
                        e (s2/point (*  6 da) p1)
                        f (s2/point (*  8 da) p1)
                        g (s2/point (* 10 da) p1)

                        h (s2/point (*  -1 da) p2)
                        i (s2/point (*  1 da) p2)
                        j (s2/point (*  3 da) p2)
                        k (s2/point (*  5 da) p2)
                        l (s2/point (*  7 da) p2)
                        m (s2/point (*  9 da) p2)

                        n Point2S/MINUS_K}
          cmplx (cmplx/simplicial-complex-2d
                 (map #(apply cmplx/simplex %)
                      [[a b c] [a c d] [a d e] [a e f] [a f g]
                       [b i c] [c j d] [d k e] [e l f] [f m g]
                       [b h i] [c i j] [d j k] [e k l] [f l m]
                       [n i h] [n j i] [n k j] [n l k] [n m l]
                       ]))
          ]
      (mesh/triangle-mesh cmplx s2-embedding)
      )))

;;------------------------------------------------------------------------------
;; Cut icosahedron to simplify texture mapping and other
;; 2d projections. Return wrapped s2 embedding.
;; TODO: automate the cut. Key question: what to do when it's not spherical?
;; TODO: check if this is a regular icosahedron

(let [a0 -0.5
      da (double (* Math/PI 0.2))
      p1 (double (/ Math/PI 3.0))
      p2 (double (* 2 p1))]
  (defn ^TriangleMesh u2-cut-icosahedron []
    (let [a (cmplx/simplex"a")
          b (cmplx/simplex"b") c (cmplx/simplex"c") d (cmplx/simplex"d")
          e (cmplx/simplex"e") f (cmplx/simplex"f") g (cmplx/simplex"g")
          h (cmplx/simplex"h") i (cmplx/simplex"i") j (cmplx/simplex"j")
          k (cmplx/simplex"k") l (cmplx/simplex"l") m (cmplx/simplex"m")
          n (cmplx/simplex"n")
          ;; TODO: problem with txt coordinates when cut at exactly 0 and 2PI
          s2-embedding {a Point2U/PLUS_K
                        b (Point2U/of (+ a0 (*  0 da)) p1)
                        c (Point2U/of (+ a0 (*  2 da)) p1)
                        d (Point2U/of (+ a0 (*  4 da)) p1)
                        e (Point2U/of (+ a0 (*  6 da)) p1)
                        f (Point2U/of (+ a0 (*  8 da)) p1)
                        g (Point2U/of (+ a0 (* 10 da)) p1)
                        h (Point2U/of (+ a0 (* -1 da)) p2)
                        i (Point2U/of (+ a0 (*  1 da)) p2)
                        j (Point2U/of (+ a0 (*  3 da)) p2)
                        k (Point2U/of (+ a0 (*  5 da)) p2)
                        l (Point2U/of (+ a0 (*  7 da)) p2)
                        m (Point2U/of (+ a0 (*  9 da)) p2)
                        n Point2U/MINUS_K}
          cmplx (cmplx/simplicial-complex-2d
                 (map #(apply cmplx/simplex %)
                      [[a b c]
                       [a c d] [a d e] [a e f]
                       [a f g]
                       [b i c] [c j d] [d k e] [e l f] [f m g]
                       [b h i] [c i j] [d j k] [e k l] [f l m]
                       [n i h]
                       [n j i] [n k j] [n l k] [n m l]
                       ]))
          ]
      (mesh/triangle-mesh cmplx s2-embedding)
      )))

;;---------------------------------------------------------------
