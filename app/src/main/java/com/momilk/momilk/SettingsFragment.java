package com.momilk.momilk;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.support.v4.app.FragmentActivity;
import android.support.v4.preference.PreferenceFragment;
import android.util.AttributeSet;

/**
 * Default fragment shown for a SETTINGS tab.
 */

public class SettingsFragment extends PreferenceFragment implements
        Preference.OnPreferenceClickListener {

    SettingsFragmentCallback mCallback;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        initPreferences();
    }

    private void initPreferences() {
        SharedPreferences sharedPref = getActivity().getPreferences(FragmentActivity.MODE_PRIVATE);

        Preference preference;

        // Personal data
        preference = (Preference) findPreference(getResources().getString(R.string.preference_personal_data_key));
        preference.setOnPreferenceClickListener(this);

        // Default device
        preference = (Preference) findPreference(getResources().getString(R.string.preference_default_device_key));
        preference.setOnPreferenceClickListener(this);
        preference.setSummary(getString(R.string.preference_summary_current_text) + " " +
                sharedPref.getString(getString(R.string.preference_default_device_name_key), "-"));

        // Clear history
        preference = (Preference) findPreference(getResources().getString(R.string.preference_clear_history_key));
        preference.setOnPreferenceClickListener(this);

    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (SettingsFragmentCallback) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement SettingsFragmentCallback");
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();

        if (key.equals(getResources().getString(R.string.preference_personal_data_key))) {
            mCallback.onPersonalDataClick();
            return true;
        } else if (key.equals(getResources().getString(R.string.preference_default_device_key))) {
            mCallback.onSetDefaultDeviceClick();
            return true;
        } else if (key.equals(getResources().getString(R.string.preference_clear_history_key))) {
            mCallback.onHistoryClearClick();
            return true;
        } else {
            return false;
        }

    }



    // Container Activity must implement this interface
    public interface SettingsFragmentCallback {
        public void onPersonalDataClick();
        public void onSetDefaultDeviceClick();
        public void onHistoryClearClick();
    }


 }
