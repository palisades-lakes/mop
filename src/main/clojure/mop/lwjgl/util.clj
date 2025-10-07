(ns mop.lwjgl.util

  {:doc     "LWJGL utilities"
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-10-07"}

  (:require [mop.image.util :as image])
  (:import [java.awt.image WritableRaster]
           (java.nio ByteBuffer FloatBuffer IntBuffer)
           (org.lwjgl BufferUtils)
           (org.lwjgl.opengl GL11 GL15 GL20 GL30)))

(defn make-shader [source shader-type]
  (let [shader (GL20/glCreateShader shader-type)]
    (^[int CharSequence] GL20/glShaderSource shader source)
    (GL20/glCompileShader shader)
    (when (zero? (GL20/glGetShaderi shader GL20/GL_COMPILE_STATUS))
      (throw (Exception. (GL20/glGetShaderInfoLog shader 1024))))
    shader))

(defn make-program [& shaders]
  (let [program (GL20/glCreateProgram)]
    (doseq [shader shaders]
      (GL20/glAttachShader program shader)
      (GL20/glDeleteShader shader))
    (GL20/glLinkProgram program)
    (when (zero? (GL20/glGetProgrami program GL20/GL_LINK_STATUS))
      (throw (Exception. (GL20/glGetProgramInfoLog program 1024))))
    program))

(defmacro def-make-buffer [method create-buffer return-type]
  `(defn ~(with-meta method {:tag return-type}) [data#]
     (let [buffer# (~create-buffer (count data#))]
       (.put buffer# data#)
       (.flip buffer#)
       buffer#)))

(def-make-buffer make-float-buffer
                 BufferUtils/createFloatBuffer
                 'FloatBuffer)
(def-make-buffer make-int-buffer
                 BufferUtils/createIntBuffer
                 'IntBuffer)
(def-make-buffer make-byte-buffer
                 BufferUtils/createByteBuffer
                 'ByteBuffer)

(defn setup-vao [vertices indices]
  (let [vao (GL30/glGenVertexArrays)
        vbo (GL15/glGenBuffers)
        ibo (GL15/glGenBuffers)]
    (GL30/glBindVertexArray vao)
    (GL15/glBindBuffer GL15/GL_ARRAY_BUFFER vbo)
    (^[int FloatBuffer int] GL15/glBufferData
     GL15/GL_ARRAY_BUFFER (make-float-buffer vertices)
     GL15/GL_STATIC_DRAW)
    (GL15/glBindBuffer GL15/GL_ELEMENT_ARRAY_BUFFER ibo)
    (^[int IntBuffer int] GL15/glBufferData
     GL15/GL_ELEMENT_ARRAY_BUFFER (make-int-buffer indices)
     GL15/GL_STATIC_DRAW)
    {:vao vao :vbo vbo :ibo ibo}))

(defn teardown-vao [{:keys [vao vbo ibo]}]
  (GL15/glBindBuffer GL15/GL_ELEMENT_ARRAY_BUFFER 0)
  (^[int] GL15/glDeleteBuffers ibo)
  (GL15/glBindBuffer GL15/GL_ARRAY_BUFFER 0)
  (^[int] GL15/glDeleteBuffers vbo)
  (GL30/glBindVertexArray 0)
  (^[int] GL15/glDeleteBuffers vao))

;;------------------------------------------------------------------
;; TODO: probably not general enough

(defn int-texture-from-image-file [local-path remote-url]
  (let [^WritableRaster raster (image/get-writeable-raster local-path remote-url)
        [pixels pw ph] (image/pixels-as-ints raster)
        texture (GL11/glGenTextures)]
    (println "W" pw "H" ph "p" (aget pixels (/ (* pw ph) 2)))
    (GL11/glBindTexture GL11/GL_TEXTURE_2D texture)
    (^[int int int int int int int int ByteBuffer]
      GL11/glTexImage2D
     GL11/GL_TEXTURE_2D 0 GL11/GL_RGBA pw ph 0 GL11/GL_RGB GL11/GL_UNSIGNED_BYTE
     (make-byte-buffer (byte-array (map unchecked-byte pixels))))
    (GL11/glTexParameteri GL11/GL_TEXTURE_2D
                          GL11/GL_TEXTURE_MIN_FILTER
                          GL11/GL_LINEAR)
    (GL11/glTexParameteri GL11/GL_TEXTURE_2D
                          GL11/GL_TEXTURE_MAG_FILTER
                          GL11/GL_LINEAR)
    (GL11/glTexParameteri GL11/GL_TEXTURE_2D
                          GL11/GL_TEXTURE_WRAP_S
                          GL11/GL_REPEAT)
    (GL11/glTexParameteri GL11/GL_TEXTURE_2D
                          GL11/GL_TEXTURE_WRAP_T
                          GL11/GL_REPEAT)
    (GL11/glBindTexture GL11/GL_TEXTURE_2D 0)
    [texture pw ph]))

(defn float-texture-from-image-file [local-path remote-url]
  (let [^WritableRaster raster (image/get-writeable-raster local-path remote-url)
        [pixels pw ph] (image/pixels-as-floats raster)
        texture (GL11/glGenTextures)]
    (println "W" pw "H" ph "p" (aget pixels (/ (* pw ph) 2)))
    (GL11/glBindTexture GL11/GL_TEXTURE_2D texture)
    (GL11/glTexParameteri GL11/GL_TEXTURE_2D
                          GL11/GL_TEXTURE_MIN_FILTER
                          GL11/GL_LINEAR)
    (GL11/glTexParameteri GL11/GL_TEXTURE_2D
                          GL11/GL_TEXTURE_MAG_FILTER
                          GL11/GL_LINEAR)
    (GL11/glTexParameteri GL11/GL_TEXTURE_2D
                          GL11/GL_TEXTURE_WRAP_S
                          GL11/GL_REPEAT)
    (GL11/glTexParameteri GL11/GL_TEXTURE_2D
                          GL11/GL_TEXTURE_WRAP_T
                          GL11/GL_REPEAT)
    (^[int int int int int int int int float/1]
      GL11/glTexImage2D
     GL11/GL_TEXTURE_2D 0 GL30/GL_R32F pw ph 0
     GL11/GL_RED GL11/GL_FLOAT pixels)
    (GL11/glBindTexture GL11/GL_TEXTURE_2D 0)
    [texture pw ph]))