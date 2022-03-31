package com.prozium.particlesfree;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Scanner;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by cristian on 22.05.2016.
 */
public class Renderer implements GLSurfaceView.Renderer {

    static final int FLOAT_SIZE = 4;
    final float[] mMVPMatrix = new float[16], mProjectionMatrix = new float[16], mViewMatrix = new float[16];
    FloatBuffer vertexBuffer;
    final int texture[] = new int[4], framebuffer[] = new int[1], buffer[] = new int[3];
    int mMVPMatrixHandle, program1, program2, program3, program4, positionHandle1, positionHandle2, positionHandle3, positionHandle4;
    int activeTexture, extraHandle, fps;
    final String vertexShaderCode1, vertexShaderCode2, fragmentShaderCode1, fragmentShaderCode2, fragmentShaderCode3, fragmentShaderCode4;
    final Stage stage;
    Bitmap particleBitmap;
    final Bitmap bitmap = Bitmap.createBitmap(100, 50, Bitmap.Config.ARGB_4444);
    final Canvas canvas = new Canvas(bitmap);
    final Paint textPaint = new Paint();
    long t1, t0 = System.currentTimeMillis();
    String lastfps;

    Renderer(final Context context, final Stage stage) {
        final Resources r = context.getResources();
        vertexShaderCode1 = loadTextFile(r.openRawResource(R.raw.vertex1));
        fragmentShaderCode1 = loadTextFile(r.openRawResource(R.raw.fragment1));
        vertexShaderCode2 = loadTextFile(r.openRawResource(R.raw.vertex2));
        fragmentShaderCode2 = loadTextFile(r.openRawResource(R.raw.fragment2));
        fragmentShaderCode3 = loadTextFile(r.openRawResource(R.raw.fragment3));
        fragmentShaderCode4 = loadTextFile(r.openRawResource(R.raw.fragment4));
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        particleBitmap = BitmapFactory.decodeResource(r, R.drawable.circle, options);
        this.stage = stage;
        textPaint.setColor(Color.argb(255, 255, 255, 255));
        textPaint.setAntiAlias(true);
    }

