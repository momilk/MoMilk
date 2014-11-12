package com.momilk.momilk;


import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This class holds public constants. Constants which can be exposed without security/privacy
 * undermining go here for clarity and simplicity of other classes.
 */
public class Constants {

    // Intent request codes
    public static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    public static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    public static final int REQUEST_ENABLE_BT = 3;


    // Message types sent from the BluetoothService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Tags to be used for app's tabs
    public static final String HOME_TAB_TAG = "home";
    public static final String HISTORY_TAB_TAG = "history";
    public static final String SETTINGS_TAB_TAG = "settings";
    public static final String EXTRAS_TAB_TAG = "extras";
    // Debug tabs
    public static final String COMM_TAB_TAG = "comm";


    /*
    This map is used in order to map tab's tags into default fragments for these tabs
     */
    public static final Map<String, Class> DEFAULT_TAB_FRAGMENT_MAP;
    static {
        Map<String, Class> map = new HashMap<String, Class>();
        map.put(HOME_TAB_TAG, HomeFragment.class);
        map.put(HISTORY_TAB_TAG, HistoryFragment.class);
        map.put(SETTINGS_TAB_TAG, SettingsFragment.class);
        map.put(EXTRAS_TAB_TAG, ExtrasFragment.class);
        map.put(COMM_TAB_TAG, BTCommDebugFragment.class);
        DEFAULT_TAB_FRAGMENT_MAP = Collections.unmodifiableMap(map);
    }

    /*
       This map is used in order to map tab's tags into icons fot these tabs
    */
    public static final Map<String, Integer> TAB_ICON_MAP;
    static {
        Map<String, Integer> map = new HashMap<String, Integer>();
        map.put(HOME_TAB_TAG, R.drawable.ic_home_tab);
        map.put(HISTORY_TAB_TAG, R.drawable.ic_home_tab);
        map.put(SETTINGS_TAB_TAG, R.drawable.ic_home_tab);
        map.put(EXTRAS_TAB_TAG, R.drawable.ic_home_tab);
        map.put(COMM_TAB_TAG, R.drawable.ic_home_tab);
        TAB_ICON_MAP = Collections.unmodifiableMap(map);
    }

}
