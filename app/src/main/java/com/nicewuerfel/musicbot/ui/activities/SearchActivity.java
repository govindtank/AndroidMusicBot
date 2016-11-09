package com.nicewuerfel.musicbot.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.nicewuerfel.musicbot.PreferenceKey;
import com.nicewuerfel.musicbot.R;
import com.nicewuerfel.musicbot.api.ApiConnector;
import com.nicewuerfel.musicbot.api.BotState;
import com.nicewuerfel.musicbot.api.DummyCallback;
import com.nicewuerfel.musicbot.api.MusicApi;
import com.nicewuerfel.musicbot.api.Song;
import com.nicewuerfel.musicbot.ui.fragments.SearchFragment;
import com.nicewuerfel.musicbot.ui.fragments.SongFragment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;

public class SearchActivity extends AppCompatActivity implements SongFragment.OnListFragmentInteractionListener {

  private static final String ARG_QUERY = "QUERY";

  private String query = "";

  private SearchFragmentAdapter searchFragmentAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_search);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    setTitle(R.string.title_activity_search);
    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setDisplayHomeAsUpEnabled(true);
    }

    final ViewPager viewPager = (ViewPager) findViewById(R.id.view_pager);
    viewPager.setAdapter((searchFragmentAdapter = new SearchFragmentAdapter(getSupportFragmentManager())));
    viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
      @Override
      public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
      }

      @Override
      public void onPageSelected(int position) {
        refreshSearchResults();
      }

      @Override
      public void onPageScrollStateChanged(int state) {
      }
    });
  }

  @Override
  protected void onStart() {
    super.onStart();
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
    if (preferences.getString(PreferenceKey.TOKEN, null) == null) {
      Intent intent = new Intent(this, LoginActivity.class);
      startActivity(intent);
    }

    if (BotState.getInstance().getMusicApis().isEmpty()) {
      Toast.makeText(this, getString(R.string.no_search_apis), Toast.LENGTH_SHORT).show();
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
    super.onStop();
  }

  @Override
  protected void onDestroy() {
    query = "";
    searchFragmentAdapter = null;
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
    searchView.setQueryHint(getString(R.string.search_hint));
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
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Nullable
  private SearchFragment getCurrentFragment() {
    SearchFragmentAdapter searchFragmentAdapter = this.searchFragmentAdapter;
    if (searchFragmentAdapter == null) {
      return null;
    }
    ViewPager viewPager = (ViewPager) findViewById(R.id.view_pager);
    return (SearchFragment) searchFragmentAdapter.getItem(viewPager.getCurrentItem());
  }

  private void refreshSearchResults() {
    String query = this.query;

    SearchFragment searchFragment = getCurrentFragment();
    if (searchFragment == null) {
      return;
    }

    searchFragment.search(query);
  }

  void logout() {
    PreferenceManager.getDefaultSharedPreferences(this).edit().remove(PreferenceKey.TOKEN).apply();
    Intent loginIntent = new Intent(this, LoginActivity.class);
    finish(); // Don't return to this activity after login
    startActivity(loginIntent);
  }

  @Override
  public void onSongClick(Song song) {
    ApiConnector.getService().enqueue(song).enqueue(new DummyCallback<String>() {
      @Override
      public void onResponse(Call<String> call, Response<String> response) {
        if (response.code() == 401) {
          logout();
        }
      }
    });
    finish();
  }

  @Override
  public void onSongRemoveClick(Song song) {
    // never happens, ignore
  }
}

class SearchFragmentAdapter extends FragmentPagerAdapter {
  private Map<MusicApi, Fragment> apis = new HashMap<>();

  SearchFragmentAdapter(FragmentManager fragmentManager) {
    super(fragmentManager);
  }

  @Override
  public Fragment getItem(int position) {
    List<MusicApi> musicApis = BotState.getInstance().getMusicApis();
    MusicApi api = musicApis.get(position);

    Fragment result = apis.get(api);
    if (result == null) {
      apis.put(api, (result = SearchFragment.newInstance(api)));
    }

    return result;
  }

  @Override
  public int getCount() {
    return BotState.getInstance().getMusicApis().size();
  }

  @Override
  public CharSequence getPageTitle(int position) {
    return BotState.getInstance().getMusicApis().get(position).getPrettyName();
  }
}
