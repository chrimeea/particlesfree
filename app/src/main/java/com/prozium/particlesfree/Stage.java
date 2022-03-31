package com.prozium.particlesfree;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.preference.PreferenceManager;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.Float2;
import android.renderscript.RenderScript;

import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by cristian on 22.05.2016.
 */
public class Stage implements SharedPreferences.OnSharedPreferenceChangeListener {

    Level level;
    ScheduledExecutorService timer;
    float radius, attenuation, x, y, offset, trail, glow = 1f;
    final float[] sForce = new float[4], sGravity = new float[4], sHorizon = new float[4], sGauss = new float[4];
    final int [] sFormula = new int[4], totalObjects = new int[2];
    float[] quads, extra, begin, end;
    int scaleFactor, width, height, renderMode, speed;
    GLSurfaceView view;
    boolean fps, motion, bounce, solid;
    Context context;

    Stage(final Context context) {
        this.context = context;
        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        if (!pref.contains(context.getString(R.string.pref_key_trail))) {
            resetToPredefined(context, pref.edit(), "7");
        }
        for (String key : pref.getAll().keySet()) {
            onSharedPreferenceChanged(pref, key);
        }
    }

    void setPinch(final float x, final float y) {
        this.x = x;
        this.y = y;
    }

    int getTotalObjects() {
        return totalObjects[0] + totalObjects[1];
    }

