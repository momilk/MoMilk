package com.momilk.momilk;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * Default fragment shown for a HOME tab.
 */
public class HomeFragment extends Fragment {


    HomeFragmentCallback mCallback;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        Button newMeasurementBtn = (Button) view.findViewById(R.id.new_measurement_btn);

        newMeasurementBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCallback.startNewMeasurement();
            }
        });

        Button syncBtn = (Button) view.findViewById(R.id.sync_btn);

        syncBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCallback.syncWithDevice();
            }
        });


        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (HomeFragmentCallback) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement HomeFragmentCallback");
        }
    }


    // Container Activity must implement this interface
    public interface HomeFragmentCallback {
        public void startNewMeasurement();
        public void syncWithDevice();
    }

}
