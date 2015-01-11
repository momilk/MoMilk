package com.momilk.momilk;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;


/**
 * Default fragment shown for an EXTRAS tab.
 *
 * Extras are additional app features which expand its functionality (in-app purchase)
 */
public class ExtrasFragment extends Fragment {



    ExtrasFragmentCallback mCallback;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_extras, container, false);

        ImageButton btnExtraLungs = (ImageButton) view.findViewById(R.id.btn_extra_lungs);
        btnExtraLungs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCallback.onExtraLungsClick();
            }
        });

        // TODO: add extras button listeners here
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (ExtrasFragmentCallback) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement ExtrasFragmentCallback");
        }
    }


    // Container Activity must implement this interface
    public interface ExtrasFragmentCallback {
        public void onExtraLungsClick();
    }

}
