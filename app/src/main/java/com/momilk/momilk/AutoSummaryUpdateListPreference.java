package com.momilk.momilk;


import android.content.Context;
import android.content.res.Resources;
import android.preference.ListPreference;
import android.util.AttributeSet;

import com.momilk.momilk.R;


/**
 We need this class in order to allow for summary of ListPreference to be updated
 with currently set value whenever this value changes.
 */
public class AutoSummaryUpdateListPreference extends ListPreference {
    public AutoSummaryUpdateListPreference(final Context context) {
        this(context, null);
    }

    public AutoSummaryUpdateListPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public CharSequence getSummary() {
        final CharSequence entry = getEntry();
        return entry;
    }

    @Override
    public void setValue(final String value) {
        super.setValue(value);
        notifyChanged();
    }
}
