package com.nicewuerfel.musicbot;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.nicewuerfel.musicbot.api.ApiConnector;
import com.nicewuerfel.musicbot.api.User;

import java.io.IOException;
import java.util.Objects;

import javax.net.ssl.SSLHandshakeException;

import retrofit2.Response;

/**
 * A login screen that offers login via username/password.
 */
public class LoginActivity extends AppCompatActivity {

  /**
   * Keep track of the login task to ensure we can cancel it if requested.
   */
  private UserLoginTask mAuthTask = null;

  // UI references.
  private AutoCompleteTextView mUsernameView;
  private EditText mPasswordView;
  private View mProgressView;
  private View mLoginFormView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_login);
    // Set up the login form.
    mUsernameView = (AutoCompleteTextView) findViewById(R.id.username);

    mPasswordView = (EditText) findViewById(R.id.password);
    mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
      @Override
      public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
        if (id == R.id.login || id == EditorInfo.IME_NULL) {
          attemptLogin(Boolean.FALSE);
          return true;
        }
        return false;
      }
    });

    Button mUsernameSignInButton = (Button) findViewById(R.id.username_sign_in_button);
    mUsernameSignInButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View view) {
        attemptLogin(Boolean.FALSE);
      }
    });

    Button mUsernameRegisterButton = (Button) findViewById(R.id.username_register_button);
    mUsernameRegisterButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View view) {
        attemptLogin(Boolean.TRUE);
      }
    });

    mLoginFormView = findViewById(R.id.login_form);
    mProgressView = findViewById(R.id.login_progress);

    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
    try {
      ApiConnector.getService(preferences, getString(R.string.pref_default_server));
    } catch (IllegalArgumentException e) {
      Toast.makeText(this, getString(R.string.invalid_url), Toast.LENGTH_LONG).show();
      Intent intent = new Intent(this, SettingsActivity.class);
      startActivity(intent);
      return;
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.login_action_bar_menu, menu);
    return true;
  }

  @Override
  protected void onStart() {
    super.onStart();
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    if (prefs.getString("bot_token", null) != null) {
      finish();
    }
    ApiConnector.getService(prefs, getString(R.string.pref_default_server));
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_settings:
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  /**
   * Attempts to sign in or register the account specified by the login form.
   * If there are form errors (invalid email, missing fields, etc.), the
   * errors are presented and no actual login attempt is made.
   */
  private void attemptLogin(Boolean register) {
    if (mAuthTask != null) {
      return;
    }

    // Reset errors.
    mUsernameView.setError(null);
    mPasswordView.setError(null);

    // Store values at the time of the login attempt.
    String username = mUsernameView.getText().toString();
    String password = mPasswordView.getText().toString();

    boolean cancel = false;
    View focusView = null;

    // Check for a valid password, if the user entered one.
    if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
      mPasswordView.setError(getString(R.string.error_invalid_password));
      focusView = mPasswordView;
      cancel = true;
    }

    // Check for a valid username address.
    if (TextUtils.isEmpty(username)) {
      mUsernameView.setError(getString(R.string.error_field_required));
      focusView = mUsernameView;
      cancel = true;
    } else if (!isUsernameValid(username)) {
      mUsernameView.setError(getString(R.string.error_invalid_username));
      focusView = mUsernameView;
      cancel = true;
    }

    if (cancel) {
      // There was an error; don't attempt login and focus the first
      // form field with an error.
      focusView.requestFocus();
    } else {
      // Show a progress spinner, and kick off a background task to
      // perform the user login attempt.
      showProgress(true);
      mAuthTask = new UserLoginTask(username, password);
      mAuthTask.execute(register);
    }
  }

  private boolean isUsernameValid(String username) {
    return !username.isEmpty();
  }

  private boolean isPasswordValid(String password) {
    return password.length() > 5;
  }

  /**
   * Shows the progress UI and hides the login form.
   */
  @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
  private void showProgress(final boolean show) {
    // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
    // for very easy animations. If available, use these APIs to fade-in
    // the progress spinner.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
      int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

      mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
      mLoginFormView.animate().setDuration(shortAnimTime).alpha(
          show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
          mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
      });

      mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
      mProgressView.animate().setDuration(shortAnimTime).alpha(
          show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
          mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
      });
    } else {
      // The ViewPropertyAnimator APIs are not available, so simply show
      // and hide the relevant UI components.
      mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
      mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
    }
  }


  private enum UserLoginTaskResult {
    SUCCESS, WRONG_COMBO, USER_EXISTS, SSL_CERT, ERROR
  }

  /**
   * Represents an asynchronous login/registration task used to authenticate
   * the user.
   */
  public class UserLoginTask extends AsyncTask<Boolean, Void, UserLoginTaskResult> {

    private final User user;

    UserLoginTask(String username, String password) {
      this.user = new User(username, password);
    }

    @Override
    protected UserLoginTaskResult doInBackground(Boolean... params) {
      Response<String> response;
      if (params.length != 1) {
        return UserLoginTaskResult.ERROR;
      }
      Boolean register = Objects.requireNonNull(params[0]);

      try {
        if (register) {
          response = ApiConnector.getService().register(user).execute();
        } else {
          response = ApiConnector.getService().login(user).execute();
        }
      } catch (SSLHandshakeException e) {
        return UserLoginTaskResult.SSL_CERT;
      } catch (IOException e) {
        return UserLoginTaskResult.ERROR;
      }

      if (response.isSuccessful()) {
        String token = response.body();
        if (token != null) {
          PreferenceManager.getDefaultSharedPreferences(LoginActivity.this).edit().putString("bot_token", token).apply();
          return UserLoginTaskResult.SUCCESS;
        }
      } else {
        if (register) {
          if (response.code() == 409) {
            return UserLoginTaskResult.USER_EXISTS;
          }
        } else {
          return UserLoginTaskResult.WRONG_COMBO;
        }
      }

      return UserLoginTaskResult.ERROR;
    }

    @Override
    protected void onPostExecute(final UserLoginTaskResult result) {
      mAuthTask = null;
      showProgress(false);

      mUsernameView.setError(null);
      mPasswordView.setError(null);

      switch (result) {
        case SUCCESS:
          finish();
          return;
        case USER_EXISTS:
          mUsernameView.setError(getString(R.string.error_user_exists));
          mUsernameView.requestFocus();
          return;
        case WRONG_COMBO:
          mPasswordView.setError(getString(R.string.error_incorrect_password));
          mPasswordView.requestFocus();
          return;
        case SSL_CERT:
          Toast.makeText(LoginActivity.this, getString(R.string.ssl_error), Toast.LENGTH_LONG).show();
          return;
        case ERROR:
        default:
          Toast.makeText(LoginActivity.this, getString(R.string.error_unknown), Toast.LENGTH_SHORT).show();
      }
    }

    @Override
    protected void onCancelled() {
      mAuthTask = null;
      showProgress(false);
    }
  }
}

