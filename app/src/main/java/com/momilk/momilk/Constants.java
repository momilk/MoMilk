package com.momilk.momilk;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This class holds public constants. Constants which can be exposed without security/privacy
 * undermining go here for clarity and simplicity of other classes.
 */
public class Constants {

    // The name of the preference file to use
    public static final String PREFERENCE_FILE = "com.momilk.momilk_pref";

    // Sync session timeout in seconds
    public static final int SYNC_TIMEOUT_SEC = 5;

    // Request codes
    public static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    public static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    public static final int REQUEST_ENABLE_BT = 3;

    // Intent actions
    public static final String ACTION_NOTIFY_DB_CHANGED = "com.momilk.momilk.ACTION_NOTIFY_DB_CHANGED";

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
    public static final String HISTORY_LIST_TAB_TAG = "history_list";
    public static final String HISTORY_CLOCK_TAB_TAG = "history_clock";
    public static final String SETTINGS_TAB_TAG = "settings";
    public static final String EXTRAS_TAB_TAG = "extras";



    // Default tabs to be shown in the app
    public static final String[] DEFAULT_TABS_GROUP = new String[] {
            HOME_TAB_TAG, SETTINGS_TAB_TAG, HISTORY_TAB_TAG, EXTRAS_TAB_TAG
    };

    // Tabs to be shown when the user enters HISTORY tab
    public static final String[] HISTORY_TABS_GROUP = new String[] {
            HOME_TAB_TAG, HISTORY_LIST_TAB_TAG, HISTORY_CLOCK_TAB_TAG, EXTRAS_TAB_TAG
    };



    /*
    This map is used in order to map tab's tags into default fragments for these tabs
     */
    public static final Map<String, Class> DEFAULT_TAB_FRAGMENT_MAP;
    static {
        Map<String, Class> map = new HashMap<String, Class>();
        map.put(HOME_TAB_TAG, HomeFragment.class);
        map.put(HISTORY_TAB_TAG, HistoryFragment.class);
        map.put(HISTORY_LIST_TAB_TAG, HistoryFragment.class);
        map.put(HISTORY_CLOCK_TAB_TAG, HistoryClockFragment.class);
        map.put(SETTINGS_TAB_TAG, SettingsFragment.class);
        map.put(EXTRAS_TAB_TAG, ExtrasFragment.class);
        DEFAULT_TAB_FRAGMENT_MAP = Collections.unmodifiableMap(map);
    }


    /*
    This map is used in order to map fragments into their tabs (in which tab the fragment
    should be shown)
     */
    public static final Map<Class, String> FRAGMENT_TO_TAB_MAP;
    static {
        Map<Class, String> map = new HashMap<Class, String>();
        map.put(HomeFragment.class, HOME_TAB_TAG);
        map.put(NewMeasurementFragment.class, HOME_TAB_TAG);
        map.put(EmptyFragment.class, HOME_TAB_TAG);
        map.put(HistoryFragment.class, HISTORY_LIST_TAB_TAG);
        map.put(HistoryClockFragment.class, HISTORY_CLOCK_TAB_TAG);
        map.put(SettingsFragment.class, SETTINGS_TAB_TAG);
        map.put(PersonalDataFragment.class, SETTINGS_TAB_TAG);
        map.put(DevicesListFragment.class, SETTINGS_TAB_TAG);
        map.put(ExtrasFragment.class, EXTRAS_TAB_TAG);
        FRAGMENT_TO_TAB_MAP = Collections.unmodifiableMap(map);
    }

    /*
       This map is used in order to map tab's tags into icons fot these tabs
    */
    public static final Map<String, Integer> TAB_ICON_MAP;
    static {
        Map<String, Integer> map = new HashMap<String, Integer>();
        map.put(HOME_TAB_TAG, R.drawable.ic_home_tab);
        map.put(HISTORY_TAB_TAG, R.drawable.ic_history_tab);
        map.put(HISTORY_LIST_TAB_TAG, R.drawable.ic_history_list_tab);
        map.put(HISTORY_CLOCK_TAB_TAG, R.drawable.ic_history_clock_tab);
        map.put(SETTINGS_TAB_TAG, R.drawable.ic_settings_tab);
        map.put(EXTRAS_TAB_TAG, R.drawable.ic_extras_tab);
        TAB_ICON_MAP = Collections.unmodifiableMap(map);
    }

}
