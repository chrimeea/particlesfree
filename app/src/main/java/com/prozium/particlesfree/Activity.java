package com.prozium.particlesfree;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.preference.PreferenceManager;
import android.view.GestureDetector;
import android.view.MotionEvent;

public class Activity extends android.app.Activity implements SensorEventListener {

    Stage stage;
    SensorManager mSensorManager;

    @Override
    public final void onAccuracyChanged(final Sensor sensor, final int accuracy) {
    }

    @Override
    public final void onSensorChanged(final SensorEvent event) {
        if (stage.motion) {
            stage.setPinch(event.values[0], -event.values[1]);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (stage != null) {
            if (stage.timer != null) {
                stage.timer.shutdown();
            }
            if (stage.view != null) {
                stage.view.onPause();
            }
        }
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getActionBar().hide();
        if (stage == null) {
            stage = new Stage(this);
            PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(stage);
            stage.view = new SurfaceView(this, stage);
            setContentView(stage.view);
            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        } else {
            stage.view.onResume();
        }
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    public class SurfaceView extends GLSurfaceView {

        final GestureDetector fling;

        SurfaceView(final Context context, final Stage stage) {
            super(context);
            setEGLContextClientVersion(2);
            setEGLConfigChooser(8, 8, 8, 8, 0, 0);
            //setPreserveEGLContextOnPause(true);
            setRenderer(new com.prozium.particlesfree.Renderer(context, stage));
            setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
            fling = new GestureDetector(context, new FlingListener());
        }

        @Override
        public boolean onTouchEvent(final MotionEvent event) {
            fling.onTouchEvent(event);
            return true;
        }

        class FlingListener extends GestureDetector.SimpleOnGestureListener {

            @Override
            public boolean onDown(final MotionEvent event) {
                return true;
            }

            @Override
            public boolean onFling(final MotionEvent event1, final MotionEvent event2, final float velocityX, final float velocityY) {
                getContext().startActivity(new Intent(getContext(), SettingsActivity.class));
                return true;
            }
        }
    }
}
