package com.nicewuerfel.musicbot;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;

import com.nicewuerfel.musicbot.api.ApiConnector;
import com.nicewuerfel.musicbot.api.DummyCallback;

public class AdminActivity extends AppCompatActivity implements View.OnClickListener {

  private enum Action {
    EXIT(R.string.admin_action_exit), RESET(R.string.admin_action_reset);

    private final int stringResourceId;

    Action(int stringResourceId) {
      this.stringResourceId = stringResourceId;
    }

  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_admin);
    ViewGroup root = (ViewGroup) findViewById(R.id.activity_admin_layout);

    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setDisplayHomeAsUpEnabled(true);
    }

    root.addView(createButton(Action.EXIT));
    root.addView(createButton(Action.RESET));
  }

  private Button createButton(Action action) {
    Button button = (Button) getLayoutInflater().inflate(R.layout.activity_admin_button, null);
    button.setId(action.ordinal());
    button.setText(getString(action.stringResourceId));
    button.setOnClickListener(this);
    return button;
  }

  @Override
  public void onClick(View view) {
    Action action = Action.values()[view.getId()];
    switch (action) {
      case RESET:
        ApiConnector.getService().resetBot().enqueue(new DummyCallback<String>());
        return;
      case EXIT:
        ApiConnector.getService().exitBot().enqueue(new DummyCallback<String>());
        return;
      default:
        // TODO do something
    }
  }

  @Override
  protected void onStart() {
    super.onStart();
    if (!ApiConnector.isAdmin()) {
      finish();
      return;
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
}
