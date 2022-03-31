package com.prozium.particlesfree;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

public class SettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //getActionBar().setDisplayHomeAsUpEnabled(true);
        MobileAds.initialize(getBaseContext(), getResources().getString(R.string.ad_app_id));
        getFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new GeneralPreferenceFragment())
                .commit();
    }

    public static class GeneralPreferenceFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        final Handler handler = new Handler();
        Context context;
        AdView adView;

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            context = getActivity().getBaseContext();
            final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
            for (String key: p.getAll().keySet()) {
                onSharedPreferenceChanged(p, key);
            }
            findPreference(context.getString(R.string.pref_key_predefined)).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                    Stage.resetToPredefined(context, p.edit(), (String) newValue);
                    return false;
                }
            });
        }

        @Override
        public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
            //super.onCreateView(inflater, container, savedInstanceState);
            final View v = inflater.inflate(R.layout.ad, null);
            adView = (AdView) v.findViewById(R.id.adview);
            adView.loadAd(new AdRequest.Builder().build());
            return v;
        }

        @Override
        public void onSharedPreferenceChanged(final SharedPreferences preference, final String key) {
            final Preference p = findPreference(key);
            if (p != null) {
                /*
                if (key.startsWith("com.prozium.particlesfree.force_")
                        || key.startsWith("com.prozium.particlesfree.gravity_")
                        || key.startsWith("com.prozium.particlesfree.horizon_")
                        || key.startsWith("com.prozium.particlesfree.total")) {
                    Log.e("********", key + ": " + preference.getInt(key, 0));
                }
                */
                final String t = String.valueOf(p.getTitle());
                if (key.equals(context.getString(R.string.pref_key_formula_others1))
                        || key.equals(context.getString(R.string.pref_key_formula_others2))
                        || key.equals(context.getString(R.string.pref_key_formula_self1))
                        || key.equals(context.getString(R.string.pref_key_formula_self2))) {
                    final ListPreference lp = (ListPreference) p;
                    lp.setValue(preference.getString(key, ""));
                    p.setTitle(t.substring(0, t.lastIndexOf(':') + 1) + " " + lp.getEntry());
                } else if (key.equals(context.getString(R.string.pref_key_render))) {
                    final ListPreference lp = (ListPreference) p;
                    lp.setValue(preference.getString(key, "0"));
                    p.setTitle(t.substring(0, t.lastIndexOf(':') + 1) + " " + lp.getEntry());
                    final boolean value = preference.getString(key, "0").equals("0");
                    findPreference(context.getString(R.string.pref_key_offset)).setEnabled(value);
                    findPreference(context.getString(R.string.pref_key_trail)).setEnabled(value);
                } else if (key.equals(context.getString(R.string.pref_key_fps))
                        || key.equals(context.getString(R.string.pref_key_motion))
                        || key.equals(context.getString(R.string.pref_key_bounce))
                        || key.equals(context.getString(R.string.pref_key_solid))) {
                    ((CheckBoxPreference) p).setChecked(preference.getBoolean(key, false));
                } else if (key.equals(context.getString(R.string.pref_key_cold))
                        || key.equals(context.getString(R.string.pref_key_warm))) {
                    ((ColorPreference) p).setValue(preference.getInt(key, 0));
                } else {
                    float v = preference.getInt(key, 0);
                    if (key.equals(context.getString(R.string.pref_key_force_others1))
                            || key.equals(context.getString(R.string.pref_key_force_others2))
                            || key.equals(context.getString(R.string.pref_key_force_self1))
                            || key.equals(context.getString(R.string.pref_key_force_self2))) {
                        v = (int) Stage.exponentialScale(50, 5.0, v);
                    } else if (key.equals(context.getString(R.string.pref_key_gravity_others1))
                            || key.equals(context.getString(R.string.pref_key_gravity_others2))
                            || key.equals(context.getString(R.string.pref_key_gravity_self1))
                            || key.equals(context.getString(R.string.pref_key_gravity_self2))) {
                        v = v < 10f ? v * 0.1f : v - 9f;
                    } else if (key.equals(context.getString(R.string.pref_key_total1))
                            || key.equals(context.getString(R.string.pref_key_total2))) {
                        v = (int) Stage.exponentialScale(0, 7.0, v);
                    }
                    final String s = v == (int) v ? String.valueOf((int) v) : String.format("%.1f", v);
                    final int i = t.lastIndexOf(' ') + 1;
                    if (Character.isDigit(t.charAt(i)) || t.charAt(i) == '-') {
                        p.setTitle(t.substring(0, i) + s);
                    } else {
                        p.setTitle(t + ' ' + s);
                    }
                    final SeekBarPreference sb = (SeekBarPreference) p;
                    if (sb.mProgress != v) {
                        sb.mProgress = preference.getInt(key, 0);
                        if (sb.seekBar != null) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    sb.seekBar.setProgress(sb.mProgress);
                                }
                            });
                        }
                    }
                }
            }
        }

        @Override
        public void onPause() {
            if (adView != null) {
                adView.pause();
            }
            super.onPause();
            PreferenceManager.getDefaultSharedPreferences(context).unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onResume() {
            PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(this);
            super.onResume();
            if (adView != null) {
                adView.resume();
            }
        }

        @Override
        public void onDestroy() {
            if (adView != null) {
                adView.destroy();
            }
            super.onDestroy();
        }
    }
}
