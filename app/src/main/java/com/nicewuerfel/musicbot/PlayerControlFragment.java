package com.nicewuerfel.musicbot;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.nicewuerfel.musicbot.api.ApiConnector;
import com.nicewuerfel.musicbot.api.PlayerState;
import com.nicewuerfel.musicbot.api.Song;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Response;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PlayerControlFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PlayerControlFragment extends Fragment {

  private static final String ARG_PLAYER_STATE = "PLAYER_STATE";

  private OnListFragmentInteractionListener mListener;
  private ScheduledExecutorService executor;

  private ImageButton pauseButton;
  private TextView songTitleText;
  private TextView songDescriptionText;
  private TextView songDurationText;

  public PlayerControlFragment() {
    // Required empty public constructor
  }

  /**
   * Use this factory method to create a new instance of
   * this fragment using the provided parameters.
   *
   * @return A new instance of fragment PlayerControlFragment.
   */
  public static PlayerControlFragment newInstance(@Nullable PlayerState state) {
    PlayerControlFragment fragment = new PlayerControlFragment();
    if (state != null) {
      Bundle args = new Bundle();
      args.putParcelable(ARG_PLAYER_STATE, state);
      fragment.setArguments(args);
    }
    return fragment;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getArguments() != null) {
      onPlayerStateUpdate(getArguments().<PlayerState>getParcelable(ARG_PLAYER_STATE));
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    View view = inflater.inflate(R.layout.fragment_player_control, container, false);

    pauseButton = (ImageButton) view.findViewById(R.id.pause_button);
    ImageButton nextButton = (ImageButton) view.findViewById(R.id.next_button);
    songTitleText = (TextView) view.findViewById(R.id.current_song_title);
    songDescriptionText = (TextView) view.findViewById(R.id.current_song_description);
    songDurationText = (TextView) view.findViewById(R.id.current_song_duration);
    songTitleText.setSelected(true);

    nextButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        ApiConnector.getService().nextSong().enqueue(new PlayerControlCallback());
      }
    });

    pauseButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        ApiConnector.getService().togglePause().enqueue(new PlayerControlCallback());
      }
    });

    songTitleText.setText("");
    songDescriptionText.setText("");
    songDurationText.setText("");

    executor.scheduleWithFixedDelay(new Runnable() {
      @Override
      public void run() {
        try {
          Response<PlayerState> response = ApiConnector.getService().getPlayerState().execute();
          if (response.isSuccessful()) {
            PlayerState state = response.body();
            if (state != null) {
              onPlayerStateUpdate(state);
            }
          }
        } catch (IOException e) {
        }
      }
    }, 0L, 5L, TimeUnit.SECONDS);

    return view;
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    if (context instanceof OnListFragmentInteractionListener) {
      mListener = (OnListFragmentInteractionListener) context;
      executor = Executors.newSingleThreadScheduledExecutor();
    } else {
      throw new RuntimeException(context.toString()
          + " must implement OnListFragmentInteractionListener");
    }
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    if (executor != null) {
      executor.shutdownNow();
    }
    pauseButton = null;
    songTitleText = null;
    songDescriptionText = null;
    songDurationText = null;
  }

  @Override
  public void onDetach() {
    super.onDetach();
    mListener = null;
    executor = null;
  }

  public void onPlayerStateUpdate(final PlayerState state) {
    if (state == null) {
      return;
    }

    final int drawableResource;
    if (state.isPaused()) {
      drawableResource = R.drawable.ic_action_playback_play;
    } else {
      drawableResource = R.drawable.ic_action_playback_pause;
    }

    if (pauseButton != null && songTitleText != null && songDescriptionText != null && songDurationText != null) {
      pauseButton.post(new Runnable() {
        @Override
        public void run() {
          if (pauseButton != null && songTitleText != null && songDescriptionText != null && songDurationText != null) {
            pauseButton.setImageResource(drawableResource);
            Song song = state.getCurrentSong();
            if (song != null) {
              if (!songTitleText.getText().equals(song.getTitle())) {
                songTitleText.setText(song.getTitle());
              }
              if (!songDescriptionText.getText().equals(song.getDescription())) {
                songDescriptionText.setText(song.getDescription());
              }
              songDurationText.setText(song.getDuration());
            }
          }
        }
      });
    }

    if (mListener != null) {
      mListener.onPlayerStateUpdate(state);
    }
  }

  public interface OnListFragmentInteractionListener {
    void onPlayerStateUpdate(PlayerState state);
  }

  private class PlayerControlCallback implements retrofit2.Callback<PlayerState> {

    @Override
    public void onResponse(Call<PlayerState> call, Response<PlayerState> response) {
      if (response.isSuccessful()) {
        PlayerState state = response.body();
        if (state == null) {
          return;
        }

        onPlayerStateUpdate(state);
      }
    }

    @Override
    public void onFailure(Call<PlayerState> call, Throwable t) {
      // TODO handle failure
    }
  }
}
