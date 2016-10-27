package com.nicewuerfel.musicbot.ui.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.internal.NavigationMenu;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.nicewuerfel.musicbot.NotificationService;
import com.nicewuerfel.musicbot.PreferenceKey;
import com.nicewuerfel.musicbot.R;
import com.nicewuerfel.musicbot.api.ApiConnector;
import com.nicewuerfel.musicbot.api.BotState;
import com.nicewuerfel.musicbot.api.DummyCallback;
import com.nicewuerfel.musicbot.api.MusicApi;
import com.nicewuerfel.musicbot.api.PlayerState;
import com.nicewuerfel.musicbot.api.Song;
import com.nicewuerfel.musicbot.ui.fragments.PlayerControlFragment;
import com.nicewuerfel.musicbot.ui.fragments.SongFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import io.github.yavski.fabspeeddial.FabSpeedDial;
import io.github.yavski.fabspeeddial.SimpleMenuListenerAdapter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements SongFragment.OnListFragmentInteractionListener, PlayerControlFragment.OnListFragmentInteractionListener {

  private Observer queueObserver;
  private Observer adminObserver;
  private Observer menuObserver;

  private Call<PlayerState> refreshCall;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    FabSpeedDial fabSpeedDial = (FabSpeedDial) findViewById(R.id.fab_speed_dial);
    if (fabSpeedDial != null) {
      fabSpeedDial.setMenuListener(new SimpleMenuListenerAdapter() {
        @Override
        public boolean onPrepareMenu(final NavigationMenu navigationMenu) {
          navigationMenu.clear();
          List<MusicApi> apis = BotState.getInstance().getMusicApis();

          if (apis.isEmpty()) {
            MenuItem menu = navigationMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, getString(R.string.no_search_apis));
            menu.setIcon(android.R.drawable.stat_notify_error);
          } else {
            for (int i = 0; i < apis.size(); i++) {
              MenuItem menu = navigationMenu.add(Menu.NONE, i, Menu.NONE, getString(R.string.menu_item_search, apis.get(i).getPrettyName()));
              menu.setIcon(android.R.drawable.ic_menu_search);
            }
          }
          return true;
        }

        @Override
        public boolean onMenuItemSelected(MenuItem menuItem) {
          int id = menuItem.getItemId();
          List<MusicApi> apis = BotState.getInstance().getMusicApis();
          if (id < apis.size()) {
            MusicApi api = apis.get(id);
            showSearchBar(api);
          }
          return true;
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

    final SongFragment queueFragment = SongFragment.newInstance(new ArrayList<>(BotState.getInstance().getPlayerState().getQueue()));
    Fragment playerControlFragment = PlayerControlFragment.newInstance();

    getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.activity_content, queueFragment)
        .replace(R.id.player_control, playerControlFragment)
        .commit();

    getSupportFragmentManager().executePendingTransactions();

    adminObserver = new Observer() {
      @Override
      public void update(Observable observable, Object data) {
        queueFragment.setRemovable(true);
        queueFragment.setMovable(ApiConnector.isAdmin());
      }
    };
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
    adminObserver.update(null, null);
    // TODO actually observe isAdmin, not hasAdmin
    botState.addHasAdminObserver(adminObserver);
    queueObserver.update(null, BotState.getInstance().getPlayerState());
    botState.addPlayerStateObserver(queueObserver);
    if (menuObserver != null) {
      botState.addHasAdminObserver(menuObserver);
    }

    ApiConnector.getService().checkBotValidity().enqueue(new Callback<String>() {
      @Override
      public void onResponse(Call<String> call, Response<String> response) {
        if (!response.isSuccessful()) {
          Toast.makeText(MainActivity.this, "Wrong token, please log in again", Toast.LENGTH_SHORT).show();
          logout();
        }
      }

      @Override
      public void onFailure(Call<String> call, Throwable t) {
        Intent autoDetectIntent = new Intent(MainActivity.this, SettingsActivity.class);
        autoDetectIntent.putExtra(SettingsActivity.EXTRA_AUTO_DETECT, true);
        startActivity(autoDetectIntent);
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
    BotState botState = BotState.getInstance();
    botState.deletePlayerStateObserver(queueObserver);
    botState.deleteHasAdminObserver(adminObserver);
    botState.deleteHasAdminObserver(menuObserver);
    super.onStop();
  }

  @Override
  protected void onDestroy() {
    queueObserver = null;
    adminObserver = null;
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
    menuObserver.update(null, null);
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

  private void showSearchBar(MusicApi api) {
    Intent intent = new Intent(this, SearchActivity.class);
    intent.putExtra(SearchActivity.ARG_API, api);
    startActivity(intent);
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
    startActivity(loginIntent);
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
      if (!call.isCanceled()) {
        Toast.makeText(MainActivity.this, getString(R.string.get_queue_failed), Toast.LENGTH_SHORT).show();
      }
    }
  }
}
