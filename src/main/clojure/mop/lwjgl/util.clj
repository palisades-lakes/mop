(ns mop.lwjgl.util

  {:doc "LWJGL utilities"
   :author "palisades dot lakes at gmail dot com"
   :version "2025-10-07"}

  (:import (java.nio ByteBuffer FloatBuffer IntBuffer)
           (org.lwjgl BufferUtils)
           (org.lwjgl.opengl GL15 GL20 GL30)))

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
