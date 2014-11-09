package com.momilk.momilk;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * Default fragment shown for a HISTORY tab
 */
public class HistoryFragment extends Fragment{



    HistoryFragmentCallback mCallback;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);


        Button showDayHistoryBtn = (Button) view.findViewById(R.id.show_day_history_btn);

        showDayHistoryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: add the appropriate logic here
            }
        });

        Button showWeekHistoryBtn = (Button) view.findViewById(R.id.show_week_history_btn);

        showWeekHistoryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: add the appropriate logic here
            }
        });

        Button showMonthHistoryBtn = (Button) view.findViewById(R.id.show_month_history_btn);

        showMonthHistoryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: add the appropriate logic here
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
            mCallback = (HistoryFragmentCallback) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement HistoryFragmentCallback");
        }
    }


    // Container Activity must implement this interface
    public interface HistoryFragmentCallback {
        public void showHistoryAsTable();
        public void showHistoryAsGraph();
    }

}
