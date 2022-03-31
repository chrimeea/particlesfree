package com.prozium.particlesfree;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

/**
 * Created by cristian on 02.06.2016.
 */
public class SeekBarPreference extends Preference implements OnSeekBarChangeListener {

    int mProgress;
    int mMax;
    boolean mTrackingTouch;
    SeekBar seekBar;

    public SeekBarPreference(final Context context, final AttributeSet attr) {
        this(context, attr, 0);
    }

    public SeekBarPreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SeekBarPreference, defStyle, 0);
        setMax(a.getInt(R.styleable.SeekBarPreference_android_max, mMax));
        a.recycle();
        setLayoutResource(R.layout.slider);
    }

    @Override
    protected void onBindView(final View view) {
        super.onBindView(view);
        seekBar = (SeekBar) view.findViewById(R.id.seekbar);
        seekBar.setOnSeekBarChangeListener(this);
        seekBar.setMax(mMax);
        seekBar.setProgress(mProgress);
        seekBar.setEnabled(isEnabled());
    }

    @Override
    protected void onSetInitialValue(final boolean restoreValue, final Object defaultValue) {
        setProgress(restoreValue ? getPersistedInt(mProgress) : (Integer) defaultValue, true);
    }

    @Override
    protected Object onGetDefaultValue(final TypedArray a, final int index) {
        return a.getInt(index, 0);
    }

    void setMax(final int max) {
        if (max != mMax) {
            mMax = max;
            notifyChanged();
        }
    }

    void setProgress(int progress, final boolean notifyChanged) {
        if (progress > mMax) {
            progress = mMax;
        } else if (progress < 0) {
            progress = 0;
        }
        if (progress != mProgress) {
            mProgress = progress;
            persistInt(progress);
            if (notifyChanged) {
                notifyChanged();
            }
        }
    }

    public void syncProgress(final SeekBar seekBar) {
        final int progress = seekBar.getProgress();
        if (progress != mProgress) {
            if (callChangeListener(progress)) {
                setProgress(progress, false);
            } else {
                seekBar.setProgress(mProgress);
            }
        }
    }

    @Override
    public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
        if (fromUser) {
            if (getKey().equals(getContext().getString(R.string.pref_key_total1))
                    || getKey().equals(getContext().getString(R.string.pref_key_total2))) {
                final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                final SharedPreferences.Editor editor = preferences.edit();
                final float r = Stage.exponentialScale(0, 7.0, mProgress), t = Stage.exponentialScale(0, 7.0, progress);
                float v, w, z;
                if (getKey().equals(getContext().getString(R.string.pref_key_total1))) {
                    v = preferences.getInt(getContext().getString(R.string.pref_key_force_others2), 0);
                    if (v != 50) {
                        z = Stage.multiplier(preferences.getInt(getContext().getString(R.string.pref_key_gravity_others2), 0));
                        z = (z >= 1 ? 1 / z : z >= 0.5f ? z : 0);
                        w = r / (r + (t - r) * z);
                        editor.putInt(getContext().getString(R.string.pref_key_force_others2),
                                50 + (int) Math.min(Math.max(Stage.solveExponentialScale(4.0, Stage.exponentialScale(50, 4.0, v) * w), -50.0), 50.0));
                    }
                    v = preferences.getInt(getContext().getString(R.string.pref_key_force_others1), 0);
                    if (v != 50) {
                        z = Stage.multiplier(preferences.getInt(getContext().getString(R.string.pref_key_gravity_others1), 0));
                        z = (z >= 1 ? 1 / z : z >= 0.5f ? z : 0);
                        w = r / (r + (t - r) * z);
                        editor.putInt(getContext().getString(R.string.pref_key_force_others1),
                                50 + (int) Math.min(Math.max(Stage.solveExponentialScale(4.0, Stage.exponentialScale(50, 4.0, v) / w), -50.0), 50.0));
                    }
                } else if (getKey().equals(getContext().getString(R.string.pref_key_total2))) {
                    v = preferences.getInt(getContext().getString(R.string.pref_key_force_others1), 0);
                    if (v != 50) {
                        z = Stage.multiplier(preferences.getInt(getContext().getString(R.string.pref_key_gravity_others1), 0));
                        z = (z >= 1 ? 1 / z : z >= 0.5f ? z : 0);
                        w = r / (r + (t - r) * z);
                        editor.putInt(getContext().getString(R.string.pref_key_force_others1),
                                50 + (int) Math.min(Math.max(Stage.solveExponentialScale(4.0, Stage.exponentialScale(50, 4.0, v) * w), -50.0), 50.0));
                    }
                    v = preferences.getInt(getContext().getString(R.string.pref_key_force_others2), 0);
                    if (v != 50) {
                        z = Stage.multiplier(preferences.getInt(getContext().getString(R.string.pref_key_gravity_others2), 0));
                        z = (z >= 1 ? 1 / z : z >= 0.5f ? z : 0);
                        w = r / (r + (t - r) * z);
                        editor.putInt(getContext().getString(R.string.pref_key_force_others2),
                                50 + (int) Math.min(Math.max(Stage.solveExponentialScale(4.0, Stage.exponentialScale(50, 4.0, v) / w), -50.0), 50.0));
                    }
                }
                editor.commit();
            }
            if (!mTrackingTouch) {
                syncProgress(seekBar);
            }
        }
    }

    @Override
    public void onStartTrackingTouch(final SeekBar seekBar) {
        mTrackingTouch = true;
    }

    @Override
    public void onStopTrackingTouch(final SeekBar seekBar) {
        mTrackingTouch = false;
        if (seekBar.getProgress() != mProgress) {
            syncProgress(seekBar);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            return superState;
        }
        final SavedState myState = new SavedState(superState);
        myState.progress = mProgress;
        myState.max = mMax;
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(final Parcelable state) {
        if (!state.getClass().equals(SavedState.class)) {
            super.onRestoreInstanceState(state);
            return;
        }
        final SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        mProgress = myState.progress;
        mMax = myState.max;
        notifyChanged();
    }

    public static class SavedState extends BaseSavedState {

        int progress;
        int max;

        public SavedState(Parcel source) {
            super(source);
            progress = source.readInt();
            max = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(progress);
            dest.writeInt(max);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @SuppressWarnings("unused")
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}