    @Override
    public void onSurfaceCreated(final GL10 unused, final EGLConfig config) {
        program1 = GLES20.glCreateProgram();
        GLES20.glAttachShader(program1, loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode1));
        GLES20.glAttachShader(program1, loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode1));
        GLES20.glLinkProgram(program1);
        GLES20.glUseProgram(program1);
        extraHandle = GLES20.glGetAttribLocation(program1, "extra");
        positionHandle1 = GLES20.glGetAttribLocation(program1, "position");
        mMVPMatrixHandle = GLES20.glGetUniformLocation(program1, "uMVPMatrix");
        program2 = GLES20.glCreateProgram();
        GLES20.glAttachShader(program2, loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode2));
        GLES20.glAttachShader(program2, loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode2));
        GLES20.glLinkProgram(program2);
        GLES20.glUseProgram(program2);
        positionHandle2 = GLES20.glGetAttribLocation(program2, "position");
        program3 = GLES20.glCreateProgram();
        GLES20.glAttachShader(program3, loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode2));
        GLES20.glAttachShader(program3, loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode3));
        GLES20.glLinkProgram(program3);
        GLES20.glUseProgram(program3);
        positionHandle3 = GLES20.glGetAttribLocation(program3, "position");
        program4 = GLES20.glCreateProgram();
        GLES20.glAttachShader(program4, loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode2));
        GLES20.glAttachShader(program4, loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode4));
        GLES20.glLinkProgram(program4);
        GLES20.glUseProgram(program4);
        positionHandle4 = GLES20.glGetAttribLocation(program4, "position");
        Matrix.setLookAtM(mViewMatrix, 0, 0f, 0f, -3f, 0f, 0f, 0f, 0f, 1f, 0f);
        GLES20.glGenBuffers(buffer.length, buffer, 0);
        GLES20.glGenTextures(texture.length - 2, texture, 2);
        GLES20.glGenFramebuffers(framebuffer.length, framebuffer, 0);
        final FloatBuffer tempBuffer = ByteBuffer.allocateDirect(6 * 4 * FLOAT_SIZE).order(ByteOrder.nativeOrder()).asFloatBuffer();
        tempBuffer.put(new float[] {
                -1f,  1f,  0f, 1f,
                -1f, -1f,  0f, 0f,
                1f, -1f,  1f, 0f,
                -1f,  1f,  0f, 1f,
                1f, -1f,  1f, 0f,
                1f,  1f,  1f, 1f});
        tempBuffer.position(0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffer[1]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 6 * 4 * FLOAT_SIZE, tempBuffer, GLES20.GL_STATIC_DRAW);
        tempBuffer.put(new float[] {
                -1f,  1f,  0f, 0f,
                -1f, 0.90f,  0f, 1f,
                -0.80f, 0.90f,  1f, 1f,
                -1f,  1f,  0f, 0f,
                -0.80f, 0.90f,  1f, 1f,
                -0.80f,  1f,  1f, 0f});
        tempBuffer.position(0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffer[2]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 6 * 4 * FLOAT_SIZE, tempBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        loadTextureFromBitmap(particleBitmap, texture[3]);
    }

    @Override
    public void onDrawFrame(final GL10 unused) {
        GLES20.glViewport(0, 0, stage.width, stage.height);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebuffer[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, texture[activeTexture], 0);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        if (stage.renderMode == 0 && stage.trail != 1f) {
            GLES20.glUseProgram(program3);
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffer[1]);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[1 - activeTexture]);
            GLES20.glEnableVertexAttribArray(positionHandle3);
            GLES20.glVertexAttribPointer(positionHandle3, 4, GLES20.GL_FLOAT, false, 0, 0);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
        }
        GLES20.glUseProgram(program1);
        GLES20.glEnable(GLES20.GL_BLEND);
        final int t = stage.getTotalObjects();
        if (vertexBuffer != null && t > 0) {
            Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
            GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[3]);
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffer[0]);
            GLES20.glEnableVertexAttribArray(positionHandle1);
            GLES20.glVertexAttribPointer(positionHandle1, 4, GLES20.GL_FLOAT, false, 0, 0);
            GLES20.glEnableVertexAttribArray(extraHandle);
            GLES20.glVertexAttribPointer(extraHandle, 1, GLES20.GL_FLOAT, false, 0, 6 * t * 4 * FLOAT_SIZE);
            vertexBuffer.position(0);
            vertexBuffer.put(stage.quads);
            vertexBuffer.put(stage.extra);
            vertexBuffer.position(0);
            GLES20.glBufferData(
                    GLES20.GL_ARRAY_BUFFER,
                    6 * t * 5 * FLOAT_SIZE,
                    vertexBuffer,
                    GLES20.GL_DYNAMIC_DRAW);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6 * t);
        }
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glUseProgram(stage.renderMode == 0 ? program2 : program4);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffer[1]);
        final int ph = stage.renderMode == 0 ? positionHandle2 : positionHandle4;
        GLES20.glEnableVertexAttribArray(ph);
        GLES20.glVertexAttribPointer(ph, 4, GLES20.GL_FLOAT, false, 0, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[activeTexture]);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
        if (stage.fps) {
            t1 = System.currentTimeMillis();
            if (t1 - t0 > 1000) {
                t0 = t1;
                lastfps = String.valueOf(fps) + " FPS";
                fps = 0;
            }
            fps++;
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glUseProgram(program2);
            drawText(String.valueOf(lastfps));
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffer[2]);
            GLES20.glEnableVertexAttribArray(positionHandle2);
            GLES20.glVertexAttribPointer(ph, 4, GLES20.GL_FLOAT, false, 0, 0);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
            GLES20.glDisable(GLES20.GL_BLEND);
        }
        activeTexture = 1 - activeTexture;
    }

    @Override
    public void onSurfaceChanged(final GL10 unused, final int width, final int height) {
        if (stage.timer == null || stage.timer.isShutdown() || stage.width != width || stage.height != height) {
            float ratio;
            if (width < height) {
                ratio = (float) width / height;
                Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 3, 100);
            } else {
                ratio = (float) height / width;
                Matrix.frustumM(mProjectionMatrix, 0, -1, 1, -ratio, ratio, 3, 100);
            }
            GLES20.glDeleteTextures(2, texture, 0);
            GLES20.glGenTextures(2, texture, 0);
            for (int i = 0; i < 2; i++) {
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[i]);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGB, width, height, 0, GLES20.GL_RGB, GLES20.GL_UNSIGNED_BYTE, null);
            }
            GLES20.glUseProgram(program1);
            GLES20.glUniform3f(GLES20.glGetUniformLocation(program1, "begin"), stage.begin[0], stage.begin[1], stage.begin[2]);
            GLES20.glUniform3f(GLES20.glGetUniformLocation(program1, "end"), stage.end[0], stage.end[1], stage.end[2]);
            GLES20.glUseProgram(program2);
            GLES20.glUniform1f(GLES20.glGetUniformLocation(program2, "offset"), stage.offset);
            GLES20.glUniform1f(GLES20.glGetUniformLocation(program2, "glow"), stage.glow);
            GLES20.glUseProgram(program3);
            GLES20.glUniform1f(GLES20.glGetUniformLocation(program3, "strength"), stage.trail);
            GLES20.glUseProgram(program4);
            GLES20.glUniform1f(GLES20.glGetUniformLocation(program4, "glow"), stage.glow);
            GLES20.glUniform1i(GLES20.glGetUniformLocation(program4, "rendermode"), stage.renderMode);
            vertexBuffer = ByteBuffer.allocateDirect(6 * stage.getTotalObjects() * 5 * FLOAT_SIZE).order(ByteOrder.nativeOrder()).asFloatBuffer();
            stage.update(width, height);
        }
    }

    int loadShader(final int type, final String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    String loadTextFile(final InputStream is) {
        return new Scanner(is).useDelimiter("\\A").next();
    }

    void drawText(final String text) {
        bitmap.eraseColor(Color.TRANSPARENT);
        textPaint.setTextSize(30f);
        canvas.drawText(text, 5f, 50f, textPaint);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[2]);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    }

    void loadTextureFromBitmap(final Bitmap bitmap, final int textureId) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    }
}
