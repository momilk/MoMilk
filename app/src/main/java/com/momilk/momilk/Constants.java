package com.momilk.momilk;


/**
 * This class holds public constants. Constants which can be exposed without security/privacy
 * drawbacks go here.
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


}
