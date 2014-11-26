package com.momilk.momilk;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
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

    private static final int SHOW_LAST = 0;
    private static final int SHOW_DAY = 1;
    private static final int SHOW_WEEK = 2;
    private static final int SHOW_MONTH = 3;

    private HistoryFragmentCallback mCallback;
    private HistoryArrayAdapter mAdapter;
    private int mShownState;
    private Button mShowLastHistoryBtn;
    private Button mShowDayHistoryBtn;
    private Button mShowWeekHistoryBtn;
    private Button mShowMonthHistoryBtn;
    private ListView mHistoryListView;

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_NOTIFY_DB_CHANGED)) {
                Log.d(LOG_TAG, "Got a broadcast intent: " + intent.getAction());
                switch (mShownState) {
                    case SHOW_LAST:
                        showLastHistory();
                        break;
                    case SHOW_DAY:
                        showDayHistory();
                        break;
                    case SHOW_WEEK:
                        showWeekHistory();
                        break;
                    case SHOW_MONTH:
                        showMonthHistory();
                        break;
                    default:
                        showDayHistory();
                }
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);


        mShowLastHistoryBtn = (Button) view.findViewById(R.id.show_last_history_btn);
        mShowLastHistoryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLastHistory();
            }
        });

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

        mHistoryListView = (ListView) view.findViewById(R.id.history_list);

        TextView emptyText = (TextView)view.findViewById(android.R.id.empty);
        mHistoryListView.setEmptyView(emptyText);

        mAdapter = new HistoryArrayAdapter(getActivity(), R.layout.history_item_row);
        mHistoryListView.setAdapter(mAdapter);

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver,
                new IntentFilter(Constants.ACTION_NOTIFY_DB_CHANGED));

        showLastHistory();

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


    public void showLastHistory() {

        mShowLastHistoryBtn.setBackgroundResource(R.drawable.rectangle_button_pressed);
        mShowDayHistoryBtn.setBackgroundResource(R.drawable.rectangle_button_unfocused);
        mShowWeekHistoryBtn.setBackgroundResource(R.drawable.rectangle_button_unfocused);
        mShowMonthHistoryBtn.setBackgroundResource(R.drawable.rectangle_button_unfocused);

        ArrayList<HistoryEntry> history = mCallback.getHistoryDatabaseAdapter().getLastHistory();
        mAdapter.setShadowedBackground(true);
        mAdapter.clear();
        mAdapter.addAll(history);
        mAdapter.notifyDataSetChanged();

        mShownState = SHOW_LAST;
    }
    public void showDayHistory() {

        mShowLastHistoryBtn.setBackgroundResource(R.drawable.rectangle_button_unfocused);
        mShowDayHistoryBtn.setBackgroundResource(R.drawable.rectangle_button_pressed);
        mShowWeekHistoryBtn.setBackgroundResource(R.drawable.rectangle_button_unfocused);
        mShowMonthHistoryBtn.setBackgroundResource(R.drawable.rectangle_button_unfocused);

        ArrayList<HistoryEntry> history = mCallback.getHistoryDatabaseAdapter().getDayHistory();
        mAdapter.setShadowedBackground(false);
        mAdapter.clear();
        mAdapter.addAll(history);
        mAdapter.notifyDataSetChanged();

        mShownState = SHOW_DAY;
    }


    public void showWeekHistory() {

        mShowLastHistoryBtn.setBackgroundResource(R.drawable.rectangle_button_unfocused);
        mShowDayHistoryBtn.setBackgroundResource(R.drawable.rectangle_button_unfocused);
        mShowWeekHistoryBtn.setBackgroundResource(R.drawable.rectangle_button_pressed);
        mShowMonthHistoryBtn.setBackgroundResource(R.drawable.rectangle_button_unfocused);

        ArrayList<HistoryEntry> history = mCallback.getHistoryDatabaseAdapter().getWeekHistory();
        mAdapter.setShadowedBackground(false);
        mAdapter.clear();
        mAdapter.addAll(history);
        mAdapter.notifyDataSetChanged();

        mShownState = SHOW_WEEK;
    }


    public void showMonthHistory() {

        mShowLastHistoryBtn.setBackgroundResource(R.drawable.rectangle_button_unfocused);
        mShowDayHistoryBtn.setBackgroundResource(R.drawable.rectangle_button_unfocused);
        mShowWeekHistoryBtn.setBackgroundResource(R.drawable.rectangle_button_unfocused);
        mShowMonthHistoryBtn.setBackgroundResource(R.drawable.rectangle_button_pressed);

        ArrayList<HistoryEntry> history = mCallback.getHistoryDatabaseAdapter().getMonthHistory();
        mAdapter.setShadowedBackground(false);
        mAdapter.clear();
        mAdapter.addAll(history);
        mAdapter.notifyDataSetChanged();

        mShownState = SHOW_MONTH;
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
        private boolean mShadowedBackround = false;

        // TODO: check whether this list is necessary - maybe it is possible to reuse ArrayAdapters native data structure
        private ArrayList<HistoryEntry> mHistory;

        private class ViewHolder {
            TextView dateTxt;
            TextView leftOrRightTxt;
            TextView amountTxt;
            TextView durationTxt;
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
                viewHolder.dateTxt = (TextView) convertView.findViewById(R.id.date_txt);
                viewHolder.durationTxt = (TextView) convertView.findViewById(R.id.duration_txt);
                viewHolder.amountTxt = (TextView) convertView.findViewById(R.id.amount_txt);
                viewHolder.leftOrRightTxt = (TextView) convertView.findViewById(R.id.left_or_right_txt);

                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }


            LinearLayout historyItemLayout = (LinearLayout) convertView.findViewById(R.id.history_item_layout);
            View separator = historyItemLayout.findViewById(R.id.history_items_separator);
            // setBackground() for linear layout is available only since API 16, therefore
            // we use deprecated method here.
            if (mShadowedBackround) {
                separator.setVisibility(View.GONE);
                historyItemLayout.setBackgroundDrawable(getResources().getDrawable(R.drawable.shadowed_background));
                historyItemLayout.setPadding(20, 20, 20, 20);
            } else {
                if (position >= mHistory.size() - 1) {
                    // No need for separator for the last entry
                    separator.setVisibility(View.GONE);
                } else {
                    separator.setVisibility(View.VISIBLE);
                }
                historyItemLayout.setBackgroundDrawable(null);
                historyItemLayout.setPadding(5, 5, 10, 5);
            }


            HistoryEntry entry = mHistory.get(position);

            if (entry != null) {
                viewHolder.dateTxt.setText(entry.getDate());
                viewHolder.durationTxt.setText(Integer.toString(entry.getDuration()) + "min");
                viewHolder.amountTxt.setText(Integer.toString(entry.getAmount()) + "cc");

                GradientDrawable leftOrRightBackground =
                        (GradientDrawable) viewHolder.leftOrRightTxt.getBackground();
                if(entry.getLeftOrRight().equals("R")) {
                    leftOrRightBackground.setColor(getResources().getColor(R.color.bluedark));
                } else {
                    leftOrRightBackground.setColor(getResources().getColor(R.color.cyan));
                }
                viewHolder.leftOrRightTxt.setText(entry.getLeftOrRight());

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

        public void setShadowedBackground(boolean value) {
            mShadowedBackround = value;
        }
    }

}
