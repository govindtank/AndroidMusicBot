package com.nicewuerfel.musicbot.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nicewuerfel.musicbot.R;
import com.nicewuerfel.musicbot.api.ApiConnector;
import com.nicewuerfel.musicbot.api.MusicApi;
import com.nicewuerfel.musicbot.api.Song;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link com.nicewuerfel.musicbot.ui.fragments.SongFragment.OnListFragmentInteractionListener } interface
 * to handle interaction events.
 * Use the {@link SearchFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SearchFragment extends Fragment {
  private static final String ARG_API = "music_api";

  private MusicApi api;

  @Nullable
  private Call<List<Song>> searchCall;
  @Nullable
  private String query = null;


  public SearchFragment() {
    // Required empty public constructor
  }

  /**
   * Use this factory method to create a new instance of
   * this fragment using the provided parameters.
   *
   * @param musicApi the music API to use
   * @return A new instance of fragment SearchFragment.
   */
  public static SearchFragment newInstance(MusicApi musicApi) {
    SearchFragment fragment = new SearchFragment();
    Bundle args = new Bundle();
    args.putParcelable(ARG_API, musicApi);
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (savedInstanceState != null) {
      api = savedInstanceState.getParcelable(ARG_API);
    } else if (getArguments() != null) {
      api = getArguments().getParcelable(ARG_API);
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    return inflater.inflate(R.layout.fragment_search, container, false);
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    if (!(context instanceof SongFragment.OnListFragmentInteractionListener)) {
      throw new RuntimeException(context.toString()
          + " must implement OnFragmentInteractionListener");
    }
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    outState.putParcelable(ARG_API, api);
    super.onSaveInstanceState(outState);
  }

  @Override
  public void onDetach() {
    super.onDetach();
    if (searchCall != null) {
      searchCall.cancel();
      searchCall = null;
    }
  }

  public MusicApi getApi() {
    return api;
  }

  public void search(final String query) {
    if (this.query != null && this.query.equals(query)) {
      return;
    }

    View view = getView();
    if (view != null) {
      getChildFragmentManager().beginTransaction()
          .replace(R.id.fragment_content, LoadingFragment.newInstance())
          .commit();
      getChildFragmentManager().executePendingTransactions();

      if (searchCall != null) {
        searchCall.cancel();
        searchCall = null;
      }

      Call<List<Song>> call;
      if (query.trim().isEmpty()) {
        if (api.isSongProvider()) {
          call = searchCall = ApiConnector.getService().getSuggestions(api.getName());
        } else {
          getChildFragmentManager().beginTransaction()
              .replace(R.id.fragment_content, new Fragment())
              .commit();
          return;
        }
      } else {
        call = searchCall = ApiConnector.getService().searchSong(api.getName(), query);
      }

      call.enqueue(new Callback<List<Song>>() {
        @Override
        public void onResponse(Call<List<Song>> call, Response<List<Song>> response) {
          if (response.isSuccessful()) {
            List<Song> body = response.body();
            ArrayList<Song> list = body == null ? new ArrayList<Song>() : new ArrayList<>(body);
            getChildFragmentManager().beginTransaction()
                .replace(R.id.fragment_content, SongFragment.newInstance(list, false, false))
                .commit();
            getChildFragmentManager().executePendingTransactions();
            SearchFragment.this.query = query;
          } else {
            showError();
          }
          searchCall = null;
        }

        @Override
        public void onFailure(Call<List<Song>> call, Throwable t) {
          if (!call.isCanceled()) {
            showError();
          }
          searchCall = null;
        }

        private void showError() {
          ConnectionErrorFragment fragment = ConnectionErrorFragment.newInstance();
          getChildFragmentManager().beginTransaction()
              .replace(R.id.fragment_content, fragment)
              .commit();
          getChildFragmentManager().executePendingTransactions();
          View errorView = fragment.getView();
          if (errorView != null) {
            errorView.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View view) {
                search(query);
              }
            });
          }
        }
      });
    }
  }
}
