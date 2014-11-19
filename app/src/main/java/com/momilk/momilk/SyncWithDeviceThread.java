package com.momilk.momilk;


import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;
import android.os.Handler;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SyncWithDeviceThread extends Thread {

    private static final String LOG_TAG = "SyncWithDeviceThread";

    private static final String DATE_SYNC_MESSAGE_FORMAT = "'T@'HH'@'mm'@'ss'@'dd'@'MM'@'yyyy";

    private static final String START_SESSION_SYMBOL = "S";
    private static final String ACK_SYMBOL = "R";

    private static final Pattern END_SESSION_PATTERN = Pattern.compile("^Z@(\\d+)$");
    private static final Pattern DATA_PATTERN =
            Pattern.compile("^W@(\\d+)@(L|R)@(\\d+)@(\\d+)@(\\d+)@(\\d+)@(\\d+)@(\\d+)@(\\d+)@(\\d+)@(\\d+)@(\\d+)$");

    private enum SyncState {
        IDLE, WAIT_FOR_SESSION_START, WAIT_FOR_SESSION_END,
        SESSION_SUCCESSFULL, SESSION_UNSUCCESSFULL, DONE
    }


    private final Context mContext;
    private final BluetoothService mBluetoothService;
    // Making it synch because it will be accessed by this and other threads (at least Main)
    private ConcurrentLinkedQueue<String> mInputBuffer;
    private ArrayList<DataPacket> mDataPackets;
    private final CustomDatabaseAdapter mDBAdapter;
    private final Handler mHandler;

    private SyncState mSyncState;
    private boolean mCancelled;

    public SyncWithDeviceThread(Context context, BluetoothService bluetoothService,
                                CustomDatabaseAdapter dbAdapter, Handler handler) {
        mContext = context;
        mBluetoothService = bluetoothService;
        mInputBuffer = new ConcurrentLinkedQueue<String>();
        mDataPackets = new ArrayList<DataPacket>();
        mDBAdapter = dbAdapter;
        mHandler = handler;
        mSyncState = SyncState.IDLE;
        mCancelled = false;

    }

    @Override
    public void run() {

        while (!isCancelled()) {
            switch(getSyncState()) {

                case IDLE:
                    if (sendMessage(composeDateSyncMessage())) {
                        setSyncState(SyncState.WAIT_FOR_SESSION_START);
                    } else {
                        setSyncState(SyncState.DONE);
                    }
                    break;

                case WAIT_FOR_SESSION_START:
                    if (!mInputBuffer.isEmpty()) {
                        String message = mInputBuffer.poll();
                        if (message.equals(START_SESSION_SYMBOL)) {
                            setSyncState(SyncState.WAIT_FOR_SESSION_END);
                        } else {
                            Log.e(LOG_TAG, "While waiting for 'Session Start' packet, got " +
                                    "unexpected packet: " + message);
                        }
                    }
                    break;

                case WAIT_FOR_SESSION_END:
                    if (!mInputBuffer.isEmpty()) {
                        String message = mInputBuffer.poll();
                        Matcher endSessionMatcher = END_SESSION_PATTERN.matcher(message);
                        if (endSessionMatcher.find()) {
                            sendMessage(composeAckMessage());
                            if (Integer.parseInt(endSessionMatcher.group(1)) == mDataPackets.size()) {
                                setSyncState(SyncState.SESSION_SUCCESSFULL);
                            } else {
                                setSyncState(SyncState.SESSION_UNSUCCESSFULL);
                            }
                            break;
                        }

                        Matcher dataMatcher = DATA_PATTERN.matcher(message);
                        if (dataMatcher.find()) {
                            parseDataPacket(dataMatcher);
                            break;
                        }

                    }
                    break;

                case SESSION_SUCCESSFULL:
                    storeReceivedPackets();
                    return;
                case SESSION_UNSUCCESSFULL:
                    // The packets will be re-transmitted
                    discardReceivedPackets();
                    setSyncState(SyncState.WAIT_FOR_SESSION_START);
                    break;
                case DONE:
                    return;
            }


        }

    }


    private boolean sendMessage(String message) {
        if (mBluetoothService == null) {
            Log.e(LOG_TAG, "Can't send a message: BluetoothService is null!");
            return false;
        }
        // Check that we're actually connected before trying anything
        if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(mContext, "Sync failed: not connected", Toast.LENGTH_LONG).show();
            setSyncState(SyncState.DONE);
            return false;
        }
        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothService to write
            // Adding newline char in order to be able to parse messages as lines
            mBluetoothService.write(message + "\n");
            return true;
        } else {
            Log.e(LOG_TAG, "Message's length is zero!");
            return false;
        }
    }

    private void parseDataPacket(Matcher matcher) {

        try {
            // The dots were added in order for the formatting to be able to handle single letter
            // values - when the string contains no delimeters, it is impossible to know which
            // digit belongs to which field when the length of the date string does not match
            // the length of the pattern exactly
            SimpleDateFormat fmt = new SimpleDateFormat("HH.mm.ss.dd.MM.yyyy");
            Date date = fmt.parse(matcher.group(3) + "." + matcher.group(4) + "." +
                    matcher.group(5) + "." + matcher.group(6) + "." +
                    matcher.group(7) + "." + matcher.group(8));

            fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            String formattedDate = fmt.format(date);

            String index = matcher.group(1);
            String leftOrRight = matcher.group(2);
            String duration = matcher.group(9);
            String amount = matcher.group(10);
            String deltaRoll = matcher.group(10);
            String deltaTilt = matcher.group(11);



            // Show toast in the activity
            Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString(Constants.TOAST,
                    "Date: " + formattedDate + "\nIndex: " + index +"\nL/R: " +
                            leftOrRight + "\nDuration: " + duration + "\nAmount: " + amount +
                            "\n\u0394Roll: " + deltaRoll + "\n\u0394Tilt: " + deltaTilt);
            msg.setData(bundle);
            mHandler.sendMessage(msg);

            DataPacket packet = new DataPacket();
            packet.mIndex = Integer.valueOf(index);
            packet.mLeftOrRight = leftOrRight;
            packet.mDate = formattedDate;
            packet.mDuration = Integer.valueOf(duration);
            packet.mAmount = Integer.valueOf(amount);
            packet.mDeltaRoll = Integer.valueOf(deltaRoll);
            packet.mDeltaTilt = Integer.valueOf(deltaTilt);

            mDataPackets.add(packet);

        } catch(ParseException e) {
            Log.e(LOG_TAG, "Got a matcher.group(0) of unknown format: " + matcher.group(0));
        } catch (IndexOutOfBoundsException e) {
            Log.e(LOG_TAG, "Got a matcher.group(0) of unknown format: " + matcher.group(0));
        } catch (NullPointerException e) {
            Log.e(LOG_TAG, "Got a matcher.group(0) of unknown format: " + matcher.group(0));
        }

    }


    private void storeReceivedPackets() {
        for (DataPacket packet : mDataPackets) {
            if (mDBAdapter.insertData(packet.mIndex, packet.mLeftOrRight, packet.mDate,
                    packet.mDuration, packet.mAmount, packet.mDeltaRoll, packet.mDeltaTilt) < 0) {
                Log.e(LOG_TAG, "insertData failed!");
            }
        }
    }

    private void discardReceivedPackets() {
        mDataPackets.clear();
    }

    private String composeDateSyncMessage() {
        // Format the current date according to the decided format
        SimpleDateFormat fmt = new SimpleDateFormat(DATE_SYNC_MESSAGE_FORMAT);
        return fmt.format(new Date(System.currentTimeMillis()));
    }

    private String composeAckMessage() {
        return ACK_SYMBOL + "@" + Integer.toString(mDataPackets.size()) + "\n";
    }

    public void newIncomingMessage(String message) {
        mInputBuffer.add(message);
    }


    public synchronized void cancel() {
        mCancelled = true;
    }

    private synchronized boolean isCancelled() {
        return mCancelled;
    }

    private synchronized void setSyncState(SyncState state) {
        mSyncState = state;
    }

    private synchronized SyncState getSyncState() {
        return mSyncState;
    }

    private class DataPacket {
        private int mIndex;
        private String mLeftOrRight;
        private String mDate;
        private int mDuration;
        private int mAmount;
        private int mDeltaRoll;
        private int mDeltaTilt;
    }
}
