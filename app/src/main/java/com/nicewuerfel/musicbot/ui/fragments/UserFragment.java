package com.nicewuerfel.musicbot.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nicewuerfel.musicbot.R;
import com.nicewuerfel.musicbot.api.ApiUser;

import java.util.ArrayList;

/**
 * A fragment representing a list of {@link ApiUser}.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class UserFragment extends Fragment {

  private static final String ARG_USER_LIST = "api-users";

  private ArrayList<ApiUser> users;
  private OnListFragmentInteractionListener mListener;

  /**
   * Mandatory empty constructor for the fragment manager to instantiate the
   * fragment (e.g. upon screen orientation changes).
   */
  public UserFragment() {
  }

  public static UserFragment newInstance(ArrayList<ApiUser> apiUsers) {
    UserFragment fragment = new UserFragment();
    Bundle args = new Bundle();
    args.putParcelableArrayList(ARG_USER_LIST, apiUsers);
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    if (context instanceof OnListFragmentInteractionListener) {
      mListener = (OnListFragmentInteractionListener) context;
    } else {
      throw new RuntimeException(context.toString()
          + " must implement OnListFragmentInteractionListener");
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (savedInstanceState != null) {
      users = savedInstanceState.getParcelableArrayList(ARG_USER_LIST);
    } else if (getArguments() != null) {
      users = getArguments().getParcelableArrayList(ARG_USER_LIST);
    } else {
      users = new ArrayList<>(0);
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_user_list, container, false);

    // Set the adapter
    if (view instanceof RecyclerView) {
      Context context = view.getContext();
      RecyclerView recyclerView = (RecyclerView) view;
      recyclerView.setLayoutManager(new LinearLayoutManager(context));
      recyclerView.setAdapter(new UserRecyclerViewAdapter(users, mListener));
    }
    return view;
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    outState.putParcelableArrayList(ARG_USER_LIST, users);
    super.onSaveInstanceState(outState);
  }

  @Override
  public void onDetach() {
    super.onDetach();
    mListener = null;
  }

  /**
   * This interface must be implemented by activities that contain this
   * fragment to allow an interaction in this fragment to be communicated
   * to the activity and potentially other fragments contained in that
   * activity.
   * <p/>
   * See the Android Training lesson <a href=
   * "http://developer.android.com/training/basics/fragments/communicating.html"
   * >Communicating with Other Fragments</a> for more information.
   */
  public interface OnListFragmentInteractionListener {
    void onEditClick(ApiUser user);
  }
}