    void update(final int width, final int height) {
        if (view.getVisibility() == GLSurfaceView.VISIBLE) {
            if (timer != null && !timer.isShutdown() && (width != view.getWidth() || height != view.getHeight())) {
                timer.shutdown();
            }
            this.width = width;
            this.height = height;
            final int t = getTotalObjects();
            if (t > 0) {
                final RenderScript rs = RenderScript.create(view.getContext());
                final ScriptC_stage script = new ScriptC_stage(rs);
                script.bind_extra(Allocation.createSized(rs, Element.F32(rs), 6 * t));
                script.bind_forces(Allocation.createSized(rs, Element.F32_2(rs), t));
                script.bind_quads(Allocation.createSized(rs, Element.F32_4(rs), 6 * t));
                script.bind_factor(Allocation.createSized(rs, Element.F32(rs), 1));
                final Allocation position = Allocation.createSized(rs, Element.F32_2(rs), t);
                final Allocation next_forces;
                if (solid) {
                    script.bind_pos(position);
                    next_forces = Allocation.createSized(rs, Element.F32_2(rs), t);
                    script.bind_hit(Allocation.createSized(rs, Element.I8(rs), t));
                    script.bind_axis(Allocation.createSized(rs, Element.I32(rs), t));
                    script.bind_axis_index(Allocation.createSized(rs, Element.I32(rs), t));
                    script.bind_instant(Allocation.createSized(rs, Element.F32_2(rs), t));
                } else {
                    next_forces = null;
                    script.bind_hole(Allocation.createSized(rs, Element.I32(rs), t));
                    script.bind_pgrid(Allocation.createSized(rs, Element.I32(rs), t));
                    script.bind_ptype(Allocation.createSized(rs, Element.I32(rs), t));
                    script.bind_grid_type(Allocation.createSized(rs, Element.I32(rs), t));
                    script.bind_grid_mass(Allocation.createSized(rs, Element.F32(rs), t));
                    script.bind_grid_center(Allocation.createSized(rs, Element.F32_2(rs), t));
                    script.bind_grid_forces(Allocation.createSized(rs, Element.F32_2(rs), t));
                    script.get_pgrid().copyFrom(new int[t]);
                }
                script.get_forces().copyFrom(new float[2 * t]);
                if (width < height) {
                    script.set_width((float) width / height);
                    script.set_height(1f);
                } else {
                    script.set_width(1f);
                    script.set_height((float) height / width);
                }
                quads = new float[6 * 4 * t];
                extra = new float[6 * t];
                script.set_attenuation(attenuation);
                script.set_scale(scaleFactor * 0.002f);
                script.set_pinch(new Float2(0f, 0f));
                final float[] f = new float[] {1f};
                script.get_factor().copyFrom(f);
                if (solid) {
                    script.invoke_func2(t);
                } else {
                    level = new Level(script.get_width(), script.get_height(), totalObjects);
                    position.copyFrom(level.position);
                    script.get_ptype().copyFrom(level.ptype);
                    script.set_s_force(sForce);
                    script.set_s_gravity(sGravity);
                    script.set_s_horizon(sHorizon);
                    script.set_s_gauss(sGauss);
                    script.set_s_formula(sFormula);
                    script.set_radius(radius);
                    script.set_total_grid(0);
                    script.set_total_hole(0);
                    script.set_bounce(bounce ? 1 : 0);
                    script.invoke_func1(position);
                }
                timer = new ScheduledThreadPoolExecutor(1);
                timer.schedule(new Runnable() {
                    @Override
                    public void run() {
                        final long ct = System.currentTimeMillis();
                        script.set_pinch(new Float2(x * 0.00004f, y * 0.00004f));
                        if (solid) {
                            script.forEach_root5(position, next_forces);
                            script.invoke_func3(next_forces);
                        } else {
                            script.forEach_root1(script.get_grid_center(), script.get_grid_center());
                            script.forEach_root2(position, position);
                            script.invoke_func1(position);
                        }
                        script.get_quads().copyTo(quads);
                        script.get_extra().copyTo(extra);
                        script.get_factor().copyTo(f);
                        if (view != null) {
                            view.requestRender();
                        }
                        timer.schedule(this, Math.max((long) ((speed - System.currentTimeMillis() + ct) * f[0]), 0), TimeUnit.MILLISECONDS);
                    }
                }, 0, TimeUnit.MILLISECONDS);
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences preference, final String key) {
        int v;
        if (key.equals(context.getString(R.string.pref_key_formula_self1))) {
            sFormula[0] = Integer.parseInt(preference.getString(key, ""));
        } else if (key.equals(context.getString(R.string.pref_key_formula_self2))) {
            sFormula[3] = Integer.parseInt(preference.getString(key, ""));
        } else if (key.equals(context.getString(R.string.pref_key_formula_others1))) {
            sFormula[1] = Integer.parseInt(preference.getString(key, ""));
        } else if (key.equals(context.getString(R.string.pref_key_formula_others2))) {
            sFormula[2] = Integer.parseInt(preference.getString(key, ""));
        } else if (key.equals(context.getString(R.string.pref_key_cold))) {
            v = preference.getInt(key, 0);
            begin = new float[] {Color.red(v) / 255f, Color.green(v) / 255f, Color.blue(v) / 255f};
        } else if (key.equals(context.getString(R.string.pref_key_warm))) {
            v = preference.getInt(key, 0);
            end = new float[] {Color.red(v) / 255f, Color.green(v) / 255f, Color.blue(v) / 255f};
        } else if (key.equals(context.getString(R.string.pref_key_fps))) {
            fps = preference.getBoolean(key, false);
        } else if (key.equals(context.getString(R.string.pref_key_total1))) {
            totalObjects[0] = (int) exponentialScale(0, 7.0, preference.getInt(key, 0));
        } else if (key.equals(context.getString(R.string.pref_key_total2))) {
            totalObjects[1] = (int) exponentialScale(0, 7.0, preference.getInt(key, 0));
        } else if (key.equals(context.getString(R.string.pref_key_radius))) {
            radius = (100f - preference.getInt(key, 0)) * 0.001f;
        } else if (key.equals(context.getString(R.string.pref_key_scale_factor))) {
            scaleFactor = (int) preference.getInt(key, 0) + 1;
        } else if (key.equals(context.getString(R.string.pref_key_offset))) {
            offset = preference.getInt(key, 0) * 0.0001f;
        } else if (key.equals(context.getString(R.string.pref_key_trail))) {
            v = preference.getInt(key, 0);
            trail = (v == 0f ? 1f : 1f - 1f / v);
        } else if (key.equals(context.getString(R.string.pref_key_glow))) {
            glow = preference.getInt(key, 0) * 0.5f + 1f;
        } else if (key.equals(context.getString(R.string.pref_key_horizon_self1))) {
            v = preference.getInt(key, 0);
            sHorizon[0] = (v == 0 ? 0.0001f : v * 0.001f);
        } else if (key.equals(context.getString(R.string.pref_key_horizon_others1))) {
            v = preference.getInt(key, 0);
            sHorizon[1] = (v == 0 ? 0.0001f : v * 0.001f);
        } else if (key.equals(context.getString(R.string.pref_key_horizon_self2))) {
            v = preference.getInt(key, 0);
            sHorizon[3] = (v == 0 ? 0.0001f : v * 0.001f);
        } else if (key.equals(context.getString(R.string.pref_key_horizon_others2))) {
            v = preference.getInt(key, 0);
            sHorizon[2] = (v == 0 ? 0.0001f : v * 0.001f);
        } else if (key.equals(context.getString(R.string.pref_key_attenuation))) {
            attenuation = 1f - preference.getInt(key, 0) * 0.001f;
        } else if (key.equals(context.getString(R.string.pref_key_gauss_self1))) {
            sGauss[0] = preference.getInt(key, 0) * 0.001f;
        } else if (key.equals(context.getString(R.string.pref_key_gauss_others1))) {
            sGauss[1] = preference.getInt(key, 0) * 0.001f;
        } else if (key.equals(context.getString(R.string.pref_key_gauss_self2))) {
            sGauss[3] = preference.getInt(key, 0) * 0.001f;
        } else if (key.equals(context.getString(R.string.pref_key_gauss_others2))) {
            sGauss[2] = preference.getInt(key, 0) * 0.001f;
        } else if (key.equals(context.getString(R.string.pref_key_force_self1))) {
            sForce[0] = Stage.exponentialScale(50, 4.0, preference.getInt(key, 0)) * 0.00000001f;
        } else if (key.equals(context.getString(R.string.pref_key_force_self2))) {
            sForce[3] = Stage.exponentialScale(50, 4.0, preference.getInt(key, 0)) * 0.00000001f;
        } else if (key.equals(context.getString(R.string.pref_key_force_others1))) {
            sForce[1] = Stage.exponentialScale(50, 4.0, preference.getInt(key, 0)) * 0.00000001f;
        } else if (key.equals(context.getString(R.string.pref_key_force_others2))) {
            sForce[2] = Stage.exponentialScale(50, 4.0, preference.getInt(key, 0)) * 0.00000001f;
        } else if (key.equals(context.getString(R.string.pref_key_gravity_self1))) {
            sGravity[0] = Stage.multiplier(preference.getInt(key, 0));
        } else if (key.equals(context.getString(R.string.pref_key_gravity_self2))) {
            sGravity[3] = Stage.multiplier(preference.getInt(key, 0));
        } else if (key.equals(context.getString(R.string.pref_key_gravity_others1))) {
            sGravity[1] = Stage.multiplier(preference.getInt(key, 0));
        } else if (key.equals(context.getString(R.string.pref_key_gravity_others2))) {
            sGravity[2] = Stage.multiplier(preference.getInt(key, 0));
        } else if (key.equals(context.getString(R.string.pref_key_render))) {
            renderMode = Integer.valueOf(preference.getString(key, "0"));
        } else if (key.equals(context.getString(R.string.pref_key_motion))) {
            motion = preference.getBoolean(key, false);
            x = y = 0f;
        } else if (key.equals(context.getString(R.string.pref_key_bounce))) {
            bounce = preference.getBoolean(key, false);
        } else if (key.equals(context.getString(R.string.pref_key_solid))) {
            solid = preference.getBoolean(key, false);
        } else if (key.equals(context.getString(R.string.pref_key_speed))) {
            speed = 33 - preference.getInt(key, 0);
        }
    }

    static float multiplier(final float v) {
        return v < 10f ? v * 0.1f : v - 9f;
    }

    static float exponentialScale(final int zero, final double a, final float v) {
        if (v < zero) {
            return (float) (zero - v < 20 ? v - zero : -Math.pow(2.0, (zero - v) / a) - 20);
        } else {
            return (float) (v - zero < 20 ? v - zero : Math.pow(2.0, (v - zero) / a) + 20);
        }
    }

    static int solveExponentialScale(final double a, final float v) {
        if (v < 0f) {
            return (int) Math.min(Math.round(v > -20f ? v : -Math.max(a * Math.log(-v - 20) / Math.log(2.0), 20)), -1.0);
        } else {
            return (int) Math.max(Math.round(v < 20f ? v : Math.max(a * Math.log(v - 20) / Math.log(2.0), 20)), 1.0);
        }
    }

    static void resetToPredefined(final Context context, final SharedPreferences.Editor editor, final String index) {
        editor.putInt(context.getString(R.string.pref_key_speed), 0);
        editor.putBoolean(context.getString(R.string.pref_key_solid), false);
        editor.putBoolean(context.getString(R.string.pref_key_motion), false);
        editor.putBoolean(context.getString(R.string.pref_key_bounce), false);
        editor.putString(context.getString(R.string.pref_key_render), "0");
        switch (index) {
            case "1":
                //editor.putInt(context.getString(R.string.pref_key_total1), 69);
                editor.putInt(context.getString(R.string.pref_key_total1), 86);
                editor.putInt(context.getString(R.string.pref_key_total2), 2);
                //editor.putInt(context.getString(R.string.pref_key_radius), 70);
                editor.putInt(context.getString(R.string.pref_key_radius), 60);
                editor.putInt(context.getString(R.string.pref_key_scale_factor), 3);
                editor.putInt(context.getString(R.string.pref_key_offset), 5);
                //editor.putInt(context.getString(R.string.pref_key_trail), 15);
                editor.putInt(context.getString(R.string.pref_key_trail), 0);
                editor.putInt(context.getString(R.string.pref_key_glow), 1);
                editor.putInt(context.getString(R.string.pref_key_cold), Color.BLUE);
                editor.putInt(context.getString(R.string.pref_key_warm), Color.RED);
                editor.putInt(context.getString(R.string.pref_key_attenuation), 10);
                editor.putInt(context.getString(R.string.pref_key_force_self1), 55);
                editor.putInt(context.getString(R.string.pref_key_gravity_self1), 1);
                editor.putInt(context.getString(R.string.pref_key_horizon_self1), 150);
                editor.putString(context.getString(R.string.pref_key_formula_self1), "0");
                //editor.putInt(context.getString(R.string.pref_key_force_others1), 13);
                editor.putInt(context.getString(R.string.pref_key_force_others1), 4);
                editor.putInt(context.getString(R.string.pref_key_gravity_others1), 7);
                editor.putInt(context.getString(R.string.pref_key_horizon_others1), 2);
                editor.putString(context.getString(R.string.pref_key_formula_others1), "0");
                editor.putInt(context.getString(R.string.pref_key_force_others2), 70);
                editor.putInt(context.getString(R.string.pref_key_gravity_others2), 12);
                editor.putInt(context.getString(R.string.pref_key_horizon_others2), 20);
                editor.putString(context.getString(R.string.pref_key_formula_others2), "0");
                editor.putInt(context.getString(R.string.pref_key_force_self2), 10);
                editor.putInt(context.getString(R.string.pref_key_gravity_self2), 19);
                editor.putInt(context.getString(R.string.pref_key_horizon_self2), 20);
                editor.putString(context.getString(R.string.pref_key_formula_self2), "0");
                editor.putInt(context.getString(R.string.pref_key_gauss_self1), 0);
                editor.putInt(context.getString(R.string.pref_key_gauss_self2), 0);
                editor.putInt(context.getString(R.string.pref_key_gauss_others1), 0);
                editor.putInt(context.getString(R.string.pref_key_gauss_others2), 0);
                break;
            case "2":
                //editor.putInt(context.getString(R.string.pref_key_total1), 69);
                editor.putInt(context.getString(R.string.pref_key_total1), 86);
                editor.putInt(context.getString(R.string.pref_key_total2), 20);
                editor.putInt(context.getString(R.string.pref_key_radius), 70);
                editor.putInt(context.getString(R.string.pref_key_scale_factor), 1);
                editor.putInt(context.getString(R.string.pref_key_offset), 10);
                editor.putInt(context.getString(R.string.pref_key_trail), 20);
                editor.putInt(context.getString(R.string.pref_key_glow), 1);
                editor.putInt(context.getString(R.string.pref_key_cold), Color.BLUE);
                editor.putInt(context.getString(R.string.pref_key_warm), Color.YELLOW);
                editor.putInt(context.getString(R.string.pref_key_attenuation), 30);
                editor.putInt(context.getString(R.string.pref_key_force_self1), 31);
                editor.putInt(context.getString(R.string.pref_key_gravity_self1), 10);
                editor.putInt(context.getString(R.string.pref_key_horizon_self1), 96);
                editor.putString(context.getString(R.string.pref_key_formula_self1), "0");
                editor.putInt(context.getString(R.string.pref_key_force_others1), 85);
                editor.putInt(context.getString(R.string.pref_key_gravity_others1), 1);
                editor.putInt(context.getString(R.string.pref_key_horizon_others1), 3);
                editor.putString(context.getString(R.string.pref_key_formula_others1), "0");
                editor.putInt(context.getString(R.string.pref_key_force_others2), 70);
                editor.putInt(context.getString(R.string.pref_key_gravity_others2), 15);
                editor.putInt(context.getString(R.string.pref_key_horizon_others2), 34);
                editor.putString(context.getString(R.string.pref_key_formula_others2), "0");
                editor.putInt(context.getString(R.string.pref_key_force_self2), 10);
                editor.putInt(context.getString(R.string.pref_key_gravity_self2), 4);
                editor.putInt(context.getString(R.string.pref_key_horizon_self2), 10);
                editor.putString(context.getString(R.string.pref_key_formula_self2), "0");
                editor.putInt(context.getString(R.string.pref_key_gauss_self1), 0);
                editor.putInt(context.getString(R.string.pref_key_gauss_self2), 0);
                editor.putInt(context.getString(R.string.pref_key_gauss_others1), 0);
                editor.putInt(context.getString(R.string.pref_key_gauss_others2), 0);
                break;
            case "3":
                editor.putBoolean(context.getString(R.string.pref_key_bounce), true);
                editor.putInt(context.getString(R.string.pref_key_total1), 93);
                editor.putInt(context.getString(R.string.pref_key_total2), 2);
                editor.putInt(context.getString(R.string.pref_key_radius), 0);
                editor.putInt(context.getString(R.string.pref_key_scale_factor), 4);
                editor.putInt(context.getString(R.string.pref_key_offset), 5);
                editor.putInt(context.getString(R.string.pref_key_trail), 1);
                editor.putInt(context.getString(R.string.pref_key_glow), 1);
                editor.putInt(context.getString(R.string.pref_key_cold), Color.BLUE);
                editor.putInt(context.getString(R.string.pref_key_warm), Color.RED);
                editor.putInt(context.getString(R.string.pref_key_attenuation), 10);
                editor.putInt(context.getString(R.string.pref_key_force_self1), 50);
                editor.putInt(context.getString(R.string.pref_key_gravity_self1), 10);
                editor.putInt(context.getString(R.string.pref_key_horizon_self1), 100);
                editor.putString(context.getString(R.string.pref_key_formula_self1), "0");
                editor.putInt(context.getString(R.string.pref_key_force_others1), 10);
                editor.putInt(context.getString(R.string.pref_key_gravity_others1), 7);
                editor.putInt(context.getString(R.string.pref_key_horizon_others1), 2);
                editor.putString(context.getString(R.string.pref_key_formula_others1), "0");
                editor.putInt(context.getString(R.string.pref_key_force_others2), 65);
                editor.putInt(context.getString(R.string.pref_key_gravity_others2), 13);
                editor.putInt(context.getString(R.string.pref_key_horizon_others2), 20);
                editor.putString(context.getString(R.string.pref_key_formula_others2), "0");
                editor.putInt(context.getString(R.string.pref_key_force_self2), 10);
                editor.putInt(context.getString(R.string.pref_key_gravity_self2), 19);
                editor.putInt(context.getString(R.string.pref_key_horizon_self2), 20);
                editor.putString(context.getString(R.string.pref_key_formula_self2), "0");
                editor.putInt(context.getString(R.string.pref_key_gauss_self1), 0);
                editor.putInt(context.getString(R.string.pref_key_gauss_self2), 0);
                editor.putInt(context.getString(R.string.pref_key_gauss_others1), 0);
                editor.putInt(context.getString(R.string.pref_key_gauss_others2), 0);
                break;
            case "4":
                editor.putInt(context.getString(R.string.pref_key_total1), 93);
                editor.putInt(context.getString(R.string.pref_key_total2), 2);
                editor.putInt(context.getString(R.string.pref_key_radius), 0);
                editor.putInt(context.getString(R.string.pref_key_scale_factor), 10);
                editor.putInt(context.getString(R.string.pref_key_offset), 80);
                editor.putInt(context.getString(R.string.pref_key_trail), 4);
                editor.putInt(context.getString(R.string.pref_key_glow), 1);
                editor.putInt(context.getString(R.string.pref_key_cold), Color.YELLOW);
                editor.putInt(context.getString(R.string.pref_key_warm), Color.RED);
                editor.putInt(context.getString(R.string.pref_key_attenuation), 30);
                editor.putInt(context.getString(R.string.pref_key_force_self1), 55);
                editor.putInt(context.getString(R.string.pref_key_gravity_self1), 10);
                editor.putInt(context.getString(R.string.pref_key_horizon_self1), 150);
                editor.putString(context.getString(R.string.pref_key_formula_self1), "0");
                editor.putInt(context.getString(R.string.pref_key_force_others1), 5);
                editor.putInt(context.getString(R.string.pref_key_gravity_others1), 7);
                editor.putInt(context.getString(R.string.pref_key_horizon_others1), 2);
                editor.putString(context.getString(R.string.pref_key_formula_others1), "0");
                editor.putInt(context.getString(R.string.pref_key_force_others2), 70);
                editor.putInt(context.getString(R.string.pref_key_gravity_others2), 10);
                editor.putInt(context.getString(R.string.pref_key_horizon_others2), 20);
                editor.putString(context.getString(R.string.pref_key_formula_others2), "0");
                editor.putInt(context.getString(R.string.pref_key_force_self2), 10);
                editor.putInt(context.getString(R.string.pref_key_gravity_self2), 19);
                editor.putInt(context.getString(R.string.pref_key_horizon_self2), 20);
                editor.putString(context.getString(R.string.pref_key_formula_self2), "0");
                editor.putInt(context.getString(R.string.pref_key_gauss_self1), 0);
                editor.putInt(context.getString(R.string.pref_key_gauss_self2), 0);
                editor.putInt(context.getString(R.string.pref_key_gauss_others1), 0);
                editor.putInt(context.getString(R.string.pref_key_gauss_others2), 0);
                break;
            case "5":
                editor.putBoolean(context.getString(R.string.pref_key_bounce), true);
                editor.putInt(context.getString(R.string.pref_key_total1), 83);
                editor.putInt(context.getString(R.string.pref_key_total2), 5);
                editor.putInt(context.getString(R.string.pref_key_radius), 0);
                editor.putInt(context.getString(R.string.pref_key_scale_factor), 14);
                editor.putInt(context.getString(R.string.pref_key_offset), 5);
                editor.putInt(context.getString(R.string.pref_key_trail), 4);
                editor.putInt(context.getString(R.string.pref_key_glow), 1);
                editor.putInt(context.getString(R.string.pref_key_cold), Color.BLACK);
                editor.putInt(context.getString(R.string.pref_key_warm), Color.GREEN);
                editor.putInt(context.getString(R.string.pref_key_attenuation), 10);
                editor.putInt(context.getString(R.string.pref_key_force_self1), 50);
                editor.putInt(context.getString(R.string.pref_key_gravity_self1), 10);
                editor.putInt(context.getString(R.string.pref_key_horizon_self1), 0);
                editor.putString(context.getString(R.string.pref_key_formula_self1), "0");
                editor.putInt(context.getString(R.string.pref_key_force_others1), 16);
                editor.putInt(context.getString(R.string.pref_key_gravity_others1), 2);
                editor.putInt(context.getString(R.string.pref_key_horizon_others1), 2);
                editor.putString(context.getString(R.string.pref_key_formula_others1), "1");
                editor.putInt(context.getString(R.string.pref_key_gauss_others1), 59);
                editor.putInt(context.getString(R.string.pref_key_force_others2), 77);
                editor.putInt(context.getString(R.string.pref_key_gravity_others2), 12);
                editor.putInt(context.getString(R.string.pref_key_horizon_others2), 20);
                editor.putString(context.getString(R.string.pref_key_formula_others2), "1");
                editor.putInt(context.getString(R.string.pref_key_gauss_others2), 55);
                editor.putInt(context.getString(R.string.pref_key_force_self2), 10);
                editor.putInt(context.getString(R.string.pref_key_gravity_self2), 14);
                editor.putInt(context.getString(R.string.pref_key_horizon_self2), 20);
                editor.putString(context.getString(R.string.pref_key_formula_self2), "1");
                editor.putInt(context.getString(R.string.pref_key_gauss_self2), 73);
                editor.putInt(context.getString(R.string.pref_key_gauss_self1), 0);
                break;
            case "6":
                editor.putInt(context.getString(R.string.pref_key_speed), 33);
                editor.putBoolean(context.getString(R.string.pref_key_motion), true);
                editor.putBoolean(context.getString(R.string.pref_key_solid), true);
                editor.putInt(context.getString(R.string.pref_key_total1), 65);
                editor.putInt(context.getString(R.string.pref_key_total2), 0);
                editor.putInt(context.getString(R.string.pref_key_radius), 0);
                editor.putInt(context.getString(R.string.pref_key_scale_factor), 15);
                editor.putInt(context.getString(R.string.pref_key_offset), 0);
                editor.putInt(context.getString(R.string.pref_key_trail), 0);
                editor.putInt(context.getString(R.string.pref_key_glow), 0);
                editor.putInt(context.getString(R.string.pref_key_cold), Color.DKGRAY);
                editor.putInt(context.getString(R.string.pref_key_warm), Color.LTGRAY);
                editor.putInt(context.getString(R.string.pref_key_attenuation), 0);
                editor.putInt(context.getString(R.string.pref_key_force_self1), 50);
                editor.putInt(context.getString(R.string.pref_key_gravity_self1), 10);
                editor.putInt(context.getString(R.string.pref_key_horizon_self1), 0);
                editor.putInt(context.getString(R.string.pref_key_gauss_self1), 0);
                editor.putString(context.getString(R.string.pref_key_formula_self1), "0");
                editor.putInt(context.getString(R.string.pref_key_force_others1), 50);
                editor.putInt(context.getString(R.string.pref_key_gravity_others1), 10);
                editor.putInt(context.getString(R.string.pref_key_horizon_others1), 0);
                editor.putString(context.getString(R.string.pref_key_formula_others1), "0");
                editor.putInt(context.getString(R.string.pref_key_force_others2), 50);
                editor.putInt(context.getString(R.string.pref_key_gravity_others2), 10);
                editor.putInt(context.getString(R.string.pref_key_horizon_others2), 0);
                editor.putString(context.getString(R.string.pref_key_formula_others2), "0");
                editor.putInt(context.getString(R.string.pref_key_force_self2), 50);
                editor.putInt(context.getString(R.string.pref_key_gravity_self2), 10);
                editor.putInt(context.getString(R.string.pref_key_horizon_self2), 0);
                editor.putString(context.getString(R.string.pref_key_formula_self2), "0");
                editor.putInt(context.getString(R.string.pref_key_gauss_self2), 0);
                editor.putInt(context.getString(R.string.pref_key_gauss_others1), 0);
                editor.putInt(context.getString(R.string.pref_key_gauss_others2), 0);
                break;
            case "7":
                editor.putInt(context.getString(R.string.pref_key_total1), 100);
                editor.putInt(context.getString(R.string.pref_key_total2), 0);
                editor.putInt(context.getString(R.string.pref_key_radius), 0);
                editor.putInt(context.getString(R.string.pref_key_scale_factor), 2);
                editor.putInt(context.getString(R.string.pref_key_offset), 0);
                editor.putInt(context.getString(R.string.pref_key_trail), 100);
                editor.putInt(context.getString(R.string.pref_key_glow), 5);
                editor.putInt(context.getString(R.string.pref_key_cold), Color.BLUE);
                editor.putInt(context.getString(R.string.pref_key_warm), Color.BLACK);
                editor.putInt(context.getString(R.string.pref_key_attenuation), 0);
                editor.putInt(context.getString(R.string.pref_key_force_self1), 40);
                editor.putInt(context.getString(R.string.pref_key_gravity_self1), 10);
                editor.putInt(context.getString(R.string.pref_key_horizon_self1), 150);
                editor.putInt(context.getString(R.string.pref_key_gauss_self1), 10);
                editor.putString(context.getString(R.string.pref_key_formula_self1), "1");
                editor.putInt(context.getString(R.string.pref_key_force_others1), 0);
                editor.putInt(context.getString(R.string.pref_key_gravity_others1), 0);
                editor.putInt(context.getString(R.string.pref_key_horizon_others1), 0);
                editor.putString(context.getString(R.string.pref_key_formula_others1), "0");
                editor.putInt(context.getString(R.string.pref_key_force_others2), 0);
                editor.putInt(context.getString(R.string.pref_key_gravity_others2), 0);
                editor.putInt(context.getString(R.string.pref_key_horizon_others2), 0);
                editor.putString(context.getString(R.string.pref_key_formula_others2), "0");
                editor.putInt(context.getString(R.string.pref_key_force_self2), 0);
                editor.putInt(context.getString(R.string.pref_key_gravity_self2), 0);
                editor.putInt(context.getString(R.string.pref_key_horizon_self2), 0);
                editor.putString(context.getString(R.string.pref_key_formula_self2), "0");
                editor.putInt(context.getString(R.string.pref_key_gauss_self2), 0);
                editor.putInt(context.getString(R.string.pref_key_gauss_others1), 0);
                editor.putInt(context.getString(R.string.pref_key_gauss_others2), 0);
                break;
            case "8":
                final Random r = new Random();
                editor.putBoolean(context.getString(R.string.pref_key_motion), r.nextBoolean());
                editor.putBoolean(context.getString(R.string.pref_key_bounce), r.nextBoolean());
                editor.putInt(context.getString(R.string.pref_key_total1), r.nextInt(95));
                editor.putInt(context.getString(R.string.pref_key_total2), r.nextInt(95));
                editor.putInt(context.getString(R.string.pref_key_radius), r.nextInt(101));
                editor.putString(context.getString(R.string.pref_key_render), r.nextInt(4) != 0 ? "0" : "1");
                editor.putInt(context.getString(R.string.pref_key_scale_factor), r.nextInt(31));
                editor.putInt(context.getString(R.string.pref_key_offset), r.nextInt(3) == 0 ? 0 : r.nextInt(100) + 1);
                editor.putInt(context.getString(R.string.pref_key_trail), r.nextInt(4) == 0 ? 0 : r.nextInt(100) + 1);
                editor.putInt(context.getString(R.string.pref_key_glow), r.nextInt(6));
                editor.putInt(context.getString(R.string.pref_key_cold), Color.rgb(r.nextInt(256), r.nextInt(256), r.nextInt(256)));
                editor.putInt(context.getString(R.string.pref_key_warm), Color.rgb(r.nextInt(256), r.nextInt(256), r.nextInt(256)));
                editor.putInt(context.getString(R.string.pref_key_attenuation), r.nextInt(31));
                editor.putInt(context.getString(R.string.pref_key_force_self1), r.nextInt(101));
                editor.putInt(context.getString(R.string.pref_key_gravity_self1), r.nextInt(20));
                editor.putInt(context.getString(R.string.pref_key_horizon_self1), r.nextInt(501));
                editor.putInt(context.getString(R.string.pref_key_gauss_self1), r.nextInt(101));
                editor.putString(context.getString(R.string.pref_key_formula_self1), String.valueOf(r.nextInt(2)));
                editor.putInt(context.getString(R.string.pref_key_force_others1), r.nextInt(101));
                editor.putInt(context.getString(R.string.pref_key_gravity_others1), r.nextInt(20));
                editor.putInt(context.getString(R.string.pref_key_horizon_others1), r.nextInt(501));
                editor.putInt(context.getString(R.string.pref_key_gauss_others1), r.nextInt(101));
                editor.putString(context.getString(R.string.pref_key_formula_others1), String.valueOf(r.nextInt(2)));
                editor.putInt(context.getString(R.string.pref_key_force_others2), r.nextInt(101));
                editor.putInt(context.getString(R.string.pref_key_gravity_others2), r.nextInt(20));
                editor.putInt(context.getString(R.string.pref_key_horizon_others2), r.nextInt(501));
                editor.putInt(context.getString(R.string.pref_key_gauss_others2), r.nextInt(101));
                editor.putString(context.getString(R.string.pref_key_formula_others2), String.valueOf(r.nextInt(2)));
                editor.putInt(context.getString(R.string.pref_key_force_self2), r.nextInt(101));
                editor.putInt(context.getString(R.string.pref_key_gravity_self2), r.nextInt(20));
                editor.putInt(context.getString(R.string.pref_key_horizon_self2), r.nextInt(501));
                editor.putInt(context.getString(R.string.pref_key_gauss_self2), r.nextInt(101));
                editor.putString(context.getString(R.string.pref_key_formula_self2), String.valueOf(r.nextInt(2)));
        }
        editor.commit();
    }
}

