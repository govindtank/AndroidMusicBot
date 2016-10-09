package com.nicewuerfel.musicbot;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.nicewuerfel.musicbot.api.ApiConnector;
import com.nicewuerfel.musicbot.api.DummyCallback;
import com.nicewuerfel.musicbot.api.MusicApi;
import com.nicewuerfel.musicbot.api.Song;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchActivity extends AppCompatActivity implements SongFragment.OnListFragmentInteractionListener {

  public static final String ARG_API = "API";
  private static final String ARG_QUERY = "QUERY";

  private MusicApi api;
  private String query = "";

  @Nullable
  private Call<List<Song>> searchCall;
  private SongFragment songFragment;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_search);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
    try {
      ApiConnector.getService(preferences, getString(R.string.pref_default_server));
    } catch (IllegalArgumentException e) {
      Toast.makeText(this, getString(R.string.invalid_url), Toast.LENGTH_LONG).show();
      Intent intent = new Intent(this, SettingsActivity.class);
      startActivity(intent);
    }

    setTitle(R.string.title_activity_search);
    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setDisplayHomeAsUpEnabled(true);
    }

    songFragment = SongFragment.newInstance();
    getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.activity_content, songFragment)
        .commit();
    getSupportFragmentManager().executePendingTransactions();
    songFragment.setMovable(false);
    songFragment.setRemovable(false);
  }

  @Override
  protected void onStart() {
    super.onStart();
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
    if (preferences.getString(PreferenceKey.TOKEN, null) == null) {
      Intent intent = new Intent(this, LoginActivity.class);
      startActivity(intent);
    }

    api = getIntent().getParcelableExtra(ARG_API);
    if (api == null) {
      Logger.getAnonymousLogger().warning("Missing API extra in search activity. Finishing..."); // TODO use real logger
      finish();
    }
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    query = savedInstanceState.getString(ARG_QUERY);
    if (query == null) {
      query = "";
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (query.isEmpty()) {
      InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
      imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
    }
    refreshSearchResults();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    if (!isFinishing()) {
      outState.putString(ARG_QUERY, query);
    }
    super.onSaveInstanceState(outState);
  }

  @Override
  protected void onStop() {
    if (searchCall != null) {
      searchCall.cancel();
      searchCall = null;
    }
    super.onStop();
  }

  @Override
  protected void onDestroy() {
    searchCall = null;
    songFragment = null;
    api = null;
    query = "";
    super.onDestroy();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.search_action_bar_menu, menu);
    final MenuItem menuItem = menu.findItem(R.id.search_bar);
    SearchView searchView = (SearchView) menuItem.getActionView();
    searchView.setIconifiedByDefault(false);
    searchView.requestFocus();
    searchView.setQueryHint(getString(R.string.search_hint, api.getPrettyName()));
    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
      @Override
      public boolean onQueryTextSubmit(String query) {
        return true;
      }

      @Override
      public boolean onQueryTextChange(String newText) {
        query = newText;
        refreshSearchResults();
        return true;
      }
    });
    return true;
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    final MenuItem menuItem = menu.findItem(R.id.search_bar);
    SearchView searchView = (SearchView) menuItem.getActionView();
    searchView.setQuery(query, false);
    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_settings:
        Intent settingsIntent = new Intent(this, SettingsActivity.class);
        startActivity(settingsIntent);
        return true;
      case android.R.id.home:
        finish();
        return true;
      case R.id.refresh_button:
        onRefreshClick();
        return true;
      case R.id.action_logout:
        logout();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  private void onRefreshClick() {
    if (isRefreshing()) {
      return;
    }
    refreshSearchResults();
  }

  private void refreshSearchResults() {
    if (searchCall != null) {
      searchCall.cancel();
      searchCall = null;
    }
    getSupportFragmentManager().beginTransaction()
        .replace(R.id.activity_content, LoadingFragment.newInstance())
        .commit();
    getSupportFragmentManager().executePendingTransactions();
    String query = this.query;
    if (query.isEmpty()) {
      if (api.isSongProvider()) {
        searchCall = ApiConnector.getService().getSuggestions(api.getName());
      } else {
        onSongsUpdate(Collections.<Song>emptyList());
        return;
      }
    } else {
      searchCall = ApiConnector.getService().searchSong(api.getName(), query);
    }
    if (searchCall != null) {
      searchCall.enqueue(new SearchResultCallback(query));
    }
  }

  public boolean isRefreshing() {
    return searchCall != null;
  }

  void logout() {
    PreferenceManager.getDefaultSharedPreferences(this).edit().remove(PreferenceKey.TOKEN).apply();
    Intent loginIntent = new Intent(this, LoginActivity.class);
    finish(); // Don't return to this activity after login
    startActivity(loginIntent);
  }

  @Override
  public void onSongClick(Song song) {
    ApiConnector.getService().queue(song).enqueue(new DummyCallback<String>());
    finish();
  }

  @Override
  public void onSongRemoveClick(Song song) {
    // Not possible
  }

  private void onSongsUpdate(@NonNull List<Song> songs) {
    getSupportFragmentManager().beginTransaction()
        .replace(R.id.activity_content, songFragment)
        .commit();
    getSupportFragmentManager().executePendingTransactions();
    songFragment.updateQueue(songs);
  }

  private class SearchResultCallback implements Callback<List<Song>> {

    private final String query;

    private SearchResultCallback(String query) {
      this.query = query;
    }

    @Override
    public void onResponse(Call<List<Song>> call, Response<List<Song>> response) {
      List<Song> songs = response.body();
      if (songs == null) {
        songs = Collections.emptyList();
      }
      if (SearchActivity.this.query.equals(query)) {
        onSongsUpdate(songs);
      }
      searchCall = null;
    }

    @Override
    public void onFailure(Call<List<Song>> call, Throwable t) {
      if (!call.isCanceled()) {
        ConnectionErrorFragment errorFragment = ConnectionErrorFragment.newInstance();
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.activity_content, errorFragment)
            .commit();
        View view = errorFragment.getView();
        if (view != null) {
          Snackbar.make(view, "ERROR", Snackbar.LENGTH_INDEFINITE);
        }
      }
      searchCall = null;
    }
  }
}
