package com.nicewuerfel.musicbot;

import android.os.PersistableBundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.nicewuerfel.musicbot.api.ApiConnector;
import com.nicewuerfel.musicbot.api.ApiUser;
import com.nicewuerfel.musicbot.api.DummyCallback;
import com.nicewuerfel.musicbot.api.Permission;
import com.nicewuerfel.musicbot.api.User;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditPermissionsActivity extends AppCompatActivity implements UserFragment.OnListFragmentInteractionListener, PermissionFragment.OnListFragmentInteractionListener {

  private static final String ARG_SELECTED_USER = "selected-user";

  private Call<?> refreshCall;
  private ApiUser selectedUser = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_edit_permissions);
    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setDisplayHomeAsUpEnabled(true);
    }

    if (savedInstanceState != null) {
      selectedUser = savedInstanceState.getParcelable(ARG_SELECTED_USER);
    }

    getSupportFragmentManager().beginTransaction()
        .add(R.id.activity_edit_permissions, LoadingFragment.newInstance())
        .commit();
    getSupportFragmentManager().executePendingTransactions();

    if (selectedUser != null) {
      onEditClick(selectedUser);
    }
  }

  @Override
  protected void onStart() {
    super.onStart();
    refresh();
  }

  @Override
  protected void onResume() {
    super.onResume();
    refresh();
  }

  @Override
  public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
    outState.putParcelable(ARG_SELECTED_USER, selectedUser);
    super.onSaveInstanceState(outState, outPersistentState);
  }

  @Override
  protected void onStop() {
    if (refreshCall != null) {
      refreshCall.cancel();
      refreshCall = null;
    }
    super.onStop();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.refresh_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        if (selectedUser != null) {
          selectedUser = null;
          refresh();
        } else {
          onBackPressed();
        }
        return true;
      case R.id.refresh_button:
        refresh();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  private void refresh() {
    final Call<List<ApiUser>> refreshCall = ApiConnector.getService().getUsers();
    refreshCall.enqueue(new Callback<List<ApiUser>>() {
      @Override
      public void onResponse(Call<List<ApiUser>> call, Response<List<ApiUser>> response) {
        if (!response.isSuccessful()) {
          showError();
          return;
        }

        final List<ApiUser> users = response.body();
        if (users != null) {
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              if (selectedUser == null) {
                getSupportFragmentManager().beginTransaction()
                    .replace(R.id.activity_edit_permissions, UserFragment.newInstance(new ArrayList<>(users)))
                    .commit();
              } else {
                ApiUser selectedUser = EditPermissionsActivity.this.selectedUser;
                for (ApiUser user : users) {
                  if (selectedUser.equals(user)) {
                    onEditClick(user);
                    break;
                  }
                }
              }
            }
          });
        }
        EditPermissionsActivity.this.refreshCall = null;
      }

      @Override
      public void onFailure(Call<List<ApiUser>> call, Throwable t) {
        showError();
      }

      private void showError() {
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.activity_edit_permissions, ConnectionErrorFragment.newInstance())
                .commit();
          }
        });
        EditPermissionsActivity.this.refreshCall = null;
      }
    });
    this.refreshCall = refreshCall;

  }

  @Override
  public void onEditClick(final ApiUser user) {
    selectedUser = user;
    if (this.refreshCall != null) {
      refreshCall.cancel();
    }
    final Call<List<Permission>> refreshCall = ApiConnector.getService().getAvailablePermissions();
    refreshCall.enqueue(new Callback<List<Permission>>() {
      @Override
      public void onResponse(Call<List<Permission>> call, Response<List<Permission>> response) {
        if (!response.isSuccessful()) {
          showError();
        }

        List<Permission> permissions = response.body();
        if (permissions == null) {
          return;
        }
        final ArrayList<Permission> permissionsArrayList = new ArrayList<>(permissions);
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.activity_edit_permissions, PermissionFragment.newInstance(user, permissionsArrayList))
                .commit();
          }
        });
      }

      @Override
      public void onFailure(Call<List<Permission>> call, Throwable t) {
        showError();
      }

      private void showError() {
        Toast.makeText(EditPermissionsActivity.this, "Error loading available permissions.", Toast.LENGTH_SHORT).show();
      }
    });
    this.refreshCall = refreshCall;
  }

  @Override
  public void onPermissionToggle(ApiUser user, Permission permission, boolean enable) {

    Call<String> call;
    if (enable) {
      call = ApiConnector.getService().grantPermission(user.getUsername(), permission);
    } else {
      call = ApiConnector.getService().revokePermission(user.getUsername(), permission);
    }

    call.enqueue(new Callback<String>() {

      @Override
      public void onResponse(Call<String> call, Response<String> response) {
        if (response.isSuccessful()) {
          //refresh();
        }
      }

      @Override
      public void onFailure(Call<String> call, Throwable t) {

      }
    }); // TODO handle failure
  }
}
