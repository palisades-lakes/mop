(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.lwjgl.glfw.util

  {:doc "LWJGL/GLFW utilities"
   :author "palisades dot lakes at gmail dot com"
   :version "2025-11-12"}

  (:require [mop.geom.arcball :as arcball]
            [mop.geom.rn :as rn]
            [mop.lwjgl.util :as lwjgl])
  (:import
   [java.util Map]
   [org.lwjgl PointerBuffer]
   [org.lwjgl.glfw Callbacks GLFW GLFWErrorCallback]
   [org.lwjgl.opengl GL GL46]))

;;--------------------------------------------------------------
(defn check-error []
  (let [;;buffer (PointerBuffer/allocateDirect 1024)
        code (GLFW/glfwGetError nil)
        ;;msg (.getStringUTF8 buffer)
        msg ""
        ]
    (condp == code
      GLFW/GLFW_NO_ERROR
      true
      GLFW/GLFW_NOT_INITIALIZED
      (throw (RuntimeException. (str "GLFW_NOT_INITIALIZED:" msg)))
      GLFW/GLFW_NO_CURRENT_CONTEXT
      (throw (RuntimeException. (str "GLFW_NO_CURRENT_CONTEXT:" msg)))
      GLFW/GLFW_INVALID_ENUM
      (throw (RuntimeException. (str "GLFW_INVALID_ENUM:" msg)))
      GLFW/GLFW_INVALID_VALUE
      (throw (RuntimeException. (str "GLFW_INVALID_VALUE:" msg)))
      GLFW/GLFW_OUT_OF_MEMORY
      (throw (RuntimeException. (str "GLFW_OUT_OF_MEMORY:" msg)))
      GLFW/GLFW_API_UNAVAILABLE
      (throw (RuntimeException. (str "GLFW_API_UNAVAILABLE:" msg)))
      GLFW/GLFW_PLATFORM_ERROR
      (throw (RuntimeException. (str "GLFW_PLATFORM_ERROR:" msg)))
      GLFW/GLFW_API_UNAVAILABLE
      (throw (RuntimeException. (str "GLFW_API_UNAVAILABLE:" msg)))
      GLFW/GLFW_VERSION_UNAVAILABLE
      (throw (RuntimeException. (str "GLFW_FORMAT_UNAVAILABLE:" msg)))
      ;; default
      (throw (RuntimeException. (str "Unknown error code: " code))))))

;;--------------------------------------------------------------
;; Window
;;--------------------------------------------------------------
;; WARNING: problems with multithreaded calls!!!

(let [ww (int-array 1)
      hh (int-array 1)]
  (defn window-wh [^long window]
    (GLFW/glfwGetWindowSize window ww hh)
    (rn/vector (aget ww 0) (aget hh 0))))

(let [xx (double-array 1)
      yy (double-array 1)]
  (defn cursor-xy [^long window]
    (GLFW/glfwGetCursorPos window xx yy)
    (rn/vector (aget xx 0) (aget yy 0))))

;;--------------------------------------------------------------
;; TODO: check for prior initialization?

(let [initialized? (atom false)]
  (defn init []
    (when-not @initialized?
      (.set (GLFWErrorCallback/createPrint System/err))
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
    arcball
    polygon-mode]
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

     (GLFW/glfwSwapInterval 1)
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
     (check-error)

     (GLFW/glfwSetKeyCallback
      window
      (fn [_window _key _scancode action _mods]
        (when (== (int action) GLFW/GLFW_PRESS)
          #_(println key)
          (reset! polygon-mode (if (== (int @polygon-mode) GL46/GL_LINE)
                                 GL46/GL_FILL
                                 GL46/GL_LINE)))))
     (check-error)

     #_(GLFW/glfwSetCursorPosCallback
        window
        (fn [_window x y]
          (reset! mouse-pos [x (- window-h y 1)])))

     #_(GLFW/glfwSetWindowSizeCallback
        window
        (fn [_window w h]))
     window))

  ;; TODO: better way to choose default monitor
  ([^String title mouse-button arcball polygon-mode]
   (init)
   (start-window (last (monitors)) title mouse-button arcball polygon-mode)))

;;--------------------------------------------------------------

(defn draw-faces [^long window state]
  (GL46/glClear (bit-or GL46/GL_COLOR_BUFFER_BIT
                        GL46/GL_DEPTH_BUFFER_BIT))
  (lwjgl/check-error)
  (GL46/glPolygonMode GL46/GL_FRONT_AND_BACK (deref (:polygon-mode state)))
  (lwjgl/check-error)
  (GL46/glDrawElements (:elements state) (:nindices state) GL46/GL_UNSIGNED_INT 0)
  (lwjgl/check-error)
  (GLFW/glfwSwapBuffers window)
  (check-error))

;;--------------------------------------------------------------
;; TODO: lwjgl/check-error reports problems on DestroyWindow...

(defn clean-up [^Long window ^Map setup-map]
  (lwjgl/teardown setup-map)
  (GLFW/glfwMakeContextCurrent 0) (check-error)
  (Callbacks/glfwFreeCallbacks window) (check-error)
  (GLFW/glfwDestroyWindow window) (check-error)
  (GLFW/glfwTerminate) (check-error)
  (.free (GLFW/glfwSetErrorCallback nil)))

;;--------------------------------------------------------------

(defn arcball-loop [input]
  (let [mouse-button (atom false)
        arcball (atom (arcball/ball -1 -1))
        polygon-mode (atom GL46/GL_FILL)
        window (start-window (:title input) mouse-button arcball polygon-mode)
        state (assoc (lwjgl/setup input) :polygon-mode polygon-mode)]
    ;; TODO: call on window resize
    (lwjgl/uniform (:program state) "aspect" (rn/aspect (window-wh window)))
    (lwjgl/uniform (:program state) "quaternion" (:q-origin @arcball))
    (while (not (GLFW/glfwWindowShouldClose window))
      (check-error)
      (draw-faces window state)
      (GLFW/glfwPollEvents)
      (check-error)
      (when @mouse-button
        (lwjgl/uniform
         (:program state) "quaternion"
         (arcball/current-q @arcball (cursor-xy window)))))
    (clean-up window state)))

;;--------------------------------------------------------------

