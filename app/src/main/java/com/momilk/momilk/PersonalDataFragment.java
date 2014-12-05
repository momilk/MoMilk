package com.momilk.momilk;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class PersonalDataFragment extends PreferenceFragment implements
        Preference.OnPreferenceChangeListener {


    private static final String LOG_TAG = "PersonalDataFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager manager = getPreferenceManager();
        manager.setSharedPreferencesName(Main.PREFERENCE_FILE);

        addPreferencesFromResource(R.xml.preferences_personal_data);

        // TODO: add change listeners for all required preferences

    }


    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {

        // TODO: update the summary to the current value
        return false;
    }
}
