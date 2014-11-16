package com.momilk.momilk;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Default fragment shown for a HISTORY tab
 */
public class HistoryFragment extends Fragment{

    private static final String LOG_TAG = "HistoryFragment";

    private HistoryFragmentCallback mCallback;
    private HistoryArrayAdapter mAdapter;
    private Button mShowDayHistoryBtn;
    private Button mShowWeekHistoryBtn;
    private Button mShowMonthHistoryBtn;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);


        mShowDayHistoryBtn = (Button) view.findViewById(R.id.show_day_history_btn);
        mShowDayHistoryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDayHistory();
            }
        });

        mShowWeekHistoryBtn = (Button) view.findViewById(R.id.show_week_history_btn);
        mShowWeekHistoryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showWeekHistory();
            }
        });

        mShowMonthHistoryBtn = (Button) view.findViewById(R.id.show_month_history_btn);
        mShowMonthHistoryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showMonthHistory();
            }
        });


        Button clearTableBtn = (Button) view.findViewById(R.id.clear_table_btn);
        clearTableBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearTable();
            }
        });

        ListView historyList = (ListView) view.findViewById(R.id.history_list);

//        TextView emptyText = (TextView)view.findViewById(android.R.id.empty);
//        historyList.setEmptyView(emptyText);

        mAdapter = new HistoryArrayAdapter(getActivity(), R.layout.history_item_row);
        historyList.setAdapter(mAdapter);

        showDayHistory();

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


    public void showDayHistory() {

        mShowDayHistoryBtn.setBackgroundResource(R.drawable.rectangle_button_pressed);
        mShowWeekHistoryBtn.setBackgroundResource(R.drawable.rectangle_button_unfocused);
        mShowMonthHistoryBtn.setBackgroundResource(R.drawable.rectangle_button_unfocused);

        ArrayList<HistoryEntry> history = mCallback.getHistoryDatabaseAdapter().getDayHistory();
        mAdapter.clear();
        mAdapter.addAll(history);
        mAdapter.notifyDataSetChanged();
    }


    public void showWeekHistory() {

        mShowDayHistoryBtn.setBackgroundResource(R.drawable.rectangle_button_unfocused);
        mShowWeekHistoryBtn.setBackgroundResource(R.drawable.rectangle_button_pressed);
        mShowMonthHistoryBtn.setBackgroundResource(R.drawable.rectangle_button_unfocused);

        ArrayList<HistoryEntry> history = mCallback.getHistoryDatabaseAdapter().getWeekHistory();
        mAdapter.clear();
        mAdapter.addAll(history);
        mAdapter.notifyDataSetChanged();
    }


    public void showMonthHistory() {

        mShowDayHistoryBtn.setBackgroundResource(R.drawable.rectangle_button_unfocused);
        mShowWeekHistoryBtn.setBackgroundResource(R.drawable.rectangle_button_unfocused);
        mShowMonthHistoryBtn.setBackgroundResource(R.drawable.rectangle_button_pressed);

        ArrayList<HistoryEntry> history = mCallback.getHistoryDatabaseAdapter().getMonthHistory();
        mAdapter.clear();
        mAdapter.addAll(history);
        mAdapter.notifyDataSetChanged();
    }

    private void clearTable() {
        mCallback.getHistoryDatabaseAdapter().clearTable();
        showDayHistory();
    }


    // Container Activity must implement this interface
    public interface HistoryFragmentCallback {
        public CustomDatabaseAdapter getHistoryDatabaseAdapter();
    }

    public static class HistoryEntry {

        private int mUid;
        private String mLeftOrRight;
        private String mDate;
        private int mDuration;
        private int mAmount;
        private int mDeltaRoll;
        private int mDeltaTilt;

        public HistoryEntry(int uid, String leftOrRight, String date, int duration, int amount,
                            int delta_roll, int delta_tilt) {
            mUid = uid;
            mLeftOrRight = leftOrRight;
            mDate = date;
            mDuration = duration;
            mAmount = amount;
            mDeltaRoll = delta_roll;
            mDeltaTilt = delta_tilt;
        }

        public void setDeltaTilt(int deltaTilt) {
            this.mDeltaTilt = deltaTilt;
        }

        public void setDeltaRoll(int deltaRoll) {
            this.mDeltaRoll = deltaRoll;
        }

        public int getDeltaRoll() {
            return mDeltaRoll;
        }

        public int getDeltaTilt() {
            return mDeltaTilt;
        }

        public void setUid(int mUid) {
            this.mUid = mUid;
        }

        public void setDate(String mDate) {
            this.mDate = mDate;
        }

        public void setDuration(int mDuration) {
            this.mDuration = mDuration;
        }

        public void setAmount(int mAmount) {
            this.mAmount = mAmount;
        }

        public int getUid() { return mUid; }

        public String getDate() {
            return mDate;
        }

        public int getDuration() {
            return mDuration;
        }

        public int getAmount() {
            return mAmount;
        }

        public String getLeftOrRight() { return mLeftOrRight; }

        public void setLeftOrRight(String leftOrRight) { mLeftOrRight = leftOrRight; }
    }


    private class HistoryArrayAdapter extends ArrayAdapter<HistoryEntry> {

        private static final String LOG_TAG = "HistoryArrayAdapter";

        // TODO: check whether this list is necessary - maybe it is possible to reuse ArrayAdapters native data structure
        private ArrayList<HistoryEntry> mHistory;

        private class ViewHolder {
            TextView idTxt;
            TextView leftOrRightTxt;
            TextView dateTxt;
            TextView durationTxt;
            TextView amountTxt;
            TextView deltaRollTxt;
            TextView deltaTiltTxt;
        }

        public HistoryArrayAdapter(Context context, int resource) {
            super(context, resource);
            mHistory = new ArrayList<HistoryEntry>();
        }

        private HistoryArrayAdapter(Context context, int resource,
                                    ArrayList<HistoryEntry> history) {
            super(context, resource);
            mHistory = history;
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder viewHolder;

            if (convertView == null) {
                LayoutInflater vi = (LayoutInflater)getActivity().getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                convertView = vi.inflate(R.layout.history_item_row, null);

                viewHolder = new ViewHolder();
                viewHolder.idTxt = (TextView) convertView.findViewById(R.id.id_txt);
                viewHolder.dateTxt = (TextView) convertView.findViewById(R.id.date_txt);
                viewHolder.durationTxt = (TextView) convertView.findViewById(R.id.duration_txt);
                viewHolder.amountTxt = (TextView) convertView.findViewById(R.id.amount_txt);
                viewHolder.deltaRollTxt = (TextView) convertView.findViewById(R.id.delta_roll_txt);
                viewHolder.deltaTiltTxt = (TextView) convertView.findViewById(R.id.delta_tilt_txt);

                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            HistoryEntry entry = mHistory.get(position);

            if (entry != null) {
                viewHolder.idTxt.setText(Integer.toString(entry.getUid()));
                viewHolder.dateTxt.setText("Date: " + entry.getDate());
                viewHolder.durationTxt.setText("Duration: " + Integer.toString(entry.getDuration()));
                viewHolder.amountTxt.setText("Amount: " + Integer.toString(entry.getAmount()));
                viewHolder.deltaRollTxt.setText("\u0394Roll: " + Integer.toString(entry.getDeltaRoll()));
                viewHolder.deltaTiltTxt.setText("\u0394Tilt: " + Integer.toString(entry.getDeltaTilt()));

                if(entry.getLeftOrRight().equals("R")) {
                    convertView.setBackgroundColor(getResources().getColor(R.color.bluedark));
                } else {
                    convertView.setBackgroundColor(getResources().getColor(R.color.greenlight));
                }
            } else {
                Log.e(LOG_TAG, "history entry is null!");
            }

            return convertView;
        }

        @Override
        public void add(HistoryEntry object) {
            super.add(object);
            mHistory.add(object);
        }

        @Override
        public void addAll(Collection<? extends HistoryEntry> collection) {
            super.addAll(collection);
            mHistory.addAll(collection);
        }

        @Override
        public void clear() {
            super.clear();
            mHistory.clear();
        }
    }

}
