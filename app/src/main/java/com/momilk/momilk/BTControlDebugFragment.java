package com.momilk.momilk;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * This fragment is used in a debug configuration in order to control bluetooth
 */
public class BTControlDebugFragment extends Fragment {


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_debug_bt_control, container, false);

        Button newMeasurementBtn = (Button) view.findViewById(R.id.discover_devices_btn);

        newMeasurementBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Main mainActivity = (Main) getActivity();
                mainActivity.listBluetoothDevices();
            }
        });

        Button syncBtn = (Button) view.findViewById(R.id.listen_for_connection_btn);

        syncBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Main mainActivity = (Main) getActivity();
                mainActivity.startListening();
            }
        });


        return view;
    }



}
