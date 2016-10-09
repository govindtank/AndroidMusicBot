package com.nicewuerfel.musicbot;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.nicewuerfel.musicbot.api.ApiConnector;
import com.nicewuerfel.musicbot.api.BotState;
import com.nicewuerfel.musicbot.api.PlayerState;
import com.nicewuerfel.musicbot.api.Song;

import java.io.IOException;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Response;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PlayerControlFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PlayerControlFragment extends Fragment {

  private OnListFragmentInteractionListener mListener;
  private ScheduledExecutorService executor;
  private Observer playerStateObserver;

  private ImageButton pauseButton;
  private TextView songTitleText;
  private TextView songDescriptionText;
  private TextView songDurationText;
  private ScheduledFuture<?> refreshTask;

  public PlayerControlFragment() {
    // Required empty public constructor
  }

  /**
   * Use this factory method to create a new instance of
   * this fragment using the provided parameters.
   *
   * @return A new instance of fragment PlayerControlFragment.
   */
  public static PlayerControlFragment newInstance() {
    return new PlayerControlFragment();
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
    playerStateObserver = new Observer() {
      @Override
      public void update(Observable observable, Object data) {
        onPlayerStateUpdate((PlayerState) data);
      }
    };
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

    return view;
  }

  @Override
  public void onStart() {
    super.onStart();
    BotState.getInstance().addPlayerStateObserver(playerStateObserver);
    executor = Executors.newSingleThreadScheduledExecutor();
    onPlayerStateUpdate(BotState.getInstance().getPlayerState());
  }

  @Override
  public void onResume() {
    super.onResume();
    refreshTask = executor.scheduleWithFixedDelay(new Runnable() {
      @Override
      public void run() {
        try {
          Response<PlayerState> response = ApiConnector.getService().getPlayerState().execute();
          if (response.isSuccessful()) {
            PlayerState state = response.body();
            if (state != null) {
              BotState.getInstance().setPlayerState(state);
            }
          }
        } catch (IOException e) {
        }
      }
    }, 0L, 5L, TimeUnit.SECONDS);
  }

  @Override
  public void onPause() {
    if (refreshTask != null) {
      refreshTask.cancel(false);
    }
    super.onPause();
  }

  @Override
  public void onStop() {
    BotState.getInstance().deletePlayerStateObserver(playerStateObserver);
    refreshTask = null;
    executor.shutdownNow();
    executor = null;
    super.onStop();
  }

  @Override
  public void onDestroy() {
    playerStateObserver = null;
    super.onDestroy();
  }

  @Override
  public void onDestroyView() {
    pauseButton = null;
    songTitleText = null;
    songDescriptionText = null;
    songDurationText = null;
    super.onDestroyView();
  }

  @Override
  public void onDetach() {
    mListener = null;
    super.onDetach();
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

  public interface OnListFragmentInteractionListener {
  }

  private class PlayerControlCallback implements retrofit2.Callback<PlayerState> {
    @Override
    public void onResponse(Call<PlayerState> call, Response<PlayerState> response) {
      if (response.isSuccessful()) {
        PlayerState state = response.body();
        if (state != null) {
          BotState.getInstance().setPlayerState(state);
        }
      } else if (response.code() == 401) {
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().remove(PreferenceKey.TOKEN).apply();
        Intent loginIntent = new Intent(getContext(), LoginActivity.class);
        startActivity(loginIntent);
      }
    }

    @Override
    public void onFailure(Call<PlayerState> call, Throwable t) {
    }
  }
}
