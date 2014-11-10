package com.momilk.momilk;

import android.app.Activity;
import android.content.Context;
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

import java.util.ArrayList;
import java.util.Collection;

/**
 * Default fragment shown for a HISTORY tab
 */
public class HistoryFragment extends Fragment{

    private static final String LOG_TAG = "HistoryFragment";

    private HistoryFragmentCallback mCallback;
    private HistoryArrayAdapter mAdapter;

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


        Button deleteTableBtn = (Button) view.findViewById(R.id.delete_table_btn);
        deleteTableBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteTable();
            }
        });


        Button deleteDBBtn = (Button) view.findViewById(R.id.delete_db_btn);
        deleteDBBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteDB();
            }
        });

        ListView historyList = (ListView) view.findViewById(R.id.history_list);
        mAdapter = new HistoryArrayAdapter(getActivity(), R.layout.history_item_row);

        historyList.setAdapter(mAdapter);

        showHistoryAsTable();

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


    public void showHistoryAsTable() {
        ArrayList<HistoryEntry> history = mCallback.getHistoryDatabaseAdapter().getHistory();
        if (history != null && history.size() > 0) {
            mAdapter.clear();
            mAdapter.addAll(history);
            mAdapter.notifyDataSetChanged();
        } else {
            Log.w(LOG_TAG, "history array is either empty or null");
        }
    }

    private void deleteTable() {
        mCallback.getHistoryDatabaseAdapter().deleteTable();
    }

    private void deleteDB() {
        mCallback.getHistoryDatabaseAdapter().deleteDB();
    }

    // Container Activity must implement this interface
    public interface HistoryFragmentCallback {
        public CustomDatabaseAdapter getHistoryDatabaseAdapter();
    }

    public static class HistoryEntry {

        private int mUid;
        private String mDate;
        private int mDuration;
        private int mAmount;

        public HistoryEntry(int uid, String date, int duration, int amount) {
            mUid = uid;
            mDate = date;
            mDuration = duration;
            mAmount = amount;
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

        public int getUid() {

            return mUid;
        }

        public String getDate() {
            return mDate;
        }

        public int getDuration() {
            return mDuration;
        }

        public int getAmount() {
            return mAmount;
        }
    }


    private class HistoryArrayAdapter extends ArrayAdapter<HistoryEntry> {

        private static final String LOG_TAG = "HistoryArrayAdapter";

        // TODO: check whether this list is necessary - maybe it is possible to reuse ArrayAdapters native data structure
        private ArrayList<HistoryEntry> mHistory;

        private class ViewHolder {
            TextView idTxt;
            TextView dateTxt;
            TextView durationTxt;
            TextView amountTxt;
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
