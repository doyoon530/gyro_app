import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.gyro_app.Cube
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MyGLRenderer : GLSurfaceView.Renderer {

    // 모델 변환 행렬
    private val rotationMatrix = FloatArray(16)

    // X, Y, Z 축 회전 각도
    private var rotateAngleX = 0.0f
    private var rotateAngleY = 0.0f
    private var rotateAngleZ = 0.0f

    private val cube: Cube = Cube() // Cube 클래스 인스턴스 생성

    companion object {
        fun loadShader(type: Int, shaderCode: String): Int {
            val shader = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
            return shader
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // OpenGL 환경 초기화
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        // 추가 초기화 및 쉐이더 프로그램 설정
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        // 화면 크기 변경 시 호출
        GLES20.glViewport(0, 0, width, height)
        // 투영 매트릭스 설정 등 추가 설정
    }

    override fun onDrawFrame(gl: GL10?) {
        // 프레임마다 렌더링 수행
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        // 모델 변환 행렬 초기화
        Matrix.setIdentityM(rotationMatrix, 0)

        // X, Y, Z 축 주위의 회전을 적용
        Matrix.rotateM(rotationMatrix, 0, rotateAngleX, 1.0f, 0.0f, 0.0f)
        Matrix.rotateM(rotationMatrix, 0, rotateAngleY, 0.0f, 1.0f, 0.0f)
        Matrix.rotateM(rotationMatrix, 0, rotateAngleZ, 0.0f, 0.0f, 1.0f)

        // 모델 변환 행렬을 OpenGL에 전달
        // 이 부분은 실제 3D 객체의 렌더링 부분에 따라 다를 수 있습니다.

        // 정육면체를 그립니다.
        cube.draw()
    }

    // X, Y, Z 축 주위의 회전을 설정하는 메서드
    fun setRotationAngles(angleX: Float, angleY: Float, angleZ: Float) {
        rotateAngleX = angleX
        rotateAngleY = angleY
        rotateAngleZ = angleZ
    }

    // 추가적인 OpenGL 렌더링 로직 및 메서드를 구현할 수 있습니다.
}
