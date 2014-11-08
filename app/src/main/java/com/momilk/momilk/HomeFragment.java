package com.momilk.momilk;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * Created by Vasiliy on 11/6/2014.
 */
public class HomeFragment extends Fragment {


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        Button newMeasurementBtn = (Button) view.findViewById(R.id.new_measurement_btn);

        newMeasurementBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Main mainActivity = (Main) getActivity();
                mainActivity.discoverDevices();
            }
        });

        Button syncBtn = (Button) view.findViewById(R.id.sync_btn);

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
