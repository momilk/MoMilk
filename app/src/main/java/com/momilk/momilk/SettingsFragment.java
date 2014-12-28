package com.momilk.momilk;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
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

        PreferenceManager manager = getPreferenceManager();
        manager.setSharedPreferencesName(Constants.PREFERENCE_FILE);

        addPreferencesFromResource(R.xml.preferences);

        initPreferences();
    }

    private void initPreferences() {
        SharedPreferences sharedPref = getActivity().getSharedPreferences(Constants.PREFERENCE_FILE,
                FragmentActivity.MODE_PRIVATE);

        Preference preference;

        // Personal data
        preference = (Preference) findPreference("preference_personal_data");
        preference.setOnPreferenceClickListener(this);

        // Default device
        preference = (Preference) findPreference("preference_default_device");
        preference.setOnPreferenceClickListener(this);
        preference.setSummary(
                sharedPref.getString("preference_default_device_name", ""));

        // Clear history
        preference = (Preference) findPreference("preference_clear_history");
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

        if (key.equals("preference_personal_data")) {
            mCallback.onPersonalDataClick();
            return true;
        } else if (key.equals("preference_default_device")) {
            mCallback.onSetDefaultDeviceClick();
            return true;
        } else if (key.equals("preference_clear_history")) {
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
