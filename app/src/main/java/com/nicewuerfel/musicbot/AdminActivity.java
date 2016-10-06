package com.nicewuerfel.musicbot;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.Toast;

import com.nicewuerfel.musicbot.api.ApiConnector;
import com.nicewuerfel.musicbot.api.ApiUser;
import com.nicewuerfel.musicbot.api.DummyCallback;
import com.nicewuerfel.musicbot.api.Playlist;

import java.util.List;

import id.ridsatrio.optio.Optional;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminActivity extends AppCompatActivity {

  private Call<List<Playlist>> getPlaylistsCall = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_admin);

    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setDisplayHomeAsUpEnabled(true);
    }
  }

  @Override
  protected void onStart() {
    super.onStart();
    Optional<ApiUser> userOpt = ApiConnector.getUser();
    if (!userOpt.isPresent()) {
      Intent loginIntent = new Intent(this, LoginActivity.class);
      startActivity(loginIntent);
      return;
    }
    ApiUser user = userOpt.get();

    View exitButton = findViewById(R.id.exit_button);
    if (!user.hasPermission("exit")) {
      exitButton.setVisibility(View.GONE);
    }

    View resetButton = findViewById(R.id.reset_button);
    if (!user.hasPermission("reset")) {
      resetButton.setVisibility(View.GONE);
    }
    View editPermissionsButton = findViewById(R.id.edit_perms_button);
    if (!user.hasPermission("admin")) {
      editPermissionsButton.setVisibility(View.GONE);
    }
    View selectButton = findViewById(R.id.select_playlist_button);
    if (!user.hasPermission("select_playlist")) {
      selectButton.setVisibility(View.GONE);
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        finish();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  public void onExitButton(View view) {
    new AlertDialog.Builder(this)
        .setCancelable(true)
        .setTitle("Shutdown bot")
        .setMessage("Are you sure?")
        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            ApiConnector.getService().exitBot().enqueue(new DummyCallback<String>());
          }
        })
        .setNegativeButton(android.R.string.no, null)
        .show();
  }

  public void onResetButton(View view) {
    new AlertDialog.Builder(this)
        .setCancelable(true)
        .setTitle("Reset bot")
        .setMessage("Are you sure?")
        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            ApiConnector.getService().resetBot().enqueue(new DummyCallback<String>());
          }
        })
        .setNegativeButton(android.R.string.no, null)
        .show();
  }

  public void onEditPermissionsButton(View view) {
    Intent intent = new Intent(this, EditPermissionsActivity.class);
    startActivity(intent);
  }

  public void onSelectPlaylistButton(View view) {
    if (getPlaylistsCall != null) {
      getPlaylistsCall.cancel();
      getPlaylistsCall = null;
    }
    getPlaylistsCall = ApiConnector.getService().getAvailablePlaylists();
    getPlaylistsCall.enqueue(new Callback<List<Playlist>>() {
      @Override
      public void onResponse(Call<List<Playlist>> call, Response<List<Playlist>> response) {
        if (response.isSuccessful()) {
          List<Playlist> playlists = response.body();
          if (playlists != null) {
            final ArrayAdapter<Playlist> adapter = new ArrayAdapter<>(AdminActivity.this, R.layout.playlist_text, playlists);

            runOnUiThread(new Runnable() {
              @Override
              public void run() {
                if (!isFinishing()) {
                  new AlertDialog.Builder(AdminActivity.this)//TODO fix this shit
                      .setTitle("Select playlist")
                      .setSingleChoiceItems(adapter, 0, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                          Playlist playlist = adapter.getItem(which);
                          if (playlist == null) {
                            return;
                          }
                          ApiConnector.getService().setActivePlaylist(playlist.getId()).enqueue(new DummyCallback<String>());
                          dialog.dismiss();
                        }
                      })
                      .show();
                }
              }
            });
          }
        } else {
          Toast.makeText(AdminActivity.this, "Could not load available playlists", Toast.LENGTH_SHORT).show();
        }
      }

      @Override
      public void onFailure(Call<List<Playlist>> call, Throwable t) {
        if (!call.isCanceled()) {
          Toast.makeText(AdminActivity.this, "Could not load available playlists", Toast.LENGTH_SHORT).show();
        }
      }
    });
  }
}
