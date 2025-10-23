(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.lwjgl.glfw.util

  {:doc "LWJGL/GLFW utilities"
   :author "palisades dot lakes at gmail dot com"
   :version "2025-10-22"}

  (:require [mop.geom.arcball :as arcball]
            [mop.lwjgl.util :as lwjgl])
  (:import
   [java.util Map]
   [org.apache.commons.geometry.euclidean.twod Vector2D]
   [org.lwjgl PointerBuffer]
   [org.lwjgl.glfw GLFW]
   [org.lwjgl.opengl GL GL46]))

;;--------------------------------------------------------------
;; Window
;;--------------------------------------------------------------
;; WARNING: problems with multithreaded calls!!!

(let [ww (int-array 1)
      hh (int-array 1)]
  (defn ^Vector2D window-wh [^long window]
    (GLFW/glfwGetWindowSize window ww hh)
    (Vector2D/of (aget ww 0) (aget hh 0))))

(let [xx (double-array 1)
      yy (double-array 1)]
  (defn ^Vector2D cursor-xy [^long window]
    (GLFW/glfwGetCursorPos window xx yy)
    (Vector2D/of (aget xx 0) (aget yy 0))))

;;--------------------------------------------------------------
;; TODO: check for prior initialization?

(let [initialized? (atom false)]
  (defn init []
    (when-not @initialized?
      (GLFW/glfwInit)
      (reset! initialized? true))))

;;--------------------------------------------------------------
;; Monitor
;;--------------------------------------------------------------
;; TODO: ensure monitor is a valid monitor handle
;; Wrap in a Monitor object?

;(defn ^GLFWVidMode monitor-mode [monitor]
;  ( GLFW/glfwGetVideoMode monitor))

;; TODO: return a geometric object of some kind?
;(defn monitor-wh [monitor]
;  (let [mode (monitor-mode monitor)]
;    [(.width mode) (.height mode)]))

;(defn monitor-min-wh [monitor]
;  (let [[w h] (monitor-wh monitor)]
;    (min w h)))

;;(defn primary-monitor [] (GLFW/glfwGetPrimaryMonitor))

(defn monitors
  "Return a sequence of long monitor handles,
   in the same order as GLFW/glfwGetMonitors."
  []
  (let [^PointerBuffer buffer (GLFW/glfwGetMonitors)]
    (loop [mm []]
      (if (.hasRemaining buffer)
        (recur (conj mm (.get buffer)))
        mm))))

;(defn min-monitor-dimension [mm]
;  (reduce min (map monitor-min-wh mm)))

;; TODO: return some kind of Rectangle object
(defn monitor-work-area [^Long monitor]
  (let [^ints x (int-array 1)
        ^ints y (int-array 1)
        ^ints w (int-array 1)
        ^ints h (int-array 1)]
    (GLFW/glfwGetMonitorWorkarea monitor x y w h)
    [(aget x 0)  (aget y 0) (aget w 0) (aget h 0)]))

;;--------------------------------------------------------------

(defn ^Long start-window
  ([monitor ^String title
    mouse-button
    arcball]
   (init)
   (GLFW/glfwDefaultWindowHints)
   (GLFW/glfwWindowHint GLFW/GLFW_DECORATED GLFW/GLFW_TRUE)
   (let [[^double x ^double y ^double monitor-w ^double monitor-h]
         (monitor-work-area monitor)
         ww (int (/ (* 3 monitor-w) 4))
         wh (int (/ (* 3 monitor-h) 4))
         window (GLFW/glfwCreateWindow ww wh title 0 0)]
     (GLFW/glfwSetWindowPos window (+ x (/ monitor-w 8)) (+ y (/ monitor-h 8)))
     (GLFW/glfwMakeContextCurrent window)
     ;; TODO: does this belong here?
     (GL/createCapabilities)
     (GL46/glDebugMessageCallback lwjgl/debug-msg-callback 0)
     (lwjgl/check-error)
     (GLFW/glfwShowWindow window)

     ;; TODO: reset arcball when window resized
     (reset! arcball (arcball/ball ww wh))

     (GLFW/glfwSetMouseButtonCallback
      window
      (fn [window _button action _mods]
        ;; TODO: make this atomic
        (when  (= action GLFW/GLFW_PRESS)
          (reset! mouse-button true)
          (reset! arcball (arcball/update-sphere-pt-origin @arcball (cursor-xy window))))

        (when (= action GLFW/GLFW_RELEASE)
          (reset! mouse-button false)
          (reset! arcball (arcball/update-q-origin @arcball (cursor-xy window))))))

     #_(GLFW/glfwSetCursorPosCallback
        window
        (fn [_window x y]
          (reset! mouse-pos [x (- window-h y 1)])))
     #_(GLFW/glfwSetWindowSizeCallback
        window
        (fn [_window w h]))
     window))

  ;; TODO: better way to choose default monitor
  ([^String title mouse-button arcball]
   (init)
   (start-window (last (monitors)) title mouse-button arcball)))

;;--------------------------------------------------------------

(defn draw-quads [^long window ^long max-index]
  (GL46/glClear GL46/GL_COLOR_BUFFER_BIT)
  (GL46/glCullFace GL46/GL_BACK)
  (GL46/glDrawElements GL46/GL_QUADS max-index GL46/GL_UNSIGNED_INT 0)
  (GLFW/glfwSwapBuffers window)
  (lwjgl/check-error))

;;--------------------------------------------------------------

(defn clean-up [^Long window
                ^Long program
                ^Map vao
                ^Integer color-texture
                ^Integer elevation-texture]
  (GL46/glDeleteProgram program)
  (lwjgl/teardown-vao vao)
  (GL46/glDeleteTextures color-texture)
  (GL46/glDeleteTextures elevation-texture)
  (lwjgl/check-error)
  (GLFW/glfwDestroyWindow window)
  (GLFW/glfwTerminate))

;;--------------------------------------------------------------

