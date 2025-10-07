(ns mop.lwjgl.glfw.util

  {:doc "LWJGL/GLFW utilities"
   :author "palisades dot lakes at gmail dot com"
   :version "2025-10-07"}

  (:import [org.lwjgl.glfw
            GLFW GLFWCursorPosCallbackI
            GLFWMouseButtonCallbackI GLFWVidMode]
           [org.lwjgl.opengl GL]))
;;--------------------------------------------------------------
;; TODO: check for prior initialization?
(defn init [] (GLFW/glfwInit))

;;--------------------------------------------------------------
;; Monitor
;;--------------------------------------------------------------

(defn primary-monitor [] (GLFW/glfwGetPrimaryMonitor))

(defn ^GLFWVidMode monitor-mode [monitor]
  (GLFW/glfwGetVideoMode monitor))

;;--------------------------------------------------------------
;; Window
;;--------------------------------------------------------------

(defn ^Long start-fullscreen-window
  ([monitor ^String title mouse-pos mouse-button]
   (GLFW/glfwDefaultWindowHints)
   (GLFW/glfwWindowHint GLFW/GLFW_DECORATED GLFW/GLFW_TRUE)
   (let [mode (monitor-mode monitor)
         w-mode (.width mode)
         h-mode (.height mode)
         window (GLFW/glfwCreateWindow w-mode h-mode title 0 0)]
     (GLFW/glfwShowWindow window)
     (GLFW/glfwMakeContextCurrent window)
     ;; TODO: does this belong here?
     (GL/createCapabilities)
     ;; do not simplify using a Clojure fn,
     ;; because otherwise the uber jar build breaks
     (GLFW/glfwSetCursorPosCallback
      window
      (reify GLFWCursorPosCallbackI
        (invoke
          [_this _window xpos ypos]
          (reset! mouse-pos [xpos (- h-mode ypos 1)]))))
     (GLFW/glfwSetMouseButtonCallback
      window
      (reify GLFWMouseButtonCallbackI
        (invoke
          [_this _window _button action _mods]
          (reset! mouse-button (= action GLFW/GLFW_PRESS)))))

     window))

  ([^String title mouse-pos mouse-button]
   (start-fullscreen-window
    (primary-monitor) title mouse-pos mouse-button)))

;;--------------------------------------------------------------

(defn window-size [window]
  (let [ww (int-array 1)
        hh (int-array 1)]
    (^[long int/1 int/1] GLFW/glfwGetWindowSize window ww hh)
    [(aget ww 0) (aget hh 0)]))

;;--------------------------------------------------------------

(defn clean-up [window]
  (GLFW/glfwDestroyWindow window)
  (GLFW/glfwTerminate))
;;--------------------------------------------------------------

