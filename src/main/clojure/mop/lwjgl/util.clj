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

  (:import [java.awt.image WritableRaster]
           [java.nio ByteBuffer FloatBuffer IntBuffer]
           [mop.cmplx.complex QuadComplex]
           [mop.geom.mesh Mesh]
           [org.apache.commons.geometry.euclidean.threed Vector3D Vector3D$Unit]
           [org.apache.commons.geometry.euclidean.threed.rotation QuaternionRotation]
           [org.apache.commons.geometry.euclidean.twod Vector2D]
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

(defmulti uniform
          "Set a GLSL uniform variable, dispatching on the value (usually by type)."
          (fn [_ _ value] (class value)))

(defmethod uniform Float [^Integer program ^String name ^Float value]
  (GL46/glUniform1f (GL46/glGetUniformLocation program name) (float value))
  (check-error))

;; TODO: support double uniforms?
(defmethod uniform Double [^Integer program ^String name ^Double value]
  (GL46/glUniform1f (GL46/glGetUniformLocation program name) (float value))
  (check-error))

(defmethod uniform Integer [^Integer program ^String name ^Integer value]
  (GL46/glUniform1i (GL46/glGetUniformLocation program name) (int value))
  (check-error))

(defmethod uniform Long [^Integer program ^String name ^Long value]
  (GL46/glUniform1i (GL46/glGetUniformLocation program name) (int value))
  (check-error))

(defmethod uniform
  (class (make-array Float/TYPE 0))
  [^Integer program ^String name ^floats value]
  (case (alength value)
    1 (GL46/glUniform1fv (GL46/glGetUniformLocation program name) value)
    2 (GL46/glUniform2fv (GL46/glGetUniformLocation program name) value)
    3 (GL46/glUniform3fv (GL46/glGetUniformLocation program name) value)
    4 (GL46/glUniform4fv (GL46/glGetUniformLocation program name) value))
  (check-error))

(defmethod uniform Vector2D [^Integer program ^String name ^Vector2D value]
  (uniform program name (rn/float-coordinates value))
  (check-error))

(defmethod uniform Vector3D [^Integer program ^String name ^Vector3D value]
  (uniform program name (rn/float-coordinates value))
  (check-error))

(defmethod uniform
  QuaternionRotation
  [^Integer program ^String name ^QuaternionRotation value]
  (uniform program name (rn/float-coordinates value))
  (check-error))

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
      (throw (RuntimeException. (GL46/glGetProgramInfoLog program 1024))))
    program))

