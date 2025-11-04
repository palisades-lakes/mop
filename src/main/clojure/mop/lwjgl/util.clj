(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.lwjgl.util

  {:doc     "LWJGL utilities"
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-11-03"}

  (:require [clojure.math :as math]
            [mop.geom.mesh :as mesh]
            [mop.geom.rn :as rn]
            [mop.image.util :as image])

  (:import [java.nio ByteBuffer FloatBuffer IntBuffer]
           [mop.geom.mesh Mesh]
           [org.lwjgl BufferUtils]
           [org.lwjgl.opengl GL46])  )

;;-------------------------------------------------------------------
(def ^Double TwoPI (* 2.0 math/PI))
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
    (check-error)
    (GL46/glCompileShader shader)
    (when (zero? (GL46/glGetShaderi shader GL46/GL_COMPILE_STATUS))
      (throw (RuntimeException.
              (GL46/glGetShaderInfoLog shader 1024))))
    shader))

(defn make-program [paths-and-types]
  (let [program (GL46/glCreateProgram)]
    (doseq [[^Integer type ^String path] paths-and-types]
      (let [shader (make-shader (slurp path) type)]
        (GL46/glAttachShader program shader)
        (check-error)
        (GL46/glDeleteShader shader)
        (check-error)))
    (GL46/glLinkProgram program)
    (when (zero? (GL46/glGetProgrami program GL46/GL_LINK_STATUS))
      (throw (RuntimeException.
              (GL46/glGetProgramInfoLog program 1024))))
    program))

(defn ^FloatBuffer make-float-buffer [^floats data]
  (let [buffer (BufferUtils/createFloatBuffer (count data))]
    (.put buffer data)
    (.flip buffer)
    buffer))

(defn ^IntBuffer make-int-buffer [^ints data]
  (let [buffer (BufferUtils/createIntBuffer (count data))]
    (.put buffer data)
    (.flip buffer)
    buffer))

(defn make-byte-buffer [^bytes data]
  (let [buffer (BufferUtils/createByteBuffer (count data))]
    (.put buffer data)
    (.flip buffer)
    buffer))

;; TODO: not just QuadMesh
(defn setup-vao
  ([^Mesh r3-mesh]
  (let [[coordinates elements] (mesh/coordinates-and-elements r3-mesh)
        vao (GL46/glGenVertexArrays)
        vbo (GL46/glGenBuffers)
        ibo (GL46/glGenBuffers)]
    (GL46/glBindVertexArray vao)
    (check-error)
    (GL46/glBindBuffer GL46/GL_ARRAY_BUFFER vbo)
    (check-error)
    (GL46/glBufferData GL46/GL_ARRAY_BUFFER
                       (make-float-buffer (float-array coordinates))
                       GL46/GL_STATIC_DRAW)
    (check-error)
    (GL46/glBindBuffer GL46/GL_ELEMENT_ARRAY_BUFFER ibo)
    (check-error)
    (GL46/glBufferData GL46/GL_ELEMENT_ARRAY_BUFFER
                       (make-int-buffer (int-array elements) )
                       GL46/GL_STATIC_DRAW)
    (check-error)
    {:nindices (count elements) :vao vao :vbo vbo :ibo ibo})))

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
  (let [[^ints pixels ^int pw ^int ph] (image/pixels-as-ints local-path remote-url)
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
    [texture pw ph]))

(defn float-texture-from-image-file [local-path remote-url]
  (let [[^floats pixels ^int pw ^int ph]
        (image/pixels-as-floats local-path remote-url)
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
    [texture (math/sqrt (+ (* pw pw) (* ph ph)))]))

;;----------------------------------------------------

(defn push-quaternion-coordinates [^Integer program
                                   ^String location
                                   qr]
  (GL46/glUniform4fv
   (GL46/glGetUniformLocation program location)
   (rn/float-coordinates qr)))

;;----------------------------------------------------

(defn aspect-ratio [^Integer program
                    window-wh
                    ^String program-aspect]
  (GL46/glUniform1f
   (GL46/glGetUniformLocation program program-aspect)
   (rn/aspect window-wh))
  (check-error))

;;----------------------------------------------------

