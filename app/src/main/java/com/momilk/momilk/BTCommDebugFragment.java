package com.momilk.momilk;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

public class BTCommDebugFragment extends Fragment{


    private static final String LOG_TAG = "BTDevicesListFragment";

    private ArrayAdapter<String> mAdapter = null;
    private EditText mSendMsgEdt;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_debug_bt_comm, container, false);

        Button sendMsgBtn = (Button) view.findViewById(R.id.send_msg_btn);

        mSendMsgEdt = (EditText) view.findViewById(R.id.send_msg_edt);

        sendMsgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Main mainActivity = (Main) getActivity();
                mainActivity.sendMessage(mSendMsgEdt.getText().toString());
                mSendMsgEdt.setText("");
            }
        });


        mAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1);
        ListView receivedMsgList = (ListView) view.findViewById(R.id.received_msg_list);
        receivedMsgList.setAdapter(mAdapter);

        return view;
    }


    public void newMessage(String msg) {
        if (mAdapter == null ) {
            Log.e(LOG_TAG, "mAdapter is null!");
        }
        mAdapter.add(msg);
        mAdapter.notifyDataSetChanged();
    }


}
