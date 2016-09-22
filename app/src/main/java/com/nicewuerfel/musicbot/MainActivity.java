package com.nicewuerfel.musicbot;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.design.internal.NavigationMenu;
import android.support.v4.app.Fragment;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.nicewuerfel.musicbot.api.ApiConnector;
import com.nicewuerfel.musicbot.api.DummyCallback;
import com.nicewuerfel.musicbot.api.FinishableCallback;
import com.nicewuerfel.musicbot.api.MusicApi;
import com.nicewuerfel.musicbot.api.PlayerState;
import com.nicewuerfel.musicbot.api.Song;
import com.nicewuerfel.musicbot.api.SongContentProvider;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ConcurrentHashMap;

import io.github.yavski.fabspeeddial.FabSpeedDial;
import io.github.yavski.fabspeeddial.SimpleMenuListenerAdapter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements SearchSongFragment.OnListFragmentInteractionListener, QueueSongFragment.OnListFragmentInteractionListener, PlayerControlFragment.OnListFragmentInteractionListener {

  private static final String ARG_PLAYER_STATE = "PLAYER_STATE";

  private PlayerControlFragment playerControlFragment;
  private QueueSongFragment queueFragment;
  private PlayerState playerState;
  private List<MusicApi> apis;
  private Map<Integer, MusicApi> apiIds;

  private FinishableCallback refreshCallback;
  boolean stopped = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this).build();
    ImageLoader.getInstance().init(config);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    apis = Collections.emptyList();
    apiIds = new ConcurrentHashMap<>();
    refreshCallback = new DummyCallback();

    FabSpeedDial fabSpeedDial = (FabSpeedDial) findViewById(R.id.fab_speed_dial);
    if (fabSpeedDial != null) {
      fabSpeedDial.setMenuListener(new SimpleMenuListenerAdapter() {
        @Override
        public boolean onPrepareMenu(final NavigationMenu navigationMenu) {
          navigationMenu.clear();
          apiIds.clear();
          if (apis.isEmpty()) {
            SubMenu menu = navigationMenu.addSubMenu(getString(R.string.no_search_apis));
            menu.setIcon(android.R.drawable.stat_notify_error);
          } else {
            int i = 0;
            for (MusicApi api : apis) {
              MenuItem menu = navigationMenu.add(Menu.NONE, i++, Menu.NONE, getString(R.string.menu_item_search, api.getPrettyApiName()));
              menu.setIcon(android.R.drawable.ic_menu_search);
              int id = menu.getItemId();
              apiIds.put(id, api);
            }
          }
          return true;
        }

        @Override
        public boolean onMenuItemSelected(MenuItem menuItem) {
          int id = menuItem.getItemId();
          MusicApi api = apiIds.get(id);
          if (api != null) {
            showSearchBar(api);
          }
          return true;
        }
      });
    }


    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

    if (preferences.getString("bot_token", null) == null) {
      Intent intent = new Intent(this, LoginActivity.class);
      startActivity(intent);
    }

    try {
      ApiConnector.getService(preferences, getString(R.string.pref_default_server));
    } catch (IllegalArgumentException e) {
      Toast.makeText(this, getString(R.string.invalid_url), Toast.LENGTH_LONG).show();
      Intent intent = new Intent(this, SettingsActivity.class);
      startActivity(intent);
    }

    if (this.isSearching()) {
      setTitle(R.string.title_activity_search);
      ActionBar actionBar = getSupportActionBar();
      if (actionBar != null) {
        actionBar.setDisplayHomeAsUpEnabled(true);
      }

      if (fabSpeedDial != null) {
        fabSpeedDial.setVisibility(View.GONE);
      }

      Intent intent = getIntent();
      String query = intent.getStringExtra(SearchManager.QUERY);
      refreshSearchResults(query);
    } else {
      ActionBar actionBar = getSupportActionBar();
      if (actionBar != null) {
        actionBar.setDisplayHomeAsUpEnabled(false);
      }
      setTitle(R.string.title_activity_main);

      if (savedInstanceState != null) {
        playerState = savedInstanceState.getParcelable(ARG_PLAYER_STATE);
      }

      List<Song> songs = playerState == null ? new ArrayList<Song>(0) : playerState.getQueue();
      ArrayList<Song> songsArrayList;
      if (songs instanceof ArrayList) {
        songsArrayList = (ArrayList<Song>) songs;
      } else {
        songsArrayList = new ArrayList<>(songs);
      }

      queueFragment = QueueSongFragment.newInstance(songsArrayList);
      playerControlFragment = PlayerControlFragment.newInstance(playerState);

      getSupportFragmentManager().beginTransaction()
          .replace(R.id.activity_main_content, queueFragment)
          .replace(R.id.player_control, playerControlFragment)
          .commit();
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    playerState = null;
    playerControlFragment = null;
    queueFragment = null;
    refreshCallback = new DummyCallback();
  }

  @Override
  public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
    outState.putParcelable(ARG_PLAYER_STATE, playerState);
    super.onSaveInstanceState(outState, outPersistentState);
  }

  private boolean showSearchBar(MusicApi api) {
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    Menu menu = toolbar.getMenu();
    MenuItem item = menu.findItem(R.id.search_bar);
    item.setVisible(true);
    final SearchView searchView = (SearchView) item.getActionView();
    searchView.setQueryHint(getString(R.string.search_hint, api.getPrettyApiName()));
    searchView.requestFocus();
    SongContentProvider.setApi(api);
    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
    return false;
  }

  private void refreshSearchResults(String query) {
    refreshCallback = new SearchResultCallback();
    getSupportFragmentManager().beginTransaction()
        .replace(R.id.activity_main_content, LoadingFragment.newInstance())
        .commit();
    ApiConnector.getService().searchSong(SongContentProvider.getApi().getApiName(), query).enqueue(refreshCallback);
  }

  void refreshPlayerState() {
    refreshCallback = new GetPlayerStateCallback();
    ApiConnector.getService().getPlayerState().enqueue(refreshCallback);
    ApiConnector.getService().getMusicApis().enqueue(new GetMusicApisCallback());
    ApiConnector.updateHasAdmin(null);
  }

  @Override
  protected void onStart() {
    super.onStart();
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

    if (preferences.getString("bot_token", null) == null) {
      Intent intent = new Intent(this, LoginActivity.class);
      startActivity(intent);
      return;
    }

    stopped = false;
    if (!isSearching()) {
      refreshPlayerState();
    }
  }

  @Override
  protected void onStop() {
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    if (toolbar != null) {
      Menu menu = toolbar.getMenu();
      MenuItem item = menu.findItem(R.id.search_bar);
      if (item != null) {
        item.setVisible(false);
        final SearchView searchView = (SearchView) item.getActionView();
        searchView.setQuery("", false);
      }
    }
    stopped = true;
    super.onStop();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.action_bar_menu, menu);
    final MenuItem menuItem = menu.findItem(R.id.search_bar);
    final SearchView searchView = (SearchView) menuItem.getActionView();
    searchView.setIconifiedByDefault(false);

    SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
    // Tells your app's SearchView to use this activity's searchable configuration
    searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

    final CursorAdapter adapter = searchView.getSuggestionsAdapter();

    searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
      @Override
      public boolean onSuggestionSelect(int position) {
        return false;
      }

      @Override
      public boolean onSuggestionClick(int position) {
        Cursor cursor = adapter.getCursor();
        int id = cursor.getInt(cursor.getColumnIndex(BaseColumns._ID));
        Song song = SongContentProvider.songs.get(id);
        ApiConnector.getService().queue(song).enqueue(new QueueSongCallback());
        menuItem.setVisible(false);
        searchView.setQuery("", false);
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
        return true;
      }
    });

    searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
      @Override
      public void onFocusChange(View view, boolean queryTextFocused) {
        if (!queryTextFocused) {
          menuItem.setVisible(false);
          searchView.setQuery("", false);
        }
      }
    });

    final MenuItem claimAdminItem = menu.findItem(R.id.claim_admin_rights);
    final MenuItem adminPanelItem = menu.findItem(R.id.show_admin_panel);

    ApiConnector.ADMIN_STATE_OBSERVABLE.deleteObservers();
    ApiConnector.ADMIN_STATE_OBSERVABLE.addObserver(new Observer() {
      @Override
      public void update(Observable observable, Object data) {
        claimAdminItem.setVisible(!ApiConnector.hasAdmin());
        adminPanelItem.setVisible(ApiConnector.isAdmin());
      }
    });
    ApiConnector.ADMIN_STATE_OBSERVABLE.notifyObservers();

    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_settings:
        Intent settingsIntent = new Intent(this, SettingsActivity.class);
        startActivity(settingsIntent);
        return true;
      case android.R.id.home:
        Intent mainIntent = new Intent(this, MainActivity.class);
        startActivity(mainIntent);
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
        if (response.isSuccessful()) {
          PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit()
              .putString("bot_token", response.body())
              .apply();
          ApiConnector.getService();
          ApiConnector.updateIsAdmin(Boolean.TRUE);
          Toast.makeText(MainActivity.this, getString(R.string.claim_admin_success), Toast.LENGTH_SHORT).show();
        } else {
          Toast.makeText(MainActivity.this, getString(R.string.claim_admin_not_allowed) + " " + response.code(), Toast.LENGTH_SHORT).show();
          ApiConnector.updateHasAdmin(null);
        }
      }

      @Override
      public void onFailure(Call<String> call, Throwable t) {
        Toast.makeText(MainActivity.this, getString(R.string.connection_failed), Toast.LENGTH_SHORT).show();
      }
    });
  }

  private void onRefreshClick() {
    if (isRefreshing()) {
      return;
    }
    Intent intent = getIntent();
    if (this.isSearching()) {
      String query = intent.getStringExtra(SearchManager.QUERY);
      refreshSearchResults(query);
    } else {
      refreshPlayerState();
    }
  }

  public boolean isSearching() {
    return Intent.ACTION_SEARCH.equals(getIntent().getAction());
  }

  public boolean isStopped() {
    return stopped;
  }

  public boolean isRefreshing() {
    return !refreshCallback.isFinished();
  }

  void logout() {
    PreferenceManager.getDefaultSharedPreferences(this).edit().remove("bot_token").apply();
    Intent loginIntent = new Intent(this, LoginActivity.class);
    startActivity(loginIntent);
  }

  @Override
  public void onSearchResultClick(Song song) {
    ApiConnector.getService().queue(song).enqueue(new QueueSongCallback());
    onBackPressed();
  }

  @Override
  public void onRemoveSongClick(Song song) {
    ApiConnector.getService().dequeue(song).enqueue(new QueueSongCallback());
  }

  @Override
  public void onPlayerStateUpdate(final PlayerState state) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        playerState = state;
        List<Song> songs = state.getQueue();

        if (queueFragment != null) {
          queueFragment.updateQueue(songs);
        }
      }
    });
  }

  private class GetPlayerStateCallback extends FinishableCallback<PlayerState> {

    @Override
    public void onCallResponse(Call<PlayerState> call, Response<PlayerState> response) {
      if (isStopped()) {
        return;
      }
      if (response.isSuccessful()) {
        PlayerState state = response.body();

        if (state == null) {
          return;
        }

        if (playerControlFragment != null) {
          playerControlFragment.onPlayerStateUpdate(state);
        }
      } else if (response.code() == 401) {
        logout();
      }
    }

    @Override
    public void onCallFailure(Call<PlayerState> call, Throwable t) {
      if (isStopped()) {
        return;
      }
      Toast.makeText(MainActivity.this, getString(R.string.get_queue_failed), Toast.LENGTH_SHORT).show();
    }
  }

  private class GetMusicApisCallback implements Callback<List<MusicApi>> {
    @Override
    public void onResponse(Call<List<MusicApi>> call, Response<List<MusicApi>> response) {
      if (isStopped()) {
        return;
      }
      if (response.isSuccessful()) {
        List<MusicApi> state = response.body();

        if (state == null) {
          return;
        }

        apis = state;
      }
    }

    @Override
    public void onFailure(Call<List<MusicApi>> call, Throwable t) {
    }
  }

  class SearchResultCallback extends FinishableCallback<List<Song>> {

    @Override
    public void onCallResponse(Call<List<Song>> call, Response<List<Song>> response) {
      if (isStopped()) {
        return;
      }
      List<Song> songs = response.body();
      ArrayList<Song> songsArrayList;
      if (songs instanceof ArrayList) {
        songsArrayList = (ArrayList<Song>) songs;
      } else {
        songsArrayList = new ArrayList<>(songs);
      }
      Fragment fragment = SearchSongFragment.newInstance(songsArrayList);
      getSupportFragmentManager().beginTransaction()
          .replace(R.id.activity_main_content, fragment)
          .commit();
    }

    @Override
    public void onCallFailure(Call<List<Song>> call, Throwable t) {
      if (isStopped()) {
        return;
      }
      getSupportFragmentManager().beginTransaction()
          .replace(R.id.activity_main_content, ConnectionErrorFragment.newInstance())
          .commit();
    }
  }


  class QueueSongCallback implements Callback<String> {

    @Override
    public void onResponse(Call<String> call, Response<String> response) {
      if (isStopped()) {
        return;
      }
      if (response.isSuccessful()) {
        refreshPlayerState();
      } else if (response.code() == 401) {
        logout();
      }
    }


    @Override
    public void onFailure(Call<String> call, Throwable t) {
      if (isStopped()) {
        return;
      }
      Toast.makeText(MainActivity.this, getString(R.string.queue_failed), Toast.LENGTH_SHORT).show();
    }
  }
}
