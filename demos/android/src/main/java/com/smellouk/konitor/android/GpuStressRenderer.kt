package com.smellouk.konitor.android

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

internal class GpuStressRenderer : android.opengl.GLSurfaceView.Renderer {

    private var program = 0
    private var posHandle = 0
    private var timeHandle = 0
    private val vbo = IntArray(1)
    private var frame = 0

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        GLES20.glClearColor(BG_R, BG_G, BG_B, 1f)
        program = buildProgram(VERTEX_SRC, FRAGMENT_SRC)
        posHandle  = GLES20.glGetAttribLocation(program, ATTR_POS)
        timeHandle = GLES20.glGetUniformLocation(program, UNIFORM_TIME)
        GLES20.glGenBuffers(1, vbo, 0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[0])
        val buf = ByteBuffer
            .allocateDirect(QUAD_VERTICES.size * FLOAT_BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .also { it.put(QUAD_VERTICES); it.position(0) }
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, QUAD_VERTICES.size * FLOAT_BYTES, buf, GLES20.GL_STATIC_DRAW)
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[0])
        GLES20.glEnableVertexAttribArray(posHandle)
        GLES20.glVertexAttribPointer(posHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, 0)
        GLES20.glUniform1f(timeHandle, ++frame * TIME_STEP)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, QUAD_VERTEX_COUNT)
        GLES20.glDisableVertexAttribArray(posHandle)
    }

    private fun buildProgram(vs: String, fs: String): Int {
        val vShader = compileShader(GLES20.GL_VERTEX_SHADER, vs)
        val fShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fs)
        return GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vShader)
            GLES20.glAttachShader(it, fShader)
            GLES20.glLinkProgram(it)
        }
    }

    private fun compileShader(type: Int, src: String): Int =
        GLES20.glCreateShader(type).also {
            GLES20.glShaderSource(it, src)
            GLES20.glCompileShader(it)
        }

    private companion object {
        const val FLOAT_BYTES      = 4
        const val COORDS_PER_VERTEX = 2
        const val QUAD_VERTEX_COUNT = 6
        const val TIME_STEP         = 0.02f
        const val BG_R = 0.05f
        const val BG_G = 0.05f
        const val BG_B = 0.10f

        const val ATTR_POS      = "aPos"
        const val UNIFORM_TIME  = "uTime"

        // Two triangles covering the full NDC viewport (-1,-1)..(1,1)
        val QUAD_VERTICES = floatArrayOf(
            -1f, -1f,   1f, -1f,  -1f,  1f,
             1f, -1f,   1f,  1f,  -1f,  1f
        )

        // Passes UV coordinates to fragment shader
        const val VERTEX_SRC = """
attribute vec2 aPos;
varying vec2 vUv;
void main() {
    gl_Position = vec4(aPos, 0.0, 1.0);
    vUv = aPos * 0.5 + 0.5;
}"""

        // Data-dependent iteration (Mandelbrot-style) prevents compiler from
        // collapsing the loop — each iteration reads the result of the previous one.
        // 64 iterations × 2 mul + 2 add + 1 length per pixel saturates the GPU
        // fragment pipeline and forces the Mali DVFS governor to scale up.
        const val FRAGMENT_SRC = """
precision mediump float;
varying vec2 vUv;
uniform float uTime;
void main() {
    vec2 z = vUv * 4.0 - 2.0;
    vec2 c = vec2(cos(uTime * 0.13) * 0.7885, sin(uTime * 0.11) * 0.7885);
    float n = 0.0;
    for (int i = 0; i < 64; i++) {
        if (dot(z, z) > 4.0) { n = float(i); break; }
        z = vec2(z.x * z.x - z.y * z.y + c.x, 2.0 * z.x * z.y + c.y);
    }
    float t = n / 64.0;
    gl_FragColor = vec4(
        0.5 + 0.5 * sin(t * 6.28 + uTime),
        0.5 + 0.5 * sin(t * 6.28 + uTime + 2.09),
        0.5 + 0.5 * sin(t * 6.28 + uTime + 4.19),
        1.0);
}"""
    }
}
