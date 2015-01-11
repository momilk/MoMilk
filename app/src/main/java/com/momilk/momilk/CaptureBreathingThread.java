package com.momilk.momilk;


import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CaptureBreathingThread extends Thread {

    private static final String LOG_TAG = "CaptureBreathingThread";

    private static final String DATE_SYNC_MESSAGE_FORMAT = "'T@'HH'@'mm'@'ss'@'dd'@'MM'@'yyyy";

    private static final String START_SESSION_SYMBOL = "B";
    private static final String END_SESSION_SYMBOL = "B";

    private static final Pattern ACK_PATTERN = Pattern.compile("^Z@(\\d+)$");
    private static final Pattern DATA_PATTERN =
            Pattern.compile("^(\\d+)@(-?\\d+)@(-?\\d+)@(-?\\d+)$");

    private enum ThreadState {
        SYNC_DATE, START_BREATHING_SESSION, RECEIVE_PACKETS,
        WAIT_FOR_ACK, SESSION_SUCCESSFULL, SESSION_UNSUCCESSFULL, DONE
    }



    private Context mContext;
    private final Handler mHandler;

    private final BluetoothService mBluetoothService;
    // Making it synch because it will be accessed by this and other threads (at least Main)
    private ConcurrentLinkedQueue<String> mInputBuffer;

    private ThreadState mThreadState;
    private boolean mCancelled;
    private boolean mDone;

    private File mFile;
    private BufferedWriter mWriter;

    private int mNumOfPacketsWritten;
    private long mWaitForAckStarted;



    public CaptureBreathingThread(Context context, BluetoothService bluetoothService, Handler handler,
                                  String fileName) {
        mHandler = handler;
        mContext = context;

        mBluetoothService = bluetoothService;
        mInputBuffer = new ConcurrentLinkedQueue<String>();

        mThreadState = ThreadState.SYNC_DATE;
        mCancelled = false;
        mDone = false;

        mFile = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + "MoMilk",
                fileName);

        mNumOfPacketsWritten = 0;

    }

    @Override
    public void run() {

        String incomingMsg;

        while (true) {

            if (isCancelled()) {
                setThreadState(ThreadState.DONE);
            }

            incomingMsg = null;
            if (!mInputBuffer.isEmpty()) {
                incomingMsg = mInputBuffer.poll();
            }

            switch(getThreadState()) {

                case SYNC_DATE:

                    if (isDone()) {
                        showToastInActivity("Breathing aborted!");
                        setThreadState(ThreadState.DONE);
                        break;
                    }
                    else if (incomingMsg != null) {
                        Log.e(LOG_TAG, "Received message while in SYNC_DATE state");
                        cancel();
                    }
                    else {
                        sendMessage(composeDateSyncMessage());
                        setThreadState(ThreadState.START_BREATHING_SESSION);
                    }
                    break;

                case START_BREATHING_SESSION:

                    if (isDone()) {
                        showToastInActivity("Breathing aborted!");
                        setThreadState(ThreadState.DONE);
                        break;
                    }
                    else if (incomingMsg != null) {
                        Log.e(LOG_TAG, "Received message while in START_BREATHING_SESSION state: "
                                + incomingMsg );
                        cancel();
                    }
                    else {
                        openFileForWrite();
                        sendMessage(START_SESSION_SYMBOL);
                        setThreadState(ThreadState.RECEIVE_PACKETS);
                    }
                    break;

                case RECEIVE_PACKETS:

                    if (isDone()) {
                        sendMessage(END_SESSION_SYMBOL);
                        mWaitForAckStarted = System.currentTimeMillis();
                        setThreadState(ThreadState.WAIT_FOR_ACK);
                        break;
                    }
                    else if (incomingMsg != null) {
                        Matcher dataMatcher = DATA_PATTERN.matcher(incomingMsg);
                        if (dataMatcher.find()) {
                            parseAndWriteDataPacket(dataMatcher);
                        } else {
                            Log.e(LOG_TAG, "Received unrecognized packet while in RECEIVE_PACKETS " +
                                    "state" + incomingMsg);
                        }
                    }
                    break;

                case WAIT_FOR_ACK:
                    if (System.currentTimeMillis() > mWaitForAckStarted + 1000*Constants.BREATHING_WAIT_FOR_ACK_TIMEOUT_SEC) {
                        showToastInActivity("Breathing session aborted: timeout");
                        cancel();
                        break;
                    }
                    if (incomingMsg != null) {
                        // In this state data packets might still arrive and we need to accept them
                        Matcher dataMatcher = DATA_PATTERN.matcher(incomingMsg);
                        Matcher ackMatcher = ACK_PATTERN.matcher(incomingMsg);
                        if (dataMatcher.find()) {
                            parseAndWriteDataPacket(dataMatcher);
                        }
                        else if (ackMatcher.find()) {
                            if (Integer.valueOf(ackMatcher.group(1)) == mNumOfPacketsWritten ) {
                                Log.d(LOG_TAG, "all of " + mNumOfPacketsWritten +
                                        " packets were sucessfully written to the file");
                            } else {
                                Log.e(LOG_TAG, "only " + mNumOfPacketsWritten +
                                        " packets (out of " + ackMatcher.group(1) +")" +
                                        "were sucessfully written to the file");
                            }
                            setThreadState(ThreadState.DONE);
                        }
                        else {
                            Log.e(LOG_TAG, "Received unrecognized packet while in WAIT_FOR_ACK " +
                                    "state" + incomingMsg);
                        }
                    }
                    break;

                case DONE:
                    if (!Main.ENABLE_DEBUG) {
                        mBluetoothService.stop();
                    }

                    if (mWriter != null) {
                        try {
                            mWriter.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    if (mFile.exists()) {
                        if (isCancelled()) {
                            // Don't keep the file if the session was cancelled
                            mFile.delete();
                        } else {
                            MediaScannerConnection.scanFile(mContext,
                                    new String[] { mFile.getPath() }, null, null);
                        }
                    }
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
            // Send a failure message back to the Activity
            showToastInActivity("Bluetooth is disconnected");
            cancel();
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

    private void parseAndWriteDataPacket(Matcher matcher) {

        String deltaSec = matcher.group(1);
        String value = matcher.group(2);
        String deltaRoll = matcher.group(3);
        String deltaTilt = matcher.group(4);

        String msg = "Parsed:\n" + "DeltaSec: " + deltaSec + "\nValue: " + value +
                "\n\u0394Roll: " + deltaRoll + "\n\u0394Tilt: " + deltaTilt;
        Log.d(LOG_TAG, msg);

        if (writeLineToFile(deltaSec + " , " + value + " , " + deltaRoll + " , " + deltaTilt)) {
            mNumOfPacketsWritten ++;
        }

    }

    private void openFileForWrite() {
        try {
            mFile.getParentFile().mkdirs();
            if (!mFile.createNewFile()) {
                Log.e(LOG_TAG, "File " + mFile.getPath() + " already exists!");
                cancel();
                return;
            }
            mWriter = new BufferedWriter(new FileWriter(mFile));
        } catch (IOException e) {
            cancel();
            e.printStackTrace();
            Log.e(LOG_TAG, "Couldn't open " + mFile.getPath() + " for writing!");
        }
    }

    private boolean writeLineToFile(String line) {
        try {
            mWriter.newLine();
            mWriter.write(line);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String composeDateSyncMessage() {
        // Format the current date according to the decided format
        SimpleDateFormat fmt = new SimpleDateFormat(DATE_SYNC_MESSAGE_FORMAT);
        return fmt.format(new Date(System.currentTimeMillis()));
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

    public synchronized void done() {
        Log.d(LOG_TAG, "the session is done!");
        mDone = true;
    }

    private synchronized boolean isDone() {
        return mDone;
    }

    private synchronized void setThreadState(ThreadState state) {
        mThreadState = state;
    }

    private synchronized ThreadState getThreadState() {
        return mThreadState;
    }

    private void showToastInActivity(String message) {
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, message);
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }


}
