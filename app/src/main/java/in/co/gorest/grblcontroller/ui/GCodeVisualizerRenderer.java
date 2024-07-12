package in.co.gorest.grblcontroller.ui;

import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import in.co.gorest.grblcontroller.model.Bounds;
import in.co.gorest.grblcontroller.util.SimpleGcodeParser;

public class GCodeVisualizerRenderer implements GLSurfaceView.Renderer,
        SimpleGcodeParser.GcodeParserListener {

    // allocate 256MB RAM for vertices
    private static final int NUM_VERTICES = (256 * 1024 * 1024) / 12;

    private final float[] mProjectionMatrix = new float[16];
    private int mMVPMatrixHandle;
    private int mPositionHandle;
    private int mColorHandle;
    private int defaultProgram;
    private int zeroMVPMatrixHandle;
    private int zeroPositionHandle;
    private int zeroProgram;
    private int viewportWidth = 100;
    private int viewportHeight = 100;

    // X, Y, COLOR(<0.5 for rapid, >= 0.5 for cutting)
    private final FloatBuffer vertices;
    private int numVertices = 0;

    private final FloatBuffer axisBuffer;

    private static GCodeVisualizerRenderer instance = null;

    public static GCodeVisualizerRenderer getInstance() {
        if (instance == null) {
            instance = new GCodeVisualizerRenderer();
        }
        return instance;
    }

    private GCodeVisualizerRenderer() {
        vertices = ByteBuffer.allocateDirect(NUM_VERTICES * 3 * 4).order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        axisBuffer = ByteBuffer.allocateDirect(4 * 2 * 4).order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        axisBuffer.put(new float[]{0.0f, 10.0f, 0.0f, -10.0f, 10.0f, 0.0f, -10.0f, 0.0f,})
                .position(0);
    }

    public void setBounds(Bounds bounds) {
        // make sure the zero point is visible
        float xMin = Math.min((float) bounds.getXMin(), -10.0f);
        float xMax = Math.max((float) bounds.getXMax(), 10.0f);
        float yMin = Math.min((float) bounds.getYMin(), -10.0f);
        float yMax = Math.max((float) bounds.getYMax(), 10.0f);

        float drawingWidth = xMax - xMin;
        float drawingHeight = yMax - yMin;

        float viewportAspect = viewportWidth / viewportHeight;
        float drawingAspect = drawingWidth / drawingHeight;
        float factor = (viewportAspect / drawingAspect);

        if (drawingAspect < viewportAspect) {
            // drawing is limited by height
            xMin = xMin * factor;
            xMax = xMax * factor;
        } else {
            // drawing is limited by width
            yMin = yMin * factor;
            yMax = yMax * factor;
        }

        Matrix.orthoM(mProjectionMatrix, 0, xMin, xMax, yMin, yMax, -10, 10);
    }

    public void resetVertices() {
        numVertices = 0;
        vertices.clear();
    }

    private void addVertex(double x, double y, double z, boolean rapid) {
        if (numVertices >= NUM_VERTICES) {
            return;
        }

        int index = numVertices * 3;
        vertices.put(index, (float) x);
        vertices.put(index + 1, (float) y);
        vertices.put(index + 2, rapid ? 0.0f : 1.0f);
        numVertices++;
    }

    @Override
    public void move(double x, double y, double z) {
        addVertex(x, y, z, false);
    }

    @Override
    public void rapidMove(double x, double y, double z) {
        addVertex(x, y, z, true);
    }

    private void compileZeroShaders() {
        final String vertexShader =
                "#version 300 es\n" +
                        "uniform mat4 u_MVPMatrix;\n" +
                        "in vec2 a_Position;\n" +
                        "flat out vec4 v_Color;\n" +
                        "void main()\n" +
                        "{\n" +
                        "  v_Color = vec4(1.0, 0.4, 0.4, 1.0);\n" +
                        "   gl_Position = u_MVPMatrix * vec4(a_Position.x, a_Position.y, 0.0, 1"
                        + ".0);\n"
                        +
                        "}\n";

        final String fragmentShader =
                "#version 300 es\n" +
                        "precision mediump float;\n" +
                        "flat in vec4 v_Color;\n" +
                        "out vec4 outColor;\n" +
                        "void main()\n" +
                        "{\n" +
                        "   outColor = v_Color;\n" +
                        "}\n";

        int vertexShaderHandle = GLES30.glCreateShader(GLES30.GL_VERTEX_SHADER);
        GLES30.glShaderSource(vertexShaderHandle, vertexShader);
        GLES30.glCompileShader(vertexShaderHandle);
        int[] compileStatus = new int[1];
        GLES30.glGetShaderiv(vertexShaderHandle, GLES30.GL_COMPILE_STATUS, compileStatus, 0);
        if (compileStatus[0] == 0) {
            //GLES30.glDeleteShader(vertexShaderHandle);
            throw new RuntimeException(
                    "Error compiling vertex shader: " + GLES30.glGetShaderInfoLog(
                            vertexShaderHandle));
        }

        int fragmentShaderHandle = GLES30.glCreateShader(GLES30.GL_FRAGMENT_SHADER);
        GLES30.glShaderSource(fragmentShaderHandle, fragmentShader);
        GLES30.glCompileShader(fragmentShaderHandle);
        GLES30.glGetShaderiv(fragmentShaderHandle, GLES30.GL_COMPILE_STATUS, compileStatus, 0);
        if (compileStatus[0] == 0) {
            //GLES30.glDeleteShader(fragmentShaderHandle);
            throw new RuntimeException(
                    "Error compiling fragment shader: " + GLES30.glGetShaderInfoLog(
                            fragmentShaderHandle));
        }

        zeroProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(zeroProgram, vertexShaderHandle);
        GLES30.glAttachShader(zeroProgram, fragmentShaderHandle);

        GLES30.glBindAttribLocation(zeroProgram, 0, "a_Position");

        GLES30.glLinkProgram(zeroProgram);

        zeroMVPMatrixHandle = GLES30.glGetUniformLocation(zeroProgram, "u_MVPMatrix");
        zeroPositionHandle = GLES30.glGetAttribLocation(zeroProgram, "a_Position");
    }

    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        final String vertexShader =
                "#version 300 es\n" +
                        "uniform mat4 u_MVPMatrix;\n" +
                        "in vec2 a_Position;\n" +
                        "in float a_Color;\n" +
                        "flat out vec4 v_Color;\n" +
                        "void main()\n" +
                        "{\n" +
                        "  if (a_Color < 0.5) {\n" +
                        "    v_Color = vec4(0.6, 0.6, 0.6, 1.0);\n" +
                        "  } else {\n" +
                        "    v_Color = vec4(0.0, 1.0, 0.0, 1.0);\n" +
                        "  }\n" +
                        "   gl_Position = u_MVPMatrix * vec4(a_Position.x, a_Position.y, 0.0, 1"
                        + ".0);\n"
                        +
                        "}\n";

        final String fragmentShader =
                "#version 300 es\n" +
                        "precision mediump float;\n" +
                        "flat in vec4 v_Color;\n" +
                        "out vec4 outColor;\n" +
                        "void main()\n" +
                        "{\n" +
                        "   outColor = v_Color;\n" +
                        "}\n";

        int vertexShaderHandle = GLES30.glCreateShader(GLES30.GL_VERTEX_SHADER);
        GLES30.glShaderSource(vertexShaderHandle, vertexShader);
        GLES30.glCompileShader(vertexShaderHandle);
        int[] compileStatus = new int[1];
        GLES30.glGetShaderiv(vertexShaderHandle, GLES30.GL_COMPILE_STATUS, compileStatus, 0);
        if (compileStatus[0] == 0) {
            //GLES30.glDeleteShader(vertexShaderHandle);
            throw new RuntimeException(
                    "Error compiling vertex shader: " + GLES30.glGetShaderInfoLog(
                            vertexShaderHandle));
        }

        int fragmentShaderHandle = GLES30.glCreateShader(GLES30.GL_FRAGMENT_SHADER);
        GLES30.glShaderSource(fragmentShaderHandle, fragmentShader);
        GLES30.glCompileShader(fragmentShaderHandle);
        GLES30.glGetShaderiv(fragmentShaderHandle, GLES30.GL_COMPILE_STATUS, compileStatus, 0);
        if (compileStatus[0] == 0) {
            //GLES30.glDeleteShader(fragmentShaderHandle);
            throw new RuntimeException(
                    "Error compiling fragment shader: " + GLES30.glGetShaderInfoLog(
                            fragmentShaderHandle));
        }

        defaultProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(defaultProgram, vertexShaderHandle);
        GLES30.glAttachShader(defaultProgram, fragmentShaderHandle);

        GLES30.glBindAttribLocation(defaultProgram, 0, "a_Position");
        GLES30.glBindAttribLocation(defaultProgram, 1, "a_Color");

        GLES30.glLinkProgram(defaultProgram);

        mMVPMatrixHandle = GLES30.glGetUniformLocation(defaultProgram, "u_MVPMatrix");
        mPositionHandle = GLES30.glGetAttribLocation(defaultProgram, "a_Position");
        mColorHandle = GLES30.glGetAttribLocation(defaultProgram, "a_Color");

        compileZeroShaders();
    }

    public void onSurfaceChanged(GL10 unused, int width, int height) {
        GLES30.glViewport(0, 0, width, height);
        viewportWidth = width;
        viewportHeight = height;
    }

    public void onDrawFrame(GL10 unused) {
        GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT | GLES30.GL_COLOR_BUFFER_BIT);

        GLES30.glUseProgram(defaultProgram);

        GLES30.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mProjectionMatrix, 0);

        vertices.position(0);
        GLES30.glVertexAttribPointer(mPositionHandle, 2, GLES30.GL_FLOAT, false, 3 * 4, vertices);
        GLES30.glEnableVertexAttribArray(mPositionHandle);

        vertices.position(2);
        GLES30.glVertexAttribPointer(mColorHandle, 1, GLES30.GL_FLOAT, false, 3 * 4, vertices);
        GLES30.glEnableVertexAttribArray(mColorHandle);

        GLES30.glDrawArrays(GLES30.GL_LINE_STRIP, 0, numVertices);

        GLES30.glDisableVertexAttribArray(mColorHandle);
        GLES30.glDisableVertexAttribArray(mPositionHandle);

        GLES30.glUseProgram(zeroProgram);

        GLES30.glUniformMatrix4fv(zeroMVPMatrixHandle, 1, false, mProjectionMatrix, 0);

        axisBuffer.position(0);
        GLES30.glVertexAttribPointer(zeroPositionHandle, 2, GLES30.GL_FLOAT, false, 2 * 4,
                axisBuffer);
        GLES30.glEnableVertexAttribArray(zeroPositionHandle);

        GLES30.glDrawArrays(GLES30.GL_LINES, 0, 4);

        GLES30.glDisableVertexAttribArray(zeroPositionHandle);
    }
}
