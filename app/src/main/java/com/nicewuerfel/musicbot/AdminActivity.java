package com.nicewuerfel.musicbot;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.nicewuerfel.musicbot.api.ApiConnector;
import com.nicewuerfel.musicbot.api.ApiUser;
import com.nicewuerfel.musicbot.api.DummyCallback;

import id.ridsatrio.optio.Optional;

public class AdminActivity extends AppCompatActivity {

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
        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
          }
        })
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
        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
          }
        })
        .show();
  }

  public void onEditPermissionsButton(View view) {
    Toast.makeText(this, "Not implemented", Toast.LENGTH_SHORT).show();
    // TODO show edit permissions activity
  }

  public void onSelectPlaylistButton(View view) {
    Toast.makeText(this, "Not implemented", Toast.LENGTH_SHORT).show();
    // TODO implement
  }
}
