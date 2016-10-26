package com.nicewuerfel.musicbot;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.nicewuerfel.musicbot.api.AlbumArtLoader;
import com.nicewuerfel.musicbot.api.ApiConnector;
import com.nicewuerfel.musicbot.api.BotService;
import com.nicewuerfel.musicbot.api.BotState;
import com.nicewuerfel.musicbot.api.PlayerState;
import com.nicewuerfel.musicbot.api.Song;
import com.nicewuerfel.musicbot.ui.activities.MainActivity;

import java.util.Observable;
import java.util.Observer;

public class NotificationService extends Service {
  private static final String LOG_TAG = "NotificationService";

  public static final String ACTION_START = "com.nicewuerfel.musicbot.START";
  private static final String ACTION_PAUSE = "com.nicewuerfel.musicbot.PAUSE";
  private static final String ACTION_NEXT = "com.nicewuerfel.musicbot.NEXT";
  private static final String ACTION_STOP = "com.nicewuerfel.musicbot.STOP";

  private final Observer stateObserver;
  @NonNull
  private PlayerState state = PlayerState.EMPTY;
  @Nullable
  private Bitmap bitmap = null;

  public NotificationService() {
    stateObserver = new Observer() {
      @Override
      public void update(Observable observable, Object o) {
        if (o instanceof PlayerState) {
          showNotification((PlayerState) o);
        }
      }
    };
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  private BotService getService() {
    return ApiConnector.getService(PreferenceManager.getDefaultSharedPreferences(this), getString(R.string.pref_default_server_url));
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if(intent == null) {
      System.out.println("null intent");
      return Service.START_REDELIVER_INTENT;
    }
    switch (intent.getAction()) {
      case ACTION_START:
        showNotification(BotState.getInstance().getPlayerState());
        if (startId == 1) {
          registerObservers();
        }
        break;
      case ACTION_PAUSE:
        getService().togglePause().enqueue(new BotState.PlayerStateCallback());
        break;
      case ACTION_NEXT:
        getService().nextSong().enqueue(new BotState.PlayerStateCallback());
        break;
      case ACTION_STOP:
        unregisterObservers();
        stopSelfResult(startId);
        return Service.START_NOT_STICKY;
      default:
        Log.w(LOG_TAG, "unknown action: " + intent.getAction());
    }
    return Service.START_STICKY;
  }

  @Override
  public void onDestroy() {
    unregisterObservers();
    bitmap = null;
    super.onDestroy();
  }

  private void registerObservers() {
    BotState.getInstance().addPlayerStateObserver(stateObserver);
  }

  private void unregisterObservers() {
    BotState.getInstance().deletePlayerStateObserver(stateObserver);
  }

  private void showNotification(@NonNull PlayerState state) {
    boolean sameSong = false;
    if ((sameSong = state.getCurrentSong().equals(this.state.getCurrentSong())) && state.isPaused() == this.state.isPaused()) {
      return;
    }
    this.state = state;
    if (sameSong) {
      showNotification(state, bitmap);
    } else {
      showNotification(state, null);
      AlbumArtLoader.getInstance(this).load(state.getCurrentSong(), new AlbumArtLoader.ImageLoadingListener() {
        @Override
        public void onLoadingComplete(@NonNull Song song, @Nullable Bitmap bitmap) {
          if (bitmap == null) {
            return;
          }
          PlayerState currentState = BotState.getInstance().getPlayerState();
          if (song.equals(currentState.getCurrentSong())) {
            showNotification(currentState, bitmap);
          }
        }
      });
    }
  }

  private void showNotification(@NonNull PlayerState state, @Nullable Bitmap albumArt) {
    Song song = state.getCurrentSong();
    this.bitmap = albumArt;

    PendingIntent mainIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

    Intent pauseIntent = new Intent(this, NotificationService.class);
    pauseIntent.setAction(ACTION_PAUSE);
    PendingIntent pendingPause = PendingIntent.getService(this, 0, pauseIntent, PendingIntent.FLAG_CANCEL_CURRENT);

    Intent nextIntent = new Intent(this, NotificationService.class);
    nextIntent.setAction(ACTION_NEXT);
    PendingIntent pendingNext = PendingIntent.getService(this, 0, nextIntent, PendingIntent.FLAG_CANCEL_CURRENT);

    Intent stopIntent = new Intent(this, NotificationService.class);
    stopIntent.setAction(ACTION_STOP);
    PendingIntent pendingStop = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_CANCEL_CURRENT);

    int pauseIcon = state.isPaused() ? R.drawable.ic_action_playback_play : R.drawable.ic_action_playback_pause;
    NotificationCompat.Action pauseAction = new NotificationCompat.Action(pauseIcon, "Pause", pendingPause);
    NotificationCompat.Action nextAction = new NotificationCompat.Action(R.drawable.ic_action_playback_next, "Next", pendingNext);

    NotificationCompat.MediaStyle style = new NotificationCompat.MediaStyle();
    ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(R.id.player_notification,
        new NotificationCompat.Builder(this)
            .setContentInfo(getString(R.string.app_name))
            .setContentTitle(song.getTitle())
            .setContentText(song.getDescription())
            .setSmallIcon(R.drawable.ic_library_music)
            .setLargeIcon(albumArt)
            .setContentIntent(mainIntent)
            .setDeleteIntent(pendingStop)
            .addAction(pauseAction)
            .addAction(nextAction)
            .setStyle(style)
            .build());
  }
}
