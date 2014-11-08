package com.momilk.momilk;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTabHost;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TabHost;
import android.widget.Toast;

import java.util.HashMap;


public class Main extends FragmentActivity implements TabHost.OnTabChangeListener {

    // Set this to true in order to load a very simple layout for debug of bluetooth connection
    private static final boolean BT_DEBUG_LAYOUT = false;

    private static final String LOG_TAG = "MainActivity";


    public static final String HOME_TAB_TAG = "home";
    public static final String HISTORY_TAB_TAG = "history";
    public static final String SETTINGS_TAB_TAG = "settings";
    public static final String EXTRAS_TAB_TAG = "extras";

    private BluetoothAdapter mBluetoothAdapter = null;

    private BluetoothChatService mChatService = null;

    private FragmentTabHost mTabHost;
    private HashMap mMapTabInfo = new HashMap();
    private TabInfo mLastTab = null;

    private String mConnectedDeviceName = null;


    /*
    This private class is used to store information about tabs.
     */
     private class TabInfo {
        private String mTag;
        private Class mClass;
        private Bundle mArgs;
        private FragmentContainer mFragmentContainer;

        TabInfo(String tag, Class clazz, Bundle args) {
            mTag = tag;
            mClass = clazz;
            mArgs = args;
        }

    }

    class TabFactory implements TabHost.TabContentFactory {

        private final Context mContext;

        public TabFactory(Context context) {
            mContext = context;
        }

        /*
        This method creates an empty view as a placeholder for a fragment
         */
        public View createTabContent(String tag) {
            View v = new View(mContext);
            v.setMinimumWidth(0);
            v.setMinimumHeight(0);
            return v;
        }

    }


    private void setStatus(int resId) {
        final ActionBar actionBar = getActionBar();
        actionBar.setSubtitle(resId);
    }


    private void setStatus(CharSequence subTitle) {
        final ActionBar actionBar = getActionBar();
        actionBar.setSubtitle(subTitle);
    }


    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            setStatus("Connected to: " + mConnectedDeviceName);
                            getCommunicationFragment();
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            setStatus("Connecting...");
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            setStatus("Not connected");
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    String writeMessage = (String) msg.obj;
                    break;
                case Constants.MESSAGE_READ:
//                    byte[] readBuf = (byte[]) msg.obj;
//                    // construct a string from the valid bytes in the buffer
//                    String readMessage = new String(readBuf, 0, msg.arg1);

                    String readMessage = (String) msg.obj;
                    Log.i(LOG_TAG, "Received message: " + readMessage);
                    BTCommDebugFragment f = getCommunicationFragment();
                    f.receivedNewMessage(readMessage);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case Constants.MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(Constants.TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };


    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                Log.i(LOG_TAG, "onReceive is called!");

                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                BluetoothClass deviceClass = intent.getParcelableExtra(BluetoothDevice.EXTRA_CLASS);
                // Add the name and address to an array adapter to show in a ListView


                BTDevicesDebugFragment devicesListFragment = getDevicesListFragment();
                devicesListFragment.add(device, deviceClass);

            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // Setup TabHost
        initializeTabHost(savedInstanceState);

        if (savedInstanceState != null) {
            mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab")); //set the tab as per the saved state
        }

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("tab", mTabHost.getCurrentTabTag()); //save the selected tab
        super.onSaveInstanceState(outState);
    }


    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
