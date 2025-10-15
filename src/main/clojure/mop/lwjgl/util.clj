(ns mop.lwjgl.util

  {:doc     "LWJGL utilities"
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-10-14"}

  (:require [clojure.math :as math]
            [clojure.math :refer [PI]]
            [mop.image.util :as image])

  (:import [java.awt.image WritableRaster]
           [java.nio ByteBuffer FloatBuffer IntBuffer]
           [org.lwjgl BufferUtils]
           [org.lwjgl.opengl GL46])  )

;;-------------------------------------------------------------------
(def ^:const TwoPI (* 2.0 PI))
;;-------------------------------------------------------------------

(defn check-error []
  (let [code (GL46/glGetError)]
    (condp == code
      GL46/GL_NO_ERROR
      true
      GL46/GL_INVALID_ENUM
      (throw (RuntimeException. "GL_INVALID_ENUM"))
      GL46/GL_INVALID_VALUE
      (throw (RuntimeException. "GL_INVALID_VALUE"))
      GL46/GL_INVALID_OPERATION
      (throw (RuntimeException. "GL_INVALID_OPERATION"))
      GL46/GL_INVALID_FRAMEBUFFER_OPERATION
      (throw (RuntimeException. "GL_INVALID_FRAMEBUFFER_OPERATION"))
      GL46/GL_OUT_OF_MEMORY
      (throw (RuntimeException. "GL_OUT_OF_MEMORY"))
      GL46/GL_STACK_UNDERFLOW
      (throw (RuntimeException. "GL_STACK_UNDERFLOW"))
      GL46/GL_STACK_OVERFLOW
      (throw (RuntimeException. "GL_STACK_OVERFLOW"))
      ;; default
      (throw (RuntimeException. (str "Unknown error code: " code))))))

(defn debug-msg-callback [source type id severity length message userParam]
  (throw
   (RuntimeException.
    (str "source=" source ", type=" type ", id=" id ", severity=" severity
         ", length=" length ", message=" message ", userParam=" userParam))))
;;-------------------------------------------------------------------

(defn make-shader [^String source shader-type]
  (let [shader (GL46/glCreateShader shader-type)]
    (check-error)
    (GL46/glShaderSource ^int shader source)
    (GL46/glCompileShader shader)
    (when (zero? (GL46/glGetShaderi shader GL46/GL_COMPILE_STATUS))
      (throw (Exception. (GL46/glGetShaderInfoLog shader 1024))))
    (check-error)
    shader))

(defn make-program [& shaders]
  (let [program (GL46/glCreateProgram)]
    (doseq [shader shaders]
      (GL46/glAttachShader program shader)
      (GL46/glDeleteShader shader))
    (GL46/glLinkProgram program)
    (when (zero? (GL46/glGetProgrami program GL46/GL_LINK_STATUS))
      (throw (RuntimeException. (GL46/glGetProgramInfoLog program 1024))))
    (check-error)
    program))

(defn make-float-buffer [^floats data]
  (let [buffer (BufferUtils/createFloatBuffer (count data))]
    (.put buffer data)
    (.flip buffer)
    buffer))

(defn make-int-buffer [^ints data]
  (let [buffer (BufferUtils/createIntBuffer (count data))]
    (.put buffer data)
    (.flip buffer)
    buffer))

(defn make-byte-buffer [^bytes data]
  (let [buffer (BufferUtils/createByteBuffer (count data))]
    (.put buffer data)
    (.flip buffer)
    buffer))

(defn setup-vao [vertices indices]
  (let [vao (GL46/glGenVertexArrays)
        vbo (GL46/glGenBuffers)
        ibo (GL46/glGenBuffers)]
    (GL46/glBindVertexArray vao)
    (GL46/glBindBuffer GL46/GL_ARRAY_BUFFER vbo)
    (GL46/glBufferData
     GL46/GL_ARRAY_BUFFER
     ^FloatBuffer (make-float-buffer vertices)
     GL46/GL_STATIC_DRAW)
    (GL46/glBindBuffer GL46/GL_ELEMENT_ARRAY_BUFFER ibo)
    (GL46/glBufferData
     GL46/GL_ELEMENT_ARRAY_BUFFER
     ^IntBuffer (make-int-buffer indices)
     GL46/GL_STATIC_DRAW)
    (check-error)
    {:vao vao :vbo vbo :ibo ibo}))

(defn teardown-vao [{:keys [^int vao ^int vbo ^int ibo]}]
  (GL46/glBindBuffer GL46/GL_ELEMENT_ARRAY_BUFFER 0)
  (GL46/glDeleteBuffers ibo)
  (GL46/glBindBuffer GL46/GL_ARRAY_BUFFER 0)
  (GL46/glDeleteBuffers vbo)
  (GL46/glBindVertexArray 0)
  (GL46/glDeleteBuffers vao)
  (check-error))

;;------------------------------------------------------------------
;; TODO: probably not general enough

(defn int-texture-from-image-file [local-path remote-url]
  (let [^WritableRaster raster (image/get-writeable-raster local-path remote-url)
        [pixels ^int pw ^int ph] (image/pixels-as-ints raster)
        texture (GL46/glGenTextures)]
    (GL46/glBindTexture GL46/GL_TEXTURE_2D texture)
    (GL46/glTexImage2D
     GL46/GL_TEXTURE_2D 0 GL46/GL_RGBA pw ph 0 GL46/GL_RGB GL46/GL_UNSIGNED_BYTE
     ^ByteBuffer (make-byte-buffer (byte-array (map unchecked-byte pixels))))
    (GL46/glTexParameteri GL46/GL_TEXTURE_2D
                          GL46/GL_TEXTURE_MIN_FILTER
                          GL46/GL_LINEAR)
    (GL46/glTexParameteri GL46/GL_TEXTURE_2D
                          GL46/GL_TEXTURE_MAG_FILTER
                          GL46/GL_LINEAR)
    (GL46/glTexParameteri GL46/GL_TEXTURE_2D
                          GL46/GL_TEXTURE_WRAP_S
                          GL46/GL_REPEAT)
    (GL46/glTexParameteri GL46/GL_TEXTURE_2D
                          GL46/GL_TEXTURE_WRAP_T
                          GL46/GL_REPEAT)
    (GL46/glBindTexture GL46/GL_TEXTURE_2D 0)
    (check-error)
    texture))

(defn float-texture-from-image-file [local-path remote-url]
  (let [^WritableRaster raster (image/get-writeable-raster local-path remote-url)
        [^floats pixels ^int pw ^int ph] (image/pixels-as-floats raster)
        texture (GL46/glGenTextures)]
    (GL46/glBindTexture GL46/GL_TEXTURE_2D texture)
    (GL46/glTexParameteri GL46/GL_TEXTURE_2D
                          GL46/GL_TEXTURE_MIN_FILTER
                          GL46/GL_LINEAR)
    (GL46/glTexParameteri GL46/GL_TEXTURE_2D
                          GL46/GL_TEXTURE_MAG_FILTER
                          GL46/GL_LINEAR)
    (GL46/glTexParameteri GL46/GL_TEXTURE_2D
                          GL46/GL_TEXTURE_WRAP_S
                          GL46/GL_REPEAT)
    (GL46/glTexParameteri GL46/GL_TEXTURE_2D
                          GL46/GL_TEXTURE_WRAP_T
                          GL46/GL_REPEAT)
    (GL46/glTexImage2D
     GL46/GL_TEXTURE_2D 0 GL46/GL_R32F pw ph 0
     GL46/GL_RED GL46/GL_FLOAT pixels)
    (GL46/glBindTexture GL46/GL_TEXTURE_2D 0)
    (check-error)
    ;;[texture (max pw ph)]
    [texture (math/sqrt (+ (* pw pw) (* ph ph)))]
    ))

;;----------------------------------------------------

(defn ^[float float] angles-from-mouse-pos [[window-w window-h]
                                            [origin-x origin-y]
                                            [mouse-x mouse-y]
                                            [theta-origin-x theta-origin-y]]
  (let [dx (- mouse-x origin-x)
        dy (- mouse-y origin-y)
        delta-x (float (* 2.0 PI (/ dx window-w)))
        delta-y (float (* 2.0 PI (/ dy window-h)))]
    [(float (Math/IEEEremainder (+ theta-origin-x delta-x) TwoPI))
     (float (Math/IEEEremainder (+ theta-origin-y delta-y) TwoPI))]))

;;----------------------------------------------------

(defn aspect-ratio [^Integer program
                    [window-w window-h]
                    ^String program-aspect]
  (GL46/glUniform1f
   (GL46/glGetUniformLocation program program-aspect)
   (/ (double window-w) window-h))
  (check-error))

;;----------------------------------------------------

