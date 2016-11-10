package com.nicewuerfel.musicbot.ui.activities;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.nicewuerfel.musicbot.NotificationService;
import com.nicewuerfel.musicbot.PreferenceKey;
import com.nicewuerfel.musicbot.R;
import com.nicewuerfel.musicbot.api.ApiConnector;
import com.nicewuerfel.musicbot.api.ApiUser;
import com.nicewuerfel.musicbot.api.BotState;
import com.nicewuerfel.musicbot.api.DummyCallback;
import com.nicewuerfel.musicbot.api.MoveRequestBody;
import com.nicewuerfel.musicbot.api.PlayerState;
import com.nicewuerfel.musicbot.api.Song;
import com.nicewuerfel.musicbot.ui.fragments.PlayerControlFragment;
import com.nicewuerfel.musicbot.ui.fragments.SongFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import id.ridsatrio.optio.Optional;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements SongFragment.OnListFragmentInteractionListener, PlayerControlFragment.OnListFragmentInteractionListener {

  private Observer queueObserver;
  private Observer menuObserver;

  private Call<PlayerState> refreshCall;
  private Call<String> validityCall;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    View fab = findViewById(R.id.fab);
    if (fab != null) {
      fab.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          Intent intent = new Intent(MainActivity.this, SearchActivity.class);
          startActivity(intent);
        }
      });
    }

    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

    try {
      ApiConnector.getService(preferences, getString(R.string.pref_default_server_url));
    } catch (IllegalArgumentException e) {
      Toast.makeText(this, getString(R.string.invalid_url), Toast.LENGTH_LONG).show();
      Intent intent = new Intent(this, SettingsActivity.class);
      startActivity(intent);
    }

    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setDisplayHomeAsUpEnabled(false);
    }


    Optional<ApiUser> userFound = ApiConnector.getUser();
    boolean movable = userFound.isPresent() && userFound.get().hasPermission("mod");
    boolean removable = userFound.isPresent() && userFound.get().hasPermission("queue_remove");
    final SongFragment queueFragment = SongFragment.newInstance(new ArrayList<>(BotState.getInstance().getPlayerState().getQueue()), movable, removable);
    Fragment playerControlFragment = PlayerControlFragment.newInstance();

    getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.activity_content, queueFragment)
        .replace(R.id.player_control, playerControlFragment)
        .commit();

    getSupportFragmentManager().executePendingTransactions();

    queueObserver = new Observer() {
      @Override
      public void update(Observable observable, Object data) {
        PlayerState state = (PlayerState) data;
        queueFragment.updateQueue(state.getCombinedLastPlayedAndQueue());
      }
    };
  }

  @Override
  protected void onStart() {
    super.onStart();
    final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

    if (preferences.getString(PreferenceKey.TOKEN, null) == null) {
      Intent intent = new Intent(this, LoginActivity.class);
      startActivity(intent);
    }

    BotState botState = BotState.getInstance();
    botState.addPlayerStateObserver(queueObserver);
    if (menuObserver != null) {
      botState.addHasAdminObserver(menuObserver);
    }

    validityCall = ApiConnector.getService().checkBotValidity();
    validityCall.enqueue(new Callback<String>() {
      @Override
      public void onResponse(Call<String> call, Response<String> response) {
        if (!response.isSuccessful()) {
          Toast.makeText(MainActivity.this, "Wrong token, please log in again", Toast.LENGTH_SHORT).show();
          logout();
        }
      }

      @Override
      public void onFailure(Call<String> call, Throwable t) {
        if (!call.isCanceled()) {
          Intent autoDetectIntent = new Intent(MainActivity.this, SettingsActivity.class);
          autoDetectIntent.putExtra(SettingsActivity.EXTRA_AUTO_DETECT, true);
          startActivity(autoDetectIntent);
        }
      }
    });

    Intent serviceIntent = new Intent(this, NotificationService.class);
    serviceIntent.setAction(NotificationService.ACTION_START);
    startService(serviceIntent);
  }

  @Override
  protected void onResume() {
    super.onResume();
    refreshPlayerState(false);
  }

  @Override
  protected void onStop() {
    if (refreshCall != null) {
      refreshCall.cancel();
      refreshCall = null;
    }
    if (validityCall != null) {
      validityCall.cancel();
      validityCall = null;
    }
    BotState botState = BotState.getInstance();
    botState.deletePlayerStateObserver(queueObserver);
    botState.deleteHasAdminObserver(menuObserver);
    super.onStop();
  }

  @Override
  protected void onDestroy() {
    queueObserver = null;
    menuObserver = null;
    super.onDestroy();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.main_action_bar_menu, menu);
    final MenuItem claimAdminItem = menu.findItem(R.id.claim_admin_rights);
    menuObserver = new Observer() {
      @Override
      public void update(Observable observable, Object data) {
        claimAdminItem.setVisible(!BotState.getInstance().hasAdmin());
      }
    };
    BotState.getInstance().addHasAdminObserver(menuObserver);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_settings:
        Intent settingsIntent = new Intent(this, SettingsActivity.class);
        startActivity(settingsIntent);
        return true;
      case R.id.refresh_button:
        onRefreshClick();
        return true;
      case R.id.action_logout:
        logout();
        return true;
      case R.id.claim_admin_rights:
        claimAdminRights();
        return true;
      case R.id.show_admin_panel:
        Intent adminIntent = new Intent(this, AdminActivity.class);
        startActivity(adminIntent);
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  private void claimAdminRights() {
    ApiConnector.getService().claimAdmin().enqueue(new Callback<String>() {
      @Override
      public void onResponse(Call<String> call, Response<String> response) {
        BotState.getInstance().hasAdmin(true);
        if (response.isSuccessful()) {
          PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit()
              .putString(PreferenceKey.TOKEN, response.body())
              .apply();
          Toast.makeText(MainActivity.this, getString(R.string.claim_admin_success), Toast.LENGTH_SHORT).show();
          finish();
          startActivity(getIntent());
        } else {
          Toast.makeText(MainActivity.this, getString(R.string.claim_admin_not_allowed) + " " + response.code(), Toast.LENGTH_SHORT).show();
        }
      }

      @Override
      public void onFailure(Call<String> call, Throwable t) {
        Toast.makeText(MainActivity.this, getString(R.string.connection_failed), Toast.LENGTH_SHORT).show();
      }
    });
  }

  private void refreshPlayerState(boolean showToast) {
    if (refreshCall != null) {
      refreshCall.cancel();
    }
    refreshCall = ApiConnector.getService().getPlayerState();
    refreshCall.enqueue(new GetPlayerStateCallback(showToast));
    ApiConnector.getService().getMusicApis().enqueue(new BotState.MusicApisCallback());
    ApiConnector.getService().hasAdmin().enqueue(new BotState.HasAdminCallback());
  }

  private void onRefreshClick() {
    if (isRefreshing()) {
      return;
    }
    refreshPlayerState(true);
  }

  public boolean isRefreshing() {
    return refreshCall != null;
  }

  void logout() {
    PreferenceManager.getDefaultSharedPreferences(this).edit().remove(PreferenceKey.TOKEN).apply();
    Intent loginIntent = new Intent(this, LoginActivity.class);
    startActivityForResult(loginIntent, 42);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == 42) {
      if (resultCode == Activity.RESULT_OK) {
        finish();
        startActivity(getIntent());
      }
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override
  public void onSongRemoveClick(Song song) {
    ApiConnector.getService().dequeue(song).enqueue(new DummyCallback<String>() {
      @Override
      public void onResponse(Call<String> call, Response<String> response) {
        refreshPlayerState(false);
      }
    });
  }

  @Override
  public void onSongClick(final Song song) {
    if (song.isLastPlayed()) {
      new AlertDialog.Builder(this)
          .setCancelable(true)
          .setTitle(getString(R.string.queue_last_played, song.getTitle()))
          .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              ApiConnector.getService().enqueue(song).enqueue(new DummyCallback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                  if (response.isSuccessful()) {
                    refreshPlayerState(false);
                  }
                }
              });
            }
          })
          .setNegativeButton(android.R.string.no, null)
          .show();
    } else {
      Optional<ApiUser> user = ApiConnector.getUser();
      if (user.isPresent() && user.get().hasPermission("mod")) {
        List<Song> queue = BotState.getInstance().getPlayerState().getQueue();
        if (queue.size() < 2 || queue.get(0).equals(song)) return;
        new AlertDialog.Builder(this)
            .setCancelable(true)
            .setTitle(getString(R.string.queue_top, song.getTitle()))
            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialogInterface, int i) {
                List<Song> queue = BotState.getInstance().getPlayerState().getQueue();
                if (queue.size() > 1) {
                  Song firstSong = queue.get(0);
                  ApiConnector.getService().moveSong(new MoveRequestBody(song, firstSong), null).enqueue(new DummyCallback<PlayerState>());
                }
              }
            })
            .setNegativeButton(android.R.string.no, null)
            .show();
      }
    }
  }

  private class GetPlayerStateCallback implements Callback<PlayerState> {

    private final boolean showToast;

    public GetPlayerStateCallback(boolean showToast) {
      this.showToast = showToast;
    }

    @Override
    public void onResponse(Call<PlayerState> call, Response<PlayerState> response) {
      refreshCall = null;
      if (response.isSuccessful()) {
        PlayerState state = response.body();

        if (state != null) {
          BotState.getInstance().setPlayerState(state);
        }

        if (showToast)
          Toast.makeText(MainActivity.this, R.string.refresh_success, Toast.LENGTH_SHORT).show();
      } else if (response.code() == 401) {
        logout();
      } else {
        if (showToast)
          Toast.makeText(MainActivity.this, R.string.unsuccessful_refresh, Toast.LENGTH_SHORT).show();
      }
    }

    @Override
    public void onFailure(Call<PlayerState> call, Throwable t) {
      refreshCall = null;
    }
  }
}
