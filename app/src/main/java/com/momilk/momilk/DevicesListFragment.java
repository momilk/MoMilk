package com.momilk.momilk;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;


public class DevicesListFragment extends ListFragment {

    private MyCustomAdapter mAdapter;
    private onBluetoothDeviceSelectedListener mCallback;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mAdapter = new MyCustomAdapter(getActivity(),
                android.R.layout.simple_list_item_1);

        setListAdapter(mAdapter);

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (onBluetoothDeviceSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement onBluetoothDeviceSelectedListener");
        }
    }

    public void add( BluetoothDevice device, BluetoothClass deviceClass) {

        Log.i("DevicesListFragment", "adding a new device:" + device.getName());

        mAdapter.add(device);
        mAdapter.notifyDataSetChanged();
    }

    public void clear() {
        mAdapter.clear();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        mCallback.deviceSelected(mAdapter.getItem(position));
    }


    public interface onBluetoothDeviceSelectedListener {
        public void deviceSelected(BluetoothDevice device);
    }


    private class MyCustomAdapter extends ArrayAdapter<BluetoothDevice> {

        private ArrayList<BluetoothDevice> mDevicesList;

        public MyCustomAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
            this.mDevicesList = new ArrayList<BluetoothDevice>();
        }
//
//        private class ViewHolder {
//            TextView code;
//            CheckBox name;
//        }


        @Override
        public void clear() {
            super.clear();
            mDevicesList.clear();
        }

        @Override
        public void add(BluetoothDevice device) {
            super.add(device);
            mDevicesList.add(device);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                LayoutInflater vi = (LayoutInflater)getActivity().getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                convertView = vi.inflate(android.R.layout.simple_list_item_1, null);
            }

            TextView textView = (TextView) convertView.findViewById(android.R.id.text1);
            textView.setText(mDevicesList.get(position).getName());

            return convertView;

        }

    }
}
