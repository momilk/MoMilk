package com.momilk.momilk;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class PersonalDataFragment extends PreferenceFragment {


    private static final String LOG_TAG = "PersonalDataFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_personal_data);

    }



}
