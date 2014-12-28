package com.momilk.momilk;

/**
 * Created by Vasiliy on 12/27/2014.
 */
public class DeviceInteractionStateMachine {

    public static enum INTERACTION_STATE {
        STATE_NONE, STATE_CONNECTING, STATE_CONNECTED,
        STATE_IDLE, STATE_SYNC, STATE_BREATHING, STATE_DISCONNECTED
    }

    private INTERACTION_STATE mState;

    public DeviceInteractionStateMachine() {
        mState = INTERACTION_STATE.STATE_IDLE;
    }

    public DeviceInteractionStateMachine(INTERACTION_STATE state) {
        mState = state;
    }

    public synchronized void setStatus(INTERACTION_STATE state) {
        mState = state;
    }

    public synchronized INTERACTION_STATE getState() {
        return mState;
    }


}
