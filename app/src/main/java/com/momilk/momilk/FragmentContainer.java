package com.momilk.momilk;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Field;


/**
 * Instances of this class are used as a containers for fragments inside app's tabs.
 *
 * The reason for using this intermediate class is that we need to switch multiple fragments
 * in a single tab. Since each tab is supposed to hold just a single fragment, this container will
 * be that fragment. The actual switching of fragments then happens here and not in app's tab.
 */
public class FragmentContainer extends Fragment {

    public static final String PARAM_CONTENT_FRAGMENT = "param_content_fragment";

    public static final String EXTRA_DEFAULT_FRAGMENT_NAME = "default_fragment";


    public static FragmentContainer newInstance(String class_name) {
        FragmentContainer container = new FragmentContainer();

        Bundle bundle = new Bundle(1);
        bundle.putString(EXTRA_DEFAULT_FRAGMENT_NAME, class_name);
        container.setArguments(bundle);

        return container;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_container, null);
    }

    @Override
    public void onDetach() {
        super.onDetach();

        try {
            Field childFragmentManager = Fragment.class.getDeclaredField("mChildFragmentManager");
            childFragmentManager.setAccessible(true);
            childFragmentManager.set(this, null);

        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }


    public Fragment replaceContent(Class<? extends Fragment> claz, Bundle args) {


        Fragment curFrag = getChildFragmentManager().findFragmentById(R.id.fragment_content);

        FragmentTransaction tx = getChildFragmentManager().beginTransaction();

        if (curFrag != null) {
            if (claz.isInstance(curFrag)) {
                // The currently shown fragment is the same as the new one - nothing to do
                Log.d("FragmentContainer", "the fragment " + claz.getSimpleName() + " is already shown");
                return curFrag;
            }

            if (!claz.getName().equals(getArguments().getString(EXTRA_DEFAULT_FRAGMENT_NAME))) {
                // Add fragment transaction to the back stack only if it is not the defualt
                // fragment which is being loaded
                tx.addToBackStack(curFrag.getClass().getSimpleName());
            } else {
                // Clear back stack if the default fragment is being loaded
                getChildFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }
        }


        // TODO: add these effects to any fragment change, not just the creation of a new fragment
        //tx.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);

        Log.d("FragmentContainer", "creating new  " + claz.getSimpleName());
        // Change to a new fragment
        try {
            Fragment newFragment = claz.newInstance();
            newFragment.setArguments(args);
            tx.replace(R.id.fragment_content, newFragment, claz.getClass().getSimpleName());
            tx.commit();
            getChildFragmentManager().executePendingTransactions();
            return newFragment;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    // Suppress "unchecked" warning. claz variable is checked for compatibility with Fragment class
    // inside the surrounding "if" statement
    @SuppressWarnings("unchecked")

    public void setDefaultContent(Bundle args) {

        Class<?> claz = null;

        try {
            claz = Class.forName(getArguments().getString(EXTRA_DEFAULT_FRAGMENT_NAME));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        // Ensure that claz extends Fragment
        if (claz != null && Fragment.class.isAssignableFrom(claz) ) {
            replaceContent((Class<? extends Fragment>) claz, args);
        } else {
            Log.e("FragmentContainer/setDefaultContent", "default class is either null or does" +
                    "not extend Fragment");
        }
    }


}