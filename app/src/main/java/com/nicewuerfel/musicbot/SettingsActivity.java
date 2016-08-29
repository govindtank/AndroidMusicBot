package com.nicewuerfel.musicbot;


import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setupActionBar();
    getFragmentManager().beginTransaction()
        .replace(android.R.id.content, new GeneralPreferenceFragment())
        .commit();
  }

  /**
   * Set up the {@link android.app.ActionBar}, if the API is available.
   */
  private void setupActionBar() {
    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      // Show the Up button in the action bar.
      actionBar.setDisplayHomeAsUpEnabled(true);
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        super.onBackPressed();
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  /**
   * This fragment shows general preferences only. It is used when the
   * activity is showing a two-pane settings UI.
   */
  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  public static class GeneralPreferenceFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      addPreferencesFromResource(R.xml.pref_general);
      setHasOptionsMenu(true);

      final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
      final String defaultUrl = getString(R.string.pref_default_server);

      // Bind the summaries of EditText/List/Dialog/Ringtone preferences
      // to their values. When their values change, their summaries are
      // updated to reflect the new value, per the Android Design
      // guidelines.
      final EditTextPreference botUrlPref = (EditTextPreference) findPreference("bot_url");
      botUrlPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
          String stringValue = value.toString().trim();
          URI oldUri;
          try {
            oldUri = new URI(stringValue);
            if (oldUri.getScheme() == null || oldUri.getHost() == null) {
              stringValue = "http://" + stringValue;
              oldUri = new URI(stringValue);
            }
          } catch (URISyntaxException e) {
            try {
              stringValue = "http://" + stringValue;
              oldUri = new URI(stringValue);
            } catch (URISyntaxException e1) {
              return false;
            }
          }

          int oldPort = oldUri.getPort();
          URI newUri;
          try {
            newUri = new URI(oldUri.getScheme(), oldUri.getUserInfo(), oldUri.getHost(), oldPort == -1 ? 8000 : oldPort, oldUri.getPath(), null, null);
          } catch (URISyntaxException e) {
            return false;
          }

          URL newUrl;
          try {
            newUrl = newUri.toURL();
          } catch (IOException e) {
            return false;
          }

          String urlString = newUrl.toExternalForm();
          if (!urlString.endsWith("/")) {
            urlString += "/";
          }
          prefs.edit().putString(preference.getKey(), urlString).apply();
          preference.setSummary(urlString);
          botUrlPref.setText(urlString);
          return false;
        }
      });
      botUrlPref.getOnPreferenceChangeListener().onPreferenceChange(botUrlPref, prefs.getString(botUrlPref.getKey(), defaultUrl));


      CheckBoxPreference checkBoxPref = (CheckBoxPreference) findPreference("bot_trust");
      checkBoxPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object o) {
          return true;
        }
      });
      checkBoxPref.getOnPreferenceChangeListener().onPreferenceChange(checkBoxPref, prefs.getBoolean(checkBoxPref.getKey(), false));
    }
  }
}