//        if (mChatService != null) {
//            // Only if the state is STATE_NONE, do we know that we haven't started already
//            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
//                // Start the Bluetooth chat services
//                mChatService.start();
//            }
//        }
    }


    @Override
    protected void onPause() {
        super.onPause();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mChatService != null) mChatService.stop();
        if(mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery();
        }

        try {
            this.unregisterReceiver(mReceiver);
        } catch (IllegalArgumentException e ) {
            // Do nothing - the receiver wasn't registered
        }
    }


    private void initializeTabHost(Bundle args) {
        mTabHost = (FragmentTabHost) findViewById(android.R.id.tabhost);
        mTabHost.setup(this, getSupportFragmentManager(), android.R.id.tabcontent);
        TabInfo tabInfo = null;


        if (BT_DEBUG_LAYOUT) {
            // This tab is used for BT debug
            Main.addTab(this, mTabHost,
                    mTabHost.newTabSpec(HOME_TAB_TAG).setIndicator(getString(R.string.home_tab_indicator)),
                    (tabInfo = new TabInfo(HOME_TAB_TAG, BTControlDebugFragment.class, args)));
            mMapTabInfo.put(tabInfo.mTag, tabInfo);

            // This tab is used for BT debug
            Main.addTab(this, mTabHost,
                    mTabHost.newTabSpec("comm").setIndicator("Comm"),
                    (tabInfo = new TabInfo("comm", BTCommDebugFragment.class, args)));
            mMapTabInfo.put(tabInfo.mTag, tabInfo);
        } else {

            Main.addTab(this, mTabHost,
                    mTabHost.newTabSpec(HOME_TAB_TAG).setIndicator(getString(R.string.home_tab_indicator)),
                    (tabInfo = new TabInfo(HOME_TAB_TAG, HomeFragment.class, args)));
            mMapTabInfo.put(tabInfo.mTag, tabInfo);
            Main.addTab(this, mTabHost,
                    mTabHost.newTabSpec(HISTORY_TAB_TAG).setIndicator(getString(R.string.history_tab_indicator)),
                    (tabInfo = new TabInfo(HISTORY_TAB_TAG, HistoryFragment.class, args)));
            mMapTabInfo.put(tabInfo.mTag, tabInfo);
            Main.addTab(this, mTabHost,
                    mTabHost.newTabSpec(SETTINGS_TAB_TAG).setIndicator(getString(R.string.history_tab_indicator)),
                    (tabInfo = new TabInfo(SETTINGS_TAB_TAG, SettingsFragment.class, args)));
            mMapTabInfo.put(tabInfo.mTag, tabInfo);
            Main.addTab(this, mTabHost,
                    mTabHost.newTabSpec(EXTRAS_TAB_TAG).setIndicator(getString(R.string.history_tab_indicator)),
                    (tabInfo = new TabInfo(EXTRAS_TAB_TAG, ExtrasFragment.class, args)));
            mMapTabInfo.put(tabInfo.mTag, tabInfo);
        }

        // Default to home tab
        this.onTabChanged(HOME_TAB_TAG);

        mTabHost.setOnTabChangedListener(this);
    }

    private static void addTab(Main activity, FragmentTabHost tabHost, TabHost.TabSpec tabSpec, TabInfo tabInfo) {
        // Attach a Tab view factory to the spec
        tabSpec.setContent(activity.new TabFactory(activity));
        String tag = tabSpec.getTag();

        // Check to see if we already have a fragment for this tab, probably
        // from a previously saved state.  If so, deactivate it, because our
        // initial state is that a tab isn't shown.
        tabInfo.mFragmentContainer = (FragmentContainer) activity.getSupportFragmentManager().findFragmentByTag(tag);
        if (tabInfo.mFragmentContainer != null && !tabInfo.mFragmentContainer.isDetached()) {
            FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
            ft.detach(tabInfo.mFragmentContainer);
            ft.commit();
            activity.getSupportFragmentManager().executePendingTransactions();
        }

        // Each tab contains FragmentContainer
        tabHost.addTab(tabSpec, FragmentContainer.class, null);
    }

    @Override
    public void onTabChanged(String tag) {
        TabInfo newTab = (TabInfo) this.mMapTabInfo.get(tag);
        if (mLastTab != newTab) {
            FragmentTransaction ft = this.getSupportFragmentManager().beginTransaction();
            if (mLastTab != null) {
                if (mLastTab.mFragmentContainer != null) {
                    ft.detach(mLastTab.mFragmentContainer);
                }
            }
            if (newTab != null) {
                if (newTab.mFragmentContainer == null) {
                    Log.i(LOG_TAG, "creating new FragmentContainer for " + newTab.mClass.getSimpleName());
                    newTab.mFragmentContainer = FragmentContainer.newInstance(newTab.mClass.getName());
                    ft.add(android.R.id.tabcontent, newTab.mFragmentContainer, newTab.mTag);
                } else {
                    Log.i(LOG_TAG, "attaching existing FragmentContainer for " + newTab.mClass.getSimpleName());
                    ft.attach(newTab.mFragmentContainer);
                }
            }

            mLastTab = newTab;
            ft.commit();
            this.getSupportFragmentManager().executePendingTransactions();

        }

        if (newTab != null) {
            newTab.mFragmentContainer.setDefaultContent(null);
        } else {
            Log.e(LOG_TAG, "newTab is null!");
        }

    }

    private void setupChat() {

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);

    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    public void startListening() {
        ensureDiscoverable();
        if (mChatService == null) {
            setupChat();
        }
        mChatService.start();
    }

    public void deviceSelected(final BluetoothDevice device) {
        Log.i(LOG_TAG, "deviceSelected is called on device: " + device.getName());


        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        connectToDevice(device);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Connect to " + device.getName()).setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();

    }

    private void connectToDevice(BluetoothDevice device) {
        if (mChatService == null) {
            setupChat();
        }
        mChatService.connect(device, true);

    }


    public void discoverDevices() {

        Log.i(LOG_TAG, "discoverDevices is called!");

        getDevicesListFragment();

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Your device does not support bluetooth",
                    Toast.LENGTH_LONG).show();
        }
        else if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT);
            //TODO: there is a potentially endless loop which ends only when the user
            //TODO: enables BT. Check for the solution - the user should be
            //TODO: prompted just once.
        }
        else {
            if(mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
            }
            if (!mBluetoothAdapter.startDiscovery()) {
                Toast.makeText(this, "Discovery initiation encountered an error", Toast.LENGTH_LONG).show();
                return;
            }

            // Register the BroadcastReceiver
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mReceiver, filter); // TODO: Don't forget to unregister during connect
        }
    }

        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            switch (requestCode) {
                case Constants.REQUEST_ENABLE_BT:
                    // When the request to enable Bluetooth returns
                    if (resultCode == Activity.RESULT_OK) {
                        // Bluetooth is now enabled, so set up a chat session
                        discoverDevices();
                    } else {
                        Toast.makeText(this, "Bluetooth is disabled - aborting", Toast.LENGTH_LONG).show();

                    }
                    default:
                        break;
            }
        }


    public void sendMessage(String message) {
        //TODO: write this method
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, "Not connected to any device", Toast.LENGTH_SHORT).show();
            return;
        }


        try {
            // Check that there's actually something to send
            if (message.length() > 0) {
                // Get the message bytes and tell the BluetoothChatService to write
                // Adding newline char in order to be able to parse messages as lines
                mChatService.write(message + "\n");
            }
        } catch (NullPointerException e) {
            Log.e(LOG_TAG, "NullPointer");
        }
    }

    private BTDevicesDebugFragment getDevicesListFragment() {

        mTabHost.setCurrentTabByTag(HOME_TAB_TAG);

        FragmentContainer fc =
                (FragmentContainer) getSupportFragmentManager().findFragmentById(android.R.id.tabcontent);

        if (fc == null) {
            Log.e(LOG_TAG, "FragmentContainer for the current tab is null");
        }
        return (BTDevicesDebugFragment) fc.replaceContent(BTDevicesDebugFragment.class, null);
    }


    private BTCommDebugFragment getCommunicationFragment() {

        mTabHost.setCurrentTabByTag("comm");

        FragmentContainer fc =
                (FragmentContainer) getSupportFragmentManager().findFragmentById(android.R.id.tabcontent);

        if (fc == null) {
            Log.e(LOG_TAG, "FragmentContainer for the current tab is null");
        }
        return (BTCommDebugFragment) fc.replaceContent(BTCommDebugFragment.class, null);




    }
}
