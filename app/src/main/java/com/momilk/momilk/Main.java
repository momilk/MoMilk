package com.momilk.momilk;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.mtp.MtpConstants;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTabHost;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.Toast;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Stack;


public class Main extends FragmentActivity implements
        TabHost.OnTabChangeListener,
        HomeFragment.HomeFragmentCallback,
        DevicesListFragment.DevicesListFragmentCallback,
        HistoryFragment.HistoryFragmentCallback,
        ExtrasFragment.ExtrasFragmentCallback,
        NewMeasurementFragment.NewMeasurementFragmentCallback,
        SettingsFragment.SettingsFragmentCallback {

    // Set this to true in order access various debug features of the app (through ActionBar)
    public static final boolean ENABLE_DEBUG = false;

    private static final int RERUN_DISCOVER_BLUETOOTH_DEVICES = 0;
    private static final int RERUN_ON_SYNC_CLICK = 1;
    private static final int RERUN_CONNECT_TO_DEFAULT_DEVICE = 2;
    private static final int RERUN_ON_SET_DEFAULT_DEVICE_CLICK = 3;

    private static final String LOG_TAG = "MainActivity";

    // This static context will be used in a static methods of this class
    private static Context context = null;


    private BluetoothAdapter mBluetoothAdapter = null;

    private BluetoothService mBluetoothService = null;
    private int mBluetoothServiceState = BluetoothService.STATE_NONE;

    private SyncWithDeviceThread mSyncWithDeviceThread = null;

    private String[] mCurrentTabsGroup;

    private FragmentTabHost mTabHost;
    private HashMap<String, TabInfo> mMapTabInfo = new HashMap<String, TabInfo>();
    private TabInfo mCurrentTabInfo = null;

    private String mConnectedDeviceName = null;

    private CustomDatabaseAdapter mDBAdapter;

    // These variables alongside RERUN_... constants will be used in order to rerun methods
    // that have dependencies on either async events or user actions.
    private Stack<Integer> mRerunMethodStack;

    // The Handler that gets information back from the BluetoothService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                                rerunMethod();
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            // If the previous state was CONNECTING then the connection was unsuccessful
                            // and we need to clear the rerun stack
                            if (mBluetoothServiceState == BluetoothService.STATE_CONNECTING) {
                                mRerunMethodStack.clear();
                            }
                            break;
                    }
                    mBluetoothServiceState = msg.arg1;
                    break;
                case Constants.MESSAGE_WRITE:
                    String writeMessage = (String) msg.obj;
                    debugToast("Sending:\n" + writeMessage, false);
                    break;
                case Constants.MESSAGE_READ:
                    String readMessage = (String) msg.obj;
                    debugToast("Received:\n" + readMessage, false);
                    if (mSyncWithDeviceThread != null && mSyncWithDeviceThread.isAlive()) {
                        mSyncWithDeviceThread.newIncomingMessage(readMessage);
                    } else {
                        Log.e(LOG_TAG, "Incoming message wasn't utilized!");
                    }
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    debugToast("Connected to " + mConnectedDeviceName, false);
                    break;
                case Constants.MESSAGE_TOAST:
                    final Toast toast = Toast.makeText(getApplicationContext(), msg.getData().getString(Constants.TOAST),
                            Toast.LENGTH_LONG);
                    toast.show();
                    break;
            }
        }
    };


    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                FragmentContainer fc = (FragmentContainer) getSupportFragmentManager()
                        .findFragmentById(android.R.id.tabcontent);

                // Update the list of devices as long as DevicesListFragment is still shown
                if (fc.getChildFragmentManager().findFragmentById(R.id.fragment_content) instanceof
                        DevicesListFragment) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    DevicesListFragment devicesListFragment = (DevicesListFragment)
                            fc.getChildFragmentManager().findFragmentById(R.id.fragment_content);
                    devicesListFragment.add(device);
                }
            }
            else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        cancelBluetoothActivities();
                        break;
                    case BluetoothAdapter.STATE_ON:
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        break;
                    default:
                        break;
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initializing static context
        if (Main.context == null) {
            Main.context = getApplicationContext();
        }

        // Setup TabHost
        initializeTabHost(savedInstanceState);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        mDBAdapter = new CustomDatabaseAdapter(this);

        mRerunMethodStack = new Stack<Integer>();

        if (ENABLE_DEBUG) {
            ActionBar actionBar= getActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("tab", mTabHost.getCurrentTabTag()); //save the selected tab
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        super.onPause();

        try {
            this.unregisterReceiver(mReceiver);
        } catch (IllegalArgumentException e ) {
            // Do nothing - the receiver wasn't registered
        }
    }

    @Override
    protected void onStop() {
        cancelBluetoothActivities();
        super.onStop();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);

        rerunMethod();
    }

    @Override
    public void onBackPressed() {
        // This implementation was used because there is a known bug with backstack management
        // of nested fragments: https://code.google.com/p/android/issues/detail?id=40323

        // This implementation was taken from StackOverflow:
        // http://stackoverflow.com/questions/13418436/android-4-2-back-stack-behaviour-with-nested-fragments

        // It is an overkill in our case because we know the depth of our fragments hierarchy,
        // but, nevertheless, I decided to use it because it is a beautiful piece of code :)

        FragmentManager fm = getSupportFragmentManager();
        if (traverseBackStackRecursively(fm)) {
            return;
        }
        super.onBackPressed();
    }

    private boolean traverseBackStackRecursively(FragmentManager fm) {
        if (fm != null) {
            if (fm.getBackStackEntryCount() > 0) {
                fm.popBackStack();
                return true;
            }

            List<Fragment> fragList = fm.getFragments();
            if (fragList != null && fragList.size() > 0) {
                for (Fragment frag : fragList) {
                    if (frag == null) {
                        continue;
                    }
                    if (frag.isVisible()) {
                        if (traverseBackStackRecursively(frag.getChildFragmentManager())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (ENABLE_DEBUG) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.main_activity_actions, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_clear_db_table:
                mDBAdapter.clearTable();
                return true;
            case R.id.action_reset_default_device:
                getSharedPreferences(Constants.PREFERENCE_FILE, FragmentActivity.MODE_PRIVATE).edit().
                        remove("preference_default_device_address").commit();
                getSharedPreferences(Constants.PREFERENCE_FILE, FragmentActivity.MODE_PRIVATE).edit().
                        remove("preference_default_device_name").commit();
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Constants.REQUEST_ENABLE_BT:
                if (resultCode != Activity.RESULT_OK) {
                    // The assumption here is that when the flow is aborted, all the
                    // methods which should've been rerun can be discarded
                    mRerunMethodStack.clear();
                    Toast.makeText(this, "Bluetooth is disabled - aborting", Toast.LENGTH_LONG).show();
                }
            default:
                break;
        }
    }

    /*
    This method accepts the index and registers this index for being rerun at a later stage
     */
    @SuppressWarnings("UnnecessaryBoxing")
    private void registerForRerun(int index) {
        mRerunMethodStack.push(Integer.valueOf(index));
    }


    /*
    This metod pops an index from the top of rerun stack and executes the appropriate method.
    If the stack is empty the call to this method does nothing.
     */
    private void rerunMethod() {
        if (!mRerunMethodStack.empty()) {
            int index = mRerunMethodStack.pop();
            switch (index) {
                case RERUN_DISCOVER_BLUETOOTH_DEVICES:
                    discoverBluetoothDevices();
                    Log.d(LOG_TAG, "reruning discoverBluetoothDevices()");
                    break;
                case RERUN_ON_SYNC_CLICK:
                    onSyncClick();
                    Log.d(LOG_TAG, "reruning onSyncClick()");
                    break;
                case RERUN_CONNECT_TO_DEFAULT_DEVICE:
                    connectToDefaultDevice();
                    Log.d(LOG_TAG, "reruning connectToDefaultDevice()");
                    break;
                case RERUN_ON_SET_DEFAULT_DEVICE_CLICK:
                    onSetDefaultDeviceClick();
                    Log.d(LOG_TAG, "reruning onSetDefaultDeviceClick()");
                    break;
                default:
                    break;
            }
        } else {
            Log.d(LOG_TAG, "rerun stack is empty");
        }
    }

    // -------------------------------------------------------------------------------------------
    //
    // Tabs management logic (classes and methods)
    //
    // -------------------------------------------------------------------------------------------

    private void initializeTabHost(Bundle savedInstanceState) {
        mTabHost = (FragmentTabHost) findViewById(android.R.id.tabhost);
        mTabHost.setup(this, getSupportFragmentManager(), android.R.id.tabcontent);

        // Create TabInfo's for all tabs
        for (String tabTag : Constants.DEFAULT_TAB_FRAGMENT_MAP.keySet()) {
            mMapTabInfo.put(tabTag, new TabInfo(tabTag));
        }

        if (savedInstanceState != null) {
            // Set the tab as per the saved state
            onTabChanged(savedInstanceState.getString("tab"));
        } else {
            // Default to home tab
            onTabChanged(Constants.HOME_TAB_TAG);
        }

        mTabHost.setOnTabChangedListener(this);
    }

    /*
    This metod clears the tabs host from all the tabs and then adds the tabs specified in
    input array
     */
    private void setTabsByGroup(String[] tabsGroup, Bundle args) {


        if (mCurrentTabsGroup != null && mCurrentTabsGroup == tabsGroup) {
            // The currently shown tab group is the same as the requested one
            return;
        }

        mCurrentTabsGroup = tabsGroup;

        // clearAllTabs does not work if the current tab is not set to 0 - this is a known
        // bug which has not been fixed for years.
        // Bug ticket: https://code.google.com/p/android/issues/detail?id=2772
        //
        // Just setting the tab to HOME works, but shows the home fragment for a fraction of
        // the second during the change, which is really annoying.
        // In order to prevent this, show the empty fragment first (which will also set
        // the current tab to 0 - HOME)
        if (mTabHost.getTabWidget().getChildCount() > 0) {
            getFragment(EmptyFragment.class);
            mTabHost.clearAllTabs();
        }


        for (String tabTag : tabsGroup) {
            Main.addTab(this, mTabHost, mTabHost.newTabSpec(tabTag), mMapTabInfo.get(tabTag));
        }


        // Since all the tabs got replaced - need to restore the currently selected tab
        mTabHost.setCurrentTabByTag(mCurrentTabInfo.mTag);

        addTabSpecificListeners();
    }


    /*
    This static method is used in order to add a new tab to the app
     */
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

        View tabView = LayoutInflater.from(activity).inflate(R.layout.tab, null);
        // This tag will be used in onTouchListener for each tab
        // independently (see addTabSpecificListeners())
        tabView.setTag(tabInfo.mTag);

        ImageView image = (ImageView) tabView.findViewById(R.id.tab_icon);
        image.setImageResource(tabInfo.mIconId);

        tabSpec.setIndicator(tabView);

        // Each tab contains FragmentContainer
        tabHost.addTab(tabSpec, FragmentContainer.class, null);
    }



    /*
    This method adds onTouchListener's to each tab. This is required in order to allow a click
    on a currently selected tab to revert this tab to its default fragment (because in this
    case onTabChanged callback will not be called)
     */
    private void addTabSpecificListeners() {

        int numberOfTabs = mTabHost.getTabWidget().getChildCount();
        for (int i = 0; i < numberOfTabs; i++) {
            mTabHost.getTabWidget().getChildAt(i).setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        TabInfo newTabInfo = (TabInfo) mMapTabInfo.get((String) view.getTag());
                        Log.d(LOG_TAG, "currentTabTag: " + mCurrentTabInfo.mTag +
                                " newTabTag: " + newTabInfo.mTag);
                        if (mCurrentTabInfo == newTabInfo) {
                            // Here we handle only clicks on already selected tabs
                            // All other cases will be covered by onTabChanged callback
                            mCurrentTabInfo.mFragmentContainer.setDefaultContent(null);
                        }
                    }
                    return false;
                }
            });
        }
    }


    /**
     * This callback contains tab changing logic. Please note that this callback is not expected to
     * handle clicks on already selected tabs.
     * @param tag
     */
    @Override
    public void onTabChanged(String tag) {
        TabInfo newTabInfo = (TabInfo) mMapTabInfo.get(tag);

        // When the user clicks on HISTORY tab, he is automatically redirected to HISTORY_LIST tab
        if (newTabInfo.mTag.equals(Constants.HISTORY_TAB_TAG)) {
            onTabChanged(Constants.HISTORY_LIST_TAB_TAG);
            return;
        }


        if (mCurrentTabInfo != newTabInfo) {
            // During execution of this method (and methods called from it) there will be tabs
            // switching, but we don't want onTabChanged to be called again as a result of these
            // switches. Unregister now and register back at the end of the method
            mTabHost.setOnTabChangedListener(null);

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

            if (mCurrentTabInfo != null) {
                if (mCurrentTabInfo.mFragmentContainer != null) {
                    ft.detach(mCurrentTabInfo.mFragmentContainer);
                }
            }

            mCurrentTabInfo = newTabInfo;

            // Choose tabs to be shown
            String[] tabsGroup;
            if (newTabInfo.mTag.equals(Constants.HISTORY_TAB_TAG) ||
                    newTabInfo.mTag.equals(Constants.HISTORY_LIST_TAB_TAG) ||
                    newTabInfo.mTag.equals(Constants.HISTORY_CLOCK_TAB_TAG)) {
                tabsGroup = Constants.HISTORY_TABS_GROUP;
            } else {
                tabsGroup = Constants.DEFAULT_TABS_GROUP;
            }

            // Show the chosen tabs
            setTabsByGroup(tabsGroup, null);

            // Highlight the current tab
            refreshTabBackgrounds();

            // Create a FragmentContainer for the tab or attach the existing one
            if (newTabInfo.mFragmentContainer == null) {
                Log.d(LOG_TAG, "creating new FragmentContainer for " + newTabInfo.mClass.getSimpleName());
                newTabInfo.mFragmentContainer = FragmentContainer.newInstance(newTabInfo.mClass.getName());
                ft.add(android.R.id.tabcontent, newTabInfo.mFragmentContainer, newTabInfo.mTag);
            } else {
                Log.d(LOG_TAG, "attaching existing FragmentContainer for " + newTabInfo.mClass.getSimpleName());
                ft.attach(newTabInfo.mFragmentContainer);
            }

            ft.commit();
            this.getSupportFragmentManager().executePendingTransactions();



            // Set default content for the Fragment container
            newTabInfo.mFragmentContainer.setDefaultContent(null);

            // Set the listener back
            mTabHost.setOnTabChangedListener(this);

        }
    }

    private void refreshTabBackgrounds() {
        int defaultTabColor = R.color.white;
        int selectedTabColor = R.color.background;

        int numOfTabs = mTabHost.getTabWidget().getTabCount();
        View tabView;
        for(int i=0; i < numOfTabs; i++) {
            tabView = mTabHost.getTabWidget().getChildTabViewAt(i);

            tabView.setBackgroundColor(getResources().getColor(defaultTabColor));
            tabView.findViewById(R.id.tab_icon).setBackgroundColor(getResources().getColor(defaultTabColor));
        }

        View currentTabView = mTabHost.getCurrentTabView();
        currentTabView.setBackgroundColor(getResources().getColor(selectedTabColor));
        View currentIcon = currentTabView.findViewById(R.id.tab_icon);
        currentIcon.setBackgroundColor(getResources().getColor(selectedTabColor));
    }

    // -------------------------------------------------------------------------------------------
    //
    // Bluetooth management (init and messaging)
    //
    // -------------------------------------------------------------------------------------------


    /**
     * This method sends a message over bluetooth channel if there is a connected device. Newline
     * char is appended at the end in order to allow processing messages in a line-by-line
     * manner
     * @param message
     */
    public void sendMessage(String message) {
        if (mBluetoothService == null) {
            setupBluetoothService();
        }
        // Check that we're actually connected before trying anything
        if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, "Not connected to any device", Toast.LENGTH_SHORT).show();
            return;
        }
        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothService to write
            // Adding newline char in order to be able to parse messages as lines
            mBluetoothService.write(message + "\n");
        }
    }




    private void setupBluetoothService() {
        // Initialize the BluetoothService to perform bluetooth connections
        mBluetoothService = new BluetoothService(this, mHandler);

    }


    public void startListening() {
        ensureDiscoverable();
        if (mBluetoothService == null) {
            setupBluetoothService();
        }
        mBluetoothService.start();
    }


    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }


    public void discoverBluetoothDevices() {

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Your device does not support bluetooth",
                    Toast.LENGTH_LONG).show();
        }
        else if (!mBluetoothAdapter.isEnabled()) {
            // This will cause this method to be rerun during onActivityResult execution
            registerForRerun(RERUN_DISCOVER_BLUETOOTH_DEVICES);

            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT);
        }
        else {

            if(mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
            }
            if (!mBluetoothAdapter.startDiscovery()) {
                Toast.makeText(this, "Bluetooth discovery encountered an error", Toast.LENGTH_LONG).show();
            }

        }
    }


    private void connectToDefaultDevice() {
        SharedPreferences sharedPref = getSharedPreferences(Constants.PREFERENCE_FILE,
                FragmentActivity.MODE_PRIVATE);
        String deviceAddressKey = "preference_default_device_address";
        if (!sharedPref.contains(deviceAddressKey)) {
            // The assumption here is that when the flow is aborted, all the
            // methods which should've been rerun can be discarded
            mRerunMethodStack.clear();
            Toast.makeText(this, "Default device is not set - aborting", Toast.LENGTH_LONG).show();
        } else if (!mBluetoothAdapter.isEnabled()) {
            // This will cause this method to be rerun during onActivityResult execution
            registerForRerun(RERUN_CONNECT_TO_DEFAULT_DEVICE);

            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT);
        } else {
            if (mBluetoothService == null) {
                setupBluetoothService();
            }
            mBluetoothService.connect(mBluetoothAdapter.
                    getRemoteDevice(sharedPref.getString(deviceAddressKey, null)), true);
        }

    }

    private void cancelBluetoothActivities() {
        if (mBluetoothService != null)
            mBluetoothService.stop();
        if (mBluetoothAdapter != null)
            mBluetoothAdapter.cancelDiscovery();
        if (mSyncWithDeviceThread != null)
            mSyncWithDeviceThread.cancel();
    }


    // -------------------------------------------------------------------------------------------
    //
    // Callback methods for interaction with HomeFragment
    //
    // -------------------------------------------------------------------------------------------

    @Override
    public void onNewMeasurementClick() {

        getFragment(NewMeasurementFragment.class);

    }


    @Override
    public void onSyncClick() {

        if ((mSyncWithDeviceThread != null && mSyncWithDeviceThread.isAlive()) ||
                mRerunMethodStack.contains(Integer.valueOf(RERUN_ON_SYNC_CLICK))) {
            // Do nothing if there is a Sync thread already running
            return;
        }

        if (mBluetoothService == null) {
            setupBluetoothService();
        }

        if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            registerForRerun(RERUN_ON_SYNC_CLICK);
            connectToDefaultDevice();
        } else {
            mSyncWithDeviceThread = new SyncWithDeviceThread(mBluetoothService, mDBAdapter,
                    mHandler, getSharedPreferences(Constants.PREFERENCE_FILE, FragmentActivity.MODE_PRIVATE), getApplicationContext());
            mSyncWithDeviceThread.start();
        }

    }



    // -------------------------------------------------------------------------------------------
    //
    // Callback methods for interaction with NewMeasurementFragment
    //
    // -------------------------------------------------------------------------------------------

    @Override
    public void startNewMeasurement() {

    }



    // -------------------------------------------------------------------------------------------
    //
    // Callback methods for interaction with DevicesListFragment
    //
    // -------------------------------------------------------------------------------------------


    public void deviceSelected(final BluetoothDevice device) {

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        SharedPreferences sharedPref = getSharedPreferences(Constants.PREFERENCE_FILE, Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putString("preference_default_device_name", device.getName());
                        editor.putString("preference_default_device_address", device.getAddress());
                        editor.commit();
                        // Show the default settings fragment after device has been chosen
                        getFragment(SettingsFragment.class);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            }
        };

        String message = getString(R.string.preference_default_device_dialog_message_part1) + " "
                + device.getName() + " " + getString(R.string.preference_default_device_dialog_message_part2);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
                .setPositiveButton(getString(R.string.dialog_positive_text), dialogClickListener)
                .setNegativeButton(getString(R.string.dialog_negative_text), dialogClickListener).show();

    }

    public Set<BluetoothDevice> getPairedDevices() {
        return mBluetoothAdapter.getBondedDevices();
    }

    // -------------------------------------------------------------------------------------------
    //
    // Callback methods for interaction with HistoryFragment
    //
    // -------------------------------------------------------------------------------------------

    @Override
    public CustomDatabaseAdapter getHistoryDatabaseAdapter() {
        return mDBAdapter;
    }


    // -------------------------------------------------------------------------------------------
    //
    // Callback methods for interaction with ExtrasFragment
    //
    // -------------------------------------------------------------------------------------------

    @Override
    public void onExtraSelected(String extraTag) {
        // TODO: complete this method
    }



    // -------------------------------------------------------------------------------------------
    //
    // Callback methods for interaction with SettingsFragment
    //
    // -------------------------------------------------------------------------------------------

    @Override
    public void onPersonalDataClick() {
        getFragment(PersonalDataFragment.class);

    }

    @Override
    public void onSetDefaultDeviceClick() {

        if (!mBluetoothAdapter.isEnabled()) {
            // This will cause this method to be rerun during onActivityResult execution
            registerForRerun(RERUN_ON_SET_DEFAULT_DEVICE_CLICK);

            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT);
        } else {
            getFragment(DevicesListFragment.class);
            discoverBluetoothDevices();
        }
    }

    @Override
    public void onHistoryClearClick() {
        DialogInterface.OnClickListener clearHistoryClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        mDBAdapter.clearTable();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.preference_clear_history_dialog_message))
                .setPositiveButton(getString(R.string.dialog_positive_text), clearHistoryClickListener)
                .setNegativeButton(getString(R.string.dialog_negative_text), clearHistoryClickListener).show();
    }


    // -------------------------------------------------------------------------------------------
    //
    // Method for switching fragments and tabs
    //
    // -------------------------------------------------------------------------------------------


    private Fragment getFragment(Class fragmentClass) {

        // Get the tab in which this fragment should be shown
        String tabTag = Constants.FRAGMENT_TO_TAB_MAP.get(fragmentClass);

        if (tabTag == null) {
            Log.e(LOG_TAG, "Fragment " + fragmentClass.getSimpleName() + " is not mapped to" +
                    " any tab in Constants.FRAGMENT_TO_TAB_MAP");
        }

        // Switch tab if needed
        if (mCurrentTabInfo == null || !mCurrentTabInfo.mTag.equals(tabTag)) {
            mTabHost.setCurrentTabByTag(tabTag);
        }

        FragmentContainer fc =
                (FragmentContainer) getSupportFragmentManager().findFragmentById(android.R.id.tabcontent);

        if (fc == null) {
            Log.e(LOG_TAG, "FragmentContainer for tab " + tabTag + " is null");
        }

        return fc.replaceContent(fragmentClass, null);

    }


    // -------------------------------------------------------------------------------------------
    //
    // Inner classes
    //
    // -------------------------------------------------------------------------------------------


    /*
    This private class is used to store information about tabs.
    */
    private class TabInfo {
        private String mTag;
        private Class mClass;
        private int mIconId;

        @SuppressWarnings("UnusedDeclaration") // Keep it just in case
        private Bundle mArgs;

        private FragmentContainer mFragmentContainer;

        TabInfo(String tag) {
            mTag = tag;
            mClass = Constants.DEFAULT_TAB_FRAGMENT_MAP.get(tag);
            mIconId = Constants.TAB_ICON_MAP.get(tag);
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

    public static void debugToast(String message, boolean increaseDuration) {
        if (ENABLE_DEBUG) {
            final Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
            toast.show();

            // Just dirty workaround to show a longer toast
            if (increaseDuration) {
                new CountDownTimer(3000, 1000) {
                    public void onTick(long millisUntilFinished) {
                        toast.show();
                    }

                    public void onFinish() {
                        toast.show();
                    }
                }.start();
            }
        }
    }

}
