package com.example.gyro_app

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class Cube {
    private val vertexBuffer: FloatBuffer

    private val vertexShaderCode =
        """
        // 정점 쉐이더
        attribute vec4 vPosition;
        void main() {
            gl_Position = vPosition;
        }
        """.trimIndent()

    private val fragmentShaderCode =
        """
        // 프래그먼트 쉐이더
        precision mediump float;
        void main() {
            gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0); // 빨간색으로 칠함
        }
        """.trimIndent()

    private val program: Int

    init {
        // 정점 데이터 생성
        val vertices = floatArrayOf(
            // 정점 좌표 (x, y, z)
            -0.5f, -0.5f, -0.5f,
            0.5f, -0.5f, -0.5f,
            0.5f, 0.5f, -0.5f,
            -0.5f, 0.5f, -0.5f,
            -0.5f, -0.5f, 0.5f,
            0.5f, -0.5f, 0.5f,
            0.5f, 0.5f, 0.5f,
            -0.5f, 0.5f, 0.5f
        )

        // ByteBuffer를 사용하여 버퍼 생성 및 정점 데이터 복사
        val bb = ByteBuffer.allocateDirect(vertices.size * 4) // 4 bytes per float
        bb.order(ByteOrder.nativeOrder())
        vertexBuffer = bb.asFloatBuffer()
        vertexBuffer.put(vertices)
        vertexBuffer.position(0)

        // 쉐이더 프로그램 초기화
        val vertexShader = MyGLRenderer.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = MyGLRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
    }

    fun draw() {
        GLES20.glUseProgram(program)

        // 정점 좌표 설정
        val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(
            positionHandle, 3,
            GLES20.GL_FLOAT, false,
            12, vertexBuffer
        )

        // 렌더링
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36)

        // 설정 해제
        GLES20.glDisableVertexAttribArray(positionHandle)
    }
}
