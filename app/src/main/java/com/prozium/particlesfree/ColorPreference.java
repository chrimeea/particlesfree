package com.prozium.particlesfree;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;

/**
 * Created by cristian on 03.05.2017.
 */

public class ColorPreference extends DialogPreference {

    int value;

    public ColorPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColorPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected View onCreateDialogView() {
        final LayoutInflater inflater = LayoutInflater.from(getContext());
        final View v = inflater.inflate(R.layout.picker, null);
        final GridLayout colorGrid = (GridLayout) v.findViewById(R.id.color_grid);
        for (final int color: new int[] {
                Color.rgb(0, 0, 0),
                Color.rgb(0, 0, 128),
                Color.rgb(0, 0, 255),
                Color.rgb(0, 128, 0),
                Color.rgb(0, 128, 128),
                Color.rgb(0, 128, 255),
                Color.rgb(0, 255, 0),
                Color.rgb(0, 255, 128),
                Color.rgb(0, 255, 255),
                Color.rgb(128, 0, 0),
                Color.rgb(128, 0, 128),
                Color.rgb(128, 0, 255),
                Color.rgb(128, 128, 0),
                Color.rgb(128, 128, 128),
                Color.rgb(128, 128, 255),
                Color.rgb(128, 255, 0),
                Color.rgb(128, 255, 128),
                Color.rgb(128, 255, 255),
                Color.rgb(255, 0, 0),
                Color.rgb(255, 0, 128),
                Color.rgb(255, 0, 255),
                Color.rgb(255, 128, 0),
                Color.rgb(255, 128, 128),
                Color.rgb(255, 128, 255),
                Color.rgb(255, 255, 0),
                Color.rgb(255, 255, 128),
                Color.rgb(255, 255, 255)}) {
            final View itemView = inflater.inflate(R.layout.color, colorGrid, false);
            ColorPreference.setColorViewValue((ImageView) itemView.findViewById(R.id.color_view), color);
            itemView.setClickable(true);
            itemView.setFocusable(true);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    setValue(color);
                    getDialog().dismiss();
                }
            });
            colorGrid.addView(itemView);
        }
        return v;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        ColorPreference.setColorViewValue((ImageView) view.findViewById(R.id.color_view), value);
    }

    static void setColorViewValue(final ImageView imageView, final int color) {
        final Drawable currentDrawable = imageView.getDrawable();
        final GradientDrawable colorChoiceDrawable;
        if (currentDrawable instanceof GradientDrawable) {
            colorChoiceDrawable = (GradientDrawable) currentDrawable;
        } else {
            colorChoiceDrawable = new GradientDrawable();
            colorChoiceDrawable.setShape(GradientDrawable.RECTANGLE);
        }
        colorChoiceDrawable.setColor(color);
        imageView.setImageDrawable(colorChoiceDrawable);
    }

    public void setValue(int value) {
        if (callChangeListener(value)) {
            this.value = value;
            persistInt(value);
            notifyChanged();
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, 0);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setValue(restoreValue ? getPersistedInt(0) : (Integer) defaultValue);
    }
}
