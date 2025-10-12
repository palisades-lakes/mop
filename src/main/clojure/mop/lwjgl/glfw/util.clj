(ns mop.lwjgl.glfw.util

  {:doc "LWJGL/GLFW utilities"
   :author "palisades dot lakes at gmail dot com"
   :version "2025-10-12"}

  (:import [org.lwjgl PointerBuffer]
           [org.lwjgl.glfw GLFW]
           [org.lwjgl.opengl GL]))
;;--------------------------------------------------------------
;; TODO: check for prior initialization?
(defn init [] (GLFW/glfwInit))

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
;; Window
;;--------------------------------------------------------------

(defn ^Long start-window
  ([monitor ^String title mouse-pos mouse-button]
   (GLFW/glfwDefaultWindowHints)
   (GLFW/glfwWindowHint GLFW/GLFW_DECORATED GLFW/GLFW_TRUE)
   (let [[x y w h] (monitor-work-area monitor)
         window (GLFW/glfwCreateWindow
                 (int (/ (* 3 w) 4)) (int (/ (* 3 h) 4)) title 0 0)]
     (GLFW/glfwSetWindowPos window (+ x (/ w 8)) (+ y (/ h 8)))
     (GLFW/glfwMakeContextCurrent window)
     (GLFW/glfwShowWindow window)
     ;; TODO: does this belong here?
     (GL/createCapabilities)
     (GLFW/glfwSetCursorPosCallback
      window
      (fn [_window x y]
        (reset! mouse-pos [x (- h y 1)])))
     (GLFW/glfwSetMouseButtonCallback
      window
      (fn [_window _button action _mods]
        (reset! mouse-button (= action GLFW/GLFW_PRESS))))
     window))

  ;; TODO: better way to choose default monitor
  ([^String title mouse-pos mouse-button]
   (let [mm (monitors)
         m (last mm)]
     (start-window m title mouse-pos mouse-button))))

;;--------------------------------------------------------------

(defn window-size [^long window]
  (let [ww (int-array 1)
        hh (int-array 1)]
    (GLFW/glfwGetWindowSize window ww hh)
    [(aget ww 0) (aget hh 0)]))

;;--------------------------------------------------------------

(defn clean-up [^long window]
  (GLFW/glfwDestroyWindow window)
  (GLFW/glfwTerminate))

;;--------------------------------------------------------------