(defn use-program [paths-and-types]
  (let [program (make-program paths-and-types)]
    (GL46/glUseProgram program)
    (check-error)
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

(defn ^ByteBuffer make-byte-buffer [^bytes data]
  (let [buffer (BufferUtils/createByteBuffer (count data))]
    (.put buffer data)
    (.flip buffer)
    buffer))

;;-------------------------------------------------------------------

(defmulti texture-parameter
          "Set a GLSL uniform variable, dispatching on the value (usually by type)."
          (fn [_target _name value] (class value)))

(defmethod texture-parameter
  Integer
  [^Integer target ^Integer name ^Integer value]
  (GL46/glTexParameteri target name value)
  (check-error))

;;------------------------------------------------------------------
;; TODO: probably not general enough

(defn- setup-texture [texture-name]
  (GL46/glBindTexture GL46/GL_TEXTURE_2D texture-name)
  (texture-parameter GL46/GL_TEXTURE_2D
                     GL46/GL_TEXTURE_MIN_FILTER
                     GL46/GL_NEAREST)
  (texture-parameter GL46/GL_TEXTURE_2D
                     GL46/GL_TEXTURE_MAG_FILTER
                     GL46/GL_NEAREST)
  (texture-parameter GL46/GL_TEXTURE_2D
                     GL46/GL_TEXTURE_WRAP_S
                     GL46/GL_CLAMP_TO_BORDER)
  (texture-parameter GL46/GL_TEXTURE_2D
                     GL46/GL_TEXTURE_WRAP_T
                     GL46/GL_CLAMP_TO_BORDER)
  (GL46/glBindTexture GL46/GL_TEXTURE_2D 0)
  (check-error))

(defn- setup-color-texture [^WritableRaster image]
  (let [[^ints pixels ^int pw ^int ph] (image/pixels-as-ints image)
        ^ByteBuffer bytes (make-byte-buffer (byte-array (map unchecked-byte pixels)))
        texture-name (GL46/glGenTextures)]
    (setup-texture texture-name)
    (GL46/glBindTexture GL46/GL_TEXTURE_2D texture-name)
    (GL46/glTexImage2D
     GL46/GL_TEXTURE_2D 0
     GL46/GL_RGBA pw ph 0
     GL46/GL_RGB
     GL46/GL_UNSIGNED_BYTE bytes)
    (GL46/glBindTexture GL46/GL_TEXTURE_2D 0)
    (check-error)
    [texture-name pw ph]))

(defn- setup-elevation-texture [^WritableRaster image]
  (let [[^floats pixels ^int pw ^int ph] (image/pixels-as-floats image)
        texture-name (GL46/glGenTextures)]
    (setup-texture texture-name)
    (GL46/glBindTexture GL46/GL_TEXTURE_2D texture-name)
    (check-error)
    (GL46/glTexImage2D
     GL46/GL_TEXTURE_2D 0
     GL46/GL_R32F pw ph 0
     GL46/GL_RED
     GL46/GL_FLOAT pixels)
    (check-error)
    (GL46/glBindTexture GL46/GL_TEXTURE_2D 0)
    (check-error)
    [texture-name (math/sqrt (+ (* pw pw) (* ph ph)))]))

(defn- setup-textures [^Integer program
                       ^WritableRaster color-image
                       ^WritableRaster elevation-image
                       ^Double radius]
  (let [[color-texture _ _] (setup-color-texture color-image)
        [elevation-texture ^Double r] (setup-elevation-texture elevation-image)
        resolution (/ (* (.doubleValue TwoPI) (.doubleValue radius))
                      (.doubleValue r))]

    (println "color-texture: " color-texture)
    (println "elevation-texture: " elevation-texture)

    (uniform program "colorTexture" 0)

    (GL46/glActiveTexture GL46/GL_TEXTURE0)
    (check-error)

    (GL46/glBindTexture GL46/GL_TEXTURE_2D color-texture)
    (check-error)

    (uniform program "elevationTexture" 1)

    (GL46/glActiveTexture GL46/GL_TEXTURE1)
    (check-error)

    (GL46/glBindTexture GL46/GL_TEXTURE_2D elevation-texture)
    (check-error)

    ;; used to calculate texture coordinates
    ;; TODO: replace by texture embedding
    (uniform program "resolution" resolution)

    {:program program
     :color-texture color-texture
     :elevation-texture elevation-texture}))

;;------------------------------------------------------------------

(defn- setup-lighting [^Integer program
                       ^Double ambient
                       ^Double diffuse
                       ^Vector3D light]
  (uniform program "ambient" ambient)
  (uniform program "diffuse" diffuse)
  (uniform program "light" light))

;;------------------------------------------------------------------

(defn- setup-view [^Integer program
                   ^Double fov
                   ^Double radius]
  (uniform program "fov" fov)
  ;; TODO: units?
  (uniform program "distance"  (* (.doubleValue radius) 12.0)))

;;------------------------------------------------------------------

(defn- setup-vertices [^Integer program ^Mesh mesh-r3]

  (let [[coordinates elements] (mesh/coordinates-and-elements mesh-r3)
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
    (let [index (int (GL46/glGetAttribLocation ^int program "point"))
          size (int 3)
          type (int GL46/GL_FLOAT)
          normalized (boolean false)
          stride (int (* 3 Float/BYTES))
          pointer (long (* 0 Float/BYTES))]
      (GL46/glVertexAttribPointer index size type normalized stride pointer))
    (check-error)

    (GL46/glEnableVertexAttribArray 0)
    {:nindices (count elements)
     :vao vao :vbo vbo :ibo ibo}
    ))

;;------------------------------------------------------------------
;; TODO: not just QuadMesh
(defn setup [{:keys [^Mesh mesh-r3
                     ^Mesh mesh-texture
                     ^WritableRaster color-image
                     ^WritableRaster elevation-image
                     ^Double fov
                     ^Double radius
                     ^Double ambient
                     ^Double diffuse
                     ^Vector3D$Unit light
                     ^String vertex-shader
                     ^String fragment-shader]
              :or   {fov     (Math/toRadians 20.0)
                     ambient 0.3
                     diffuse 0.6
                     light   (rn/unit-vector 1.0 1.0 1.0)}}]

  (assert (= (.cmplx mesh-r3) (.cmplx mesh-texture)))

  ;;TODO: one map with accumulated parameters passed to all inner setup fns?
  (println "faces:" (count (.faces ^QuadComplex (.cmplx mesh-r3))))
  (println "vertices:" (count (.vertices ^QuadComplex (.cmplx mesh-r3))))

  (let [program (use-program
                 {GL46/GL_VERTEX_SHADER   vertex-shader
                  GL46/GL_FRAGMENT_SHADER fragment-shader})
        textures (setup-textures program color-image elevation-image radius)
        vertices (setup-vertices program mesh-r3)]
    (setup-lighting program ambient diffuse light)
    (setup-view program fov radius)
    (GL46/glEnable GL46/GL_CULL_FACE)
    (GL46/glClearColor 0.0 0.0 0.0 1.0)
    (merge textures vertices)))

;;----------------------------------------------------

(defn teardown [setup-map]
  (GL46/glBindBuffer GL46/GL_ELEMENT_ARRAY_BUFFER 0)
  (GL46/glDeleteBuffers ^Integer (:ibo setup-map))
  (GL46/glBindBuffer GL46/GL_ARRAY_BUFFER 0)
  (GL46/glDeleteBuffers ^Integer (:vbo setup-map))
  (GL46/glBindVertexArray 0)
  (GL46/glDeleteBuffers ^Integer (:vao setup-map))
  (check-error))

;;----------------------------------------------------

