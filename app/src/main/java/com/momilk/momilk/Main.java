package com.momilk.momilk;

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
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTabHost;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Stack;


public class Main extends FragmentActivity implements
        TabHost.OnTabChangeListener, HomeFragment.HomeFragmentCallback,
        BTDevicesListFragment.BTDevicesListFragmentCallback,
        HistoryFragment.HistoryFragmentCallback,
        ExtrasFragment.ExtrasFragmentCallback,
        NewMeasurementFragment.NewMeasurementFragmentCallback {

    // Set this to true in order to load a very simple layout for debug of bluetooth connection
    private static final boolean BT_DEBUG_LAYOUT = false;



    private static final int RERUN_LIST_BLUETOOTH_DEVICES = 0;
    private static final int RERUN_ON_SYNC_CLICK = 1;


    private static final String LOG_TAG = "MainActivity";

    private BluetoothAdapter mBluetoothAdapter = null;

    private BluetoothService mBluetoothService = null;

    private SyncWithDeviceThread mSyncWithDeviceThread = null;

    private FragmentTabHost mTabHost;
    private HashMap<String, TabInfo> mMapTabInfo = new HashMap<String, TabInfo>();
    private TabInfo mCurrentTabInfo = null;

    private String mConnectedDeviceName = null;

    private CustomDatabaseAdapter mDBAdapter;

    // These variables alongside RERUN_... constants will be used in order to get back to methods
    // that have dependencies on either async events or user actions.
    private Stack<Integer> mRerunMethodStack;

    // This variable will be set if there is need to switch a fragment when
    // commiting FragmentTransaction will cause IllegalStateException due to state loss.
    // If this variable is not null, then the fragment will be switched in onPostResume().
    private Class<? extends Fragment> mNextFragmentClass = null;


    // The Handler that gets information back from the BluetoothService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            //setStatus("Connected to: " + mConnectedDeviceName);
                            if (BT_DEBUG_LAYOUT) {
                                getFragment(BTCommDebugFragment.class);
                            } else {
                                rerunMethod();
                            }
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            //setStatus("Connecting...");
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            //setStatus("Not connected");
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    @SuppressWarnings("UnusedDeclaration") // This might come in handy at some point
                    String writeMessage = (String) msg.obj;
                    break;
                case Constants.MESSAGE_READ:
                    String readMessage = (String) msg.obj;
                    if (BT_DEBUG_LAYOUT) {
                        BTCommDebugFragment f =
                                (BTCommDebugFragment) getFragment(BTCommDebugFragment.class);
                        f.newMessage(readMessage);
                    } else {
                        if (mSyncWithDeviceThread != null && mSyncWithDeviceThread.isAlive()) {
                            mSyncWithDeviceThread.newIncomingMessage(readMessage);
                        } else {
                            Log.e(LOG_TAG, "Incoming message wasn't utilized!");
                        }
                    }
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case Constants.MESSAGE_TOAST:
                    final Toast toast = Toast.makeText(getApplicationContext(), msg.getData().getString(Constants.TOAST),
                            Toast.LENGTH_SHORT);
                    toast.show();

                    // Just dirty workaround to show a longer toast
                    new CountDownTimer(3000, 1000)
                    {

                        public void onTick(long millisUntilFinished) {toast.show();}
                        public void onFinish() {toast.show();}

                    }.start();
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


                BTDevicesListFragment devicesListFragment =
                        (BTDevicesListFragment) getFragment(BTDevicesListFragment.class);
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

        mDBAdapter = new CustomDatabaseAdapter(this);

        mRerunMethodStack = new Stack<Integer>();
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
//        if (mBluetoothService != null) {
//            // Only if the state is STATE_NONE, do we know that we haven't started already
//            if (mBluetoothService.getState() == BluetoothService.STATE_NONE) {
//                // Start the Bluetooth chat services
//                mBluetoothService.start();
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
        if (mBluetoothService != null) mBluetoothService.stop();
        if(mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery();
        }

        try {
            this.unregisterReceiver(mReceiver);
        } catch (IllegalArgumentException e ) {
            // Do nothing - the receiver wasn't registered
        }
    }


    @Override
    protected void onPostResume() {
        super.onPostResume();

        Log.d(LOG_TAG, "onPostResume() is called");

        if (mNextFragmentClass != null) {
            getFragment(mNextFragmentClass);
            mNextFragmentClass = null;
        }

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


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Constants.REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Fragment transactions can't be executed from within onActivityResult,
                    // therefore need to schedule it to be executed during onPostResume.
                    scheduleFragmentChange(BTDevicesListFragment.class);
                    rerunMethod();
                } else {
                    Toast.makeText(this, "Bluetooth is disabled - aborting", Toast.LENGTH_LONG).show();
                }
            default:
                break;
        }
    }

    private void scheduleFragmentChange(Class<? extends Fragment> claz) {
        if (mNextFragmentClass != null) {
            Log.e(LOG_TAG, "owerwriting mNextFragmentClass which is not null. Current: " +
            mNextFragmentClass.getSimpleName() + " New: " + claz.getSimpleName());
        }
        mNextFragmentClass = claz;
    }


    /*
    This method accepts the index and registers this index for being rerun at a later stage
     */
    @SuppressWarnings("UnnecessaryBoxing")
    private void registerForRerun(int index) {
        mRerunMethodStack.push(Integer.valueOf(index));
    }


    /*
    This metod pops an index from the top of rerun stack and executes an appropriate method
     */
    private void rerunMethod() {
        if (!mRerunMethodStack.empty()) {
            int index = mRerunMethodStack.pop();
            switch (index) {
                case RERUN_LIST_BLUETOOTH_DEVICES:
                    listBluetoothDevices();
                    break;
                case RERUN_ON_SYNC_CLICK:
                    onSyncClick();
                    break;
                default:
                    break;
            }
        } else {
            Log.e(LOG_TAG, "rerun was requested, but the method to rerun wasn't set");
        }
    }
    // -------------------------------------------------------------------------------------------
    //
    // Tabs management logic (classes and methods)
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

        TabInfo(String tag, Bundle args) {
            mTag = tag;
            mClass = Constants.DEFAULT_TAB_FRAGMENT_MAP.get(tag);
            mIconId = Constants.TAB_ICON_MAP.get(tag);
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


    private void initializeTabHost(Bundle args) {
        mTabHost = (FragmentTabHost) findViewById(android.R.id.tabhost);
        mTabHost.setup(this, getSupportFragmentManager(), android.R.id.tabcontent);
        TabInfo tabInfo;
        Main.addTab(this, mTabHost, mTabHost.newTabSpec(Constants.HOME_TAB_TAG),
                tabInfo = new TabInfo(Constants.HOME_TAB_TAG, args));
        mMapTabInfo.put(tabInfo.mTag, tabInfo);
        Main.addTab(this, mTabHost, mTabHost.newTabSpec(Constants.SETTINGS_TAB_TAG),
                tabInfo = new TabInfo(Constants.SETTINGS_TAB_TAG, args));
        mMapTabInfo.put(tabInfo.mTag, tabInfo);
        Main.addTab(this, mTabHost, mTabHost.newTabSpec(Constants.HISTORY_TAB_TAG),
                tabInfo = new TabInfo(Constants.HISTORY_TAB_TAG, args));
        mMapTabInfo.put(tabInfo.mTag, tabInfo);
        Main.addTab(this, mTabHost, mTabHost.newTabSpec(Constants.EXTRAS_TAB_TAG),
                tabInfo = new TabInfo(Constants.EXTRAS_TAB_TAG, args));
        mMapTabInfo.put(tabInfo.mTag, tabInfo);


        if (BT_DEBUG_LAYOUT) {

            // TODO: remove this part and implement debug features via action bar visibility
            // This tab is used for BT debug
            Main.addTab(this, mTabHost, mTabHost.newTabSpec(Constants.CTRL_TAB_TAG),
                    tabInfo = new TabInfo(Constants.CTRL_TAB_TAG, args));
            mMapTabInfo.put(tabInfo.mTag, tabInfo);

            // TODO: remove this part and implement debug features via action bar visibility
            // This tab is used for BT debug
            Main.addTab(this, mTabHost, mTabHost.newTabSpec(Constants.COMM_TAB_TAG),
                    tabInfo = new TabInfo(Constants.COMM_TAB_TAG, args));
            mMapTabInfo.put(tabInfo.mTag, tabInfo);
        }

        addTabSpecificListeners();

        // Default to home tab
        onTabChanged(Constants.HOME_TAB_TAG);

        mTabHost.setOnTabChangedListener(this);
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



    @Override
    public void onTabChanged(String tag) {
        TabInfo newTabInfo = (TabInfo) mMapTabInfo.get(tag);
        if (mCurrentTabInfo != newTabInfo) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            if (mCurrentTabInfo != null) {
                if (mCurrentTabInfo.mFragmentContainer != null) {
                    ft.detach(mCurrentTabInfo.mFragmentContainer);
                }
            }
            if (newTabInfo != null) {
                if (newTabInfo.mFragmentContainer == null) {
                    Log.d(LOG_TAG, "creating new FragmentContainer for " + newTabInfo.mClass.getSimpleName());
                    newTabInfo.mFragmentContainer = FragmentContainer.newInstance(newTabInfo.mClass.getName());
                    ft.add(android.R.id.tabcontent, newTabInfo.mFragmentContainer, newTabInfo.mTag);
                } else {
                    Log.d(LOG_TAG, "attaching existing FragmentContainer for " + newTabInfo.mClass.getSimpleName());
                    ft.attach(newTabInfo.mFragmentContainer);
                }
            }

            mCurrentTabInfo = newTabInfo;
            ft.commit();
            this.getSupportFragmentManager().executePendingTransactions();

        }

        if (newTabInfo != null) {
            newTabInfo.mFragmentContainer.setDefaultContent(null);
        } else {
            Log.e(LOG_TAG, "newTabInfo is null!");
        }

        refreshTabBackgrounds();
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


    public void listBluetoothDevices() {

        Log.i(LOG_TAG, "listBluetoothDevices is called!");

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Your device does not support bluetooth",
                    Toast.LENGTH_LONG).show();
        }
        else if (!mBluetoothAdapter.isEnabled()) {
            // This will cause this method to be rerun during onActivityResult execution
            registerForRerun(RERUN_LIST_BLUETOOTH_DEVICES);

            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT);
        }
        else {

            // This check is required because this method might be rerun from onActivityResult - trying
            // to execute any fragment transaction during onActivityResult results
            // in a state loss and IllegalStateException.
            // In case this method indeed rerun from onActivityResult, then the switching of the
            // fragment will be handled in onPostResume().
            if (mNextFragmentClass == null || !mNextFragmentClass.equals(BTDevicesListFragment.class)) {
                getFragment(BTDevicesListFragment.class);
            }

            if(mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
            }
            if (!mBluetoothAdapter.startDiscovery()) {
                Toast.makeText(this, "Discovery initiation encountered an error", Toast.LENGTH_LONG).show();
                return;
            }

            // Register the BroadcastReceiver
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mReceiver, filter);
        }
    }


    private void connectToDevice(BluetoothDevice device) {

        try {
            this.unregisterReceiver(mReceiver);
        } catch (IllegalArgumentException e ) {
            // Do nothing - the receiver wasn't registered
        }

        if (mBluetoothService == null) {
            setupBluetoothService();
        }
        mBluetoothService.connect(device, true);

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

        if (mBluetoothService == null) {
            setupBluetoothService();
        }

        // TODO: this method should not initiate devices discovery, but use a default device first
        if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            registerForRerun(RERUN_ON_SYNC_CLICK);
            listBluetoothDevices();
        } else {
            if (mSyncWithDeviceThread != null) {
                // Ensure that the existing thread is cancelled
                mSyncWithDeviceThread.cancel();
                try {
                    mSyncWithDeviceThread.join();
                } catch (InterruptedException e) {
                    // Currently there is no use case when the main thread is being interrupted,
                    // but just as a precaution...
                    Thread.currentThread().interrupt();
                }
            }

            mSyncWithDeviceThread = new SyncWithDeviceThread(this, mBluetoothService, mDBAdapter, mHandler);
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
    // Methods used for debug
    //
    // -------------------------------------------------------------------------------------------

    // TODO: replace all get...Fragment methods with a single generic method

    private Fragment getFragment(Class fragmentClass) {
        String tabTag = Constants.FRAGMENT_TO_TAB_MAP.get(fragmentClass);

        if (tabTag == null) {
            Log.e(LOG_TAG, "Fragment " + fragmentClass.getSimpleName() + " is not mapped to" +
                    " any tab in Constants.FRAGMENT_TO_TAB_MAP");
            return null;
        }
        mTabHost.setCurrentTabByTag(tabTag);

        FragmentContainer fc =
                (FragmentContainer) getSupportFragmentManager().findFragmentById(android.R.id.tabcontent);

        if (fc == null) {
            Log.e(LOG_TAG, "FragmentContainer for tab " + tabTag + " is null");
        }

        return fc.replaceContent(fragmentClass, null);

    }
//
//    private BTDevicesListFragment getDevicesListFragment() {
//
//        mTabHost.setCurrentTabByTag(Constants.HOME_TAB_TAG);
//
//        FragmentContainer fc =
//                (FragmentContainer) getSupportFragmentManager().findFragmentById(android.R.id.tabcontent);
//
//        if (fc == null) {
//            Log.e(LOG_TAG, "FragmentContainer for the current tab is null");
//        }
//        return (BTDevicesListFragment) fc.replaceContent(BTDevicesListFragment.class, null);
//    }
//
//
//    private BTCommDebugFragment getCommunicationFragment() {
//
//        mTabHost.setCurrentTabByTag("comm");
//
//        FragmentContainer fc =
//                (FragmentContainer) getSupportFragmentManager().findFragmentById(android.R.id.tabcontent);
//
//        if (fc == null) {
//            Log.e(LOG_TAG, "FragmentContainer for the current tab is null");
//        }
//        return (BTCommDebugFragment) fc.replaceContent(BTCommDebugFragment.class, null);
//
//    }
//
//
//    private HomeFragment getHomeFragment() {
//
//        mTabHost.setCurrentTabByTag(Constants.HOME_TAB_TAG);
//
//        FragmentContainer fc =
//                (FragmentContainer) getSupportFragmentManager().findFragmentById(android.R.id.tabcontent);
//
//        if (fc == null) {
//            Log.e(LOG_TAG, "FragmentContainer for the current tab is null");
//        }
//
//        return (HomeFragment) fc.replaceContent(HomeFragment.class, null);
//
//    }
//
//
//
//
//    private HistoryFragment getHistoryFragment() {
//
//        mTabHost.setCurrentTabByTag(Constants.HISTORY_TAB_TAG);
//
//        FragmentContainer fc =
//                (FragmentContainer) getSupportFragmentManager().findFragmentById(android.R.id.tabcontent);
//
//        if (fc == null) {
//            Log.e(LOG_TAG, "FragmentContainer for the current tab is null");
//        }
//
//        return (HistoryFragment) fc.replaceContent(HistoryFragment.class, null);
//
//    }

}
