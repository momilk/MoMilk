package com.momilk.momilk;

import android.app.Activity;
import android.preference.PreferenceCategory;
import android.support.v4.app.Fragment;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;


public class DevicesListFragment extends Fragment {


    private static final String LOG_TAG = "DevicesListFragment";

    private DevicesArrayAdapter mPairedDevicesAdapter;
    private DevicesArrayAdapter mDiscoveredDevicesAdapter;
    private DevicesListFragmentCallback mCallback;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_devices_list, container, false);

        // Initiate the paired devices list and add listener
        final ListView pairedDevicesList = (ListView) view.findViewById(R.id.paired_devices_list);
        mPairedDevicesAdapter = new DevicesArrayAdapter(getActivity());
        pairedDevicesList.setAdapter(mPairedDevicesAdapter);

        pairedDevicesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                        mCallback.deviceSelected(mPairedDevicesAdapter.getItem(position));
            }
        });


        // Initiate the discovered devices list and add listener
        final ListView discoveredDevicesList = (ListView) view.findViewById(R.id.discovered_devices_list);
        mDiscoveredDevicesAdapter = new DevicesArrayAdapter(getActivity());
        discoveredDevicesList.setAdapter(mDiscoveredDevicesAdapter);

        discoveredDevicesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                mCallback.deviceSelected(mDiscoveredDevicesAdapter.getItem(position));
            }
        });

        // The paired devices list can be populated from the begining
        mPairedDevicesAdapter.addAll(mCallback.getPairedDevices());
        mPairedDevicesAdapter.notifyDataSetChanged();


        return view;
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (DevicesListFragmentCallback) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement DevicesListFragmentCallback");
        }
    }

    public void add(BluetoothDevice device) {
        mDiscoveredDevicesAdapter.add(device);
        mDiscoveredDevicesAdapter.notifyDataSetChanged();
    }

    public void clear() {
        mDiscoveredDevicesAdapter.clear();
    }


    public interface DevicesListFragmentCallback {
        public void deviceSelected(BluetoothDevice device);
        public Set<BluetoothDevice> getPairedDevices();
    }


    private class DevicesArrayAdapter extends ArrayAdapter<BluetoothDevice> {

        private ArrayList<BluetoothDevice> mDevicesList;

        public DevicesArrayAdapter(Context context) {
            super(context, android.R.layout.simple_list_item_1);
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
        public void addAll(Collection<? extends BluetoothDevice> collection) {
            for (BluetoothDevice device : collection) {
                add(device);
            }
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
