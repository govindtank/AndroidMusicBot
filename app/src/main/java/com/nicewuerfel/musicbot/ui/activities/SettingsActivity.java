package com.nicewuerfel.musicbot.ui.activities;


import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.nicewuerfel.musicbot.PreferenceKey;
import com.nicewuerfel.musicbot.R;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.MalformedURLException;
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

  public static final String EXTRA_AUTO_DETECT = "auto-detect";

  private GeneralPreferenceFragment fragment;
  @Nullable
  private AsyncTask<Void, Void, String> detection;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setupActionBar();
    fragment = new GeneralPreferenceFragment();
    getFragmentManager().beginTransaction()
        .replace(android.R.id.content, fragment)
        .commit();
    getFragmentManager().executePendingTransactions();
  }

  @Override
  protected void onStart() {
    super.onStart();
    if (!getIntent().hasExtra(EXTRA_AUTO_DETECT)) {
      View view = findViewById(android.R.id.content);
      if (view != null) {
        Snackbar.make(view, "Press the refresh button to auto-detect the bot IP", Snackbar.LENGTH_INDEFINITE).show();
      }
    }
  }

  @Override
  protected void onDestroy() {
    fragment = null;
    if (detection != null && isFinishing()) {
      detection.cancel(false);
      detection = null;
    }
    super.onDestroy();
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
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.settings_action_bar_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        super.onBackPressed();
        return true;
      case R.id.auto_detect:
        autoDetect();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @UiThread
  private void autoDetect() {
    if (detection == null || detection.getStatus() == AsyncTask.Status.FINISHED) {
      detection = new AsyncTask<Void, Void, String>() {
        @Override
        protected String doInBackground(Void... params) {
          DatagramSocket socket = null;
          try {
            socket = new DatagramSocket(42945);
            socket.setSoTimeout(4000);
            byte[] buffer = new byte[128];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            return packet.getAddress().getHostAddress();
          } catch (IOException e) {
            return null;
          } finally {
            if (socket != null) {
              socket.close();
            }
          }
        }

        @Override
        protected void onPostExecute(String s) {
          if (fragment != null && s != null) {
            Preference preference = fragment.findPreference(PreferenceKey.BOT_HOST);
            if (preference.getOnPreferenceChangeListener().onPreferenceChange(preference, s)) {
              SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this);
              prefs.edit().putString(preference.getKey(), s).apply();
            }
          }
          Toast.makeText(SettingsActivity.this, getString(R.string.auto_detect_result, s), Toast.LENGTH_SHORT).show();
          if (getIntent().hasExtra(EXTRA_AUTO_DETECT) && s != null) {
            onBackPressed();
          }
        }
      };
      detection.execute();
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

      // Bind the summaries of EditText/List/Dialog/Ringtone preferences
      // to their values. When their values change, their summaries are
      // updated to reflect the new value, per the Android Design
      // guidelines.
      final EditTextPreference botHostPref = (EditTextPreference) findPreference(PreferenceKey.BOT_HOST);

      botHostPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
          String stringValue = newValue.toString().trim();
          try {
            URL url = new URL("https", stringValue, 8443, "");
            prefs.edit().putString(PreferenceKey.BOT_URL, url.toExternalForm()).apply();
            botHostPref.setSummary(stringValue);
            return true;
          } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
          }
        }
      });
      botHostPref.getOnPreferenceChangeListener().onPreferenceChange(botHostPref, prefs.getString(botHostPref.getKey(), null));

      CheckBoxPreference checkBoxPref = (CheckBoxPreference) findPreference("bot_trust");
      checkBoxPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object o) {
          return true;
        }
      });
      checkBoxPref.getOnPreferenceChangeListener().onPreferenceChange(checkBoxPref, prefs.getBoolean(checkBoxPref.getKey(), false));

      if (botHostPref.getText().trim().equals("localhost")) {
        Toast.makeText(getActivity(), R.string.auto_auto_detect, Toast.LENGTH_SHORT).show();
        ((SettingsActivity) getActivity()).autoDetect();
      }
    }
  }
}
