package com.nicewuerfel.musicbot.ui.fragments;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nicewuerfel.musicbot.R;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ConnectionErrorFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ConnectionErrorFragment extends Fragment {

  public ConnectionErrorFragment() {
    // Required empty public constructor
  }

  /**
   * Use this factory method to create a new instance of
   * this fragment using the provided parameters.
   *
   * @return A new instance of fragment ConnectionErrorFragment.
   */
  @NonNull
  public static ConnectionErrorFragment newInstance() {
    return new ConnectionErrorFragment();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    return inflater.inflate(R.layout.fragment_connection_error, container, false);
  }

}
