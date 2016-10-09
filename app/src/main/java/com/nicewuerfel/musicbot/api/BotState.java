package com.nicewuerfel.musicbot.api;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Observer;

import retrofit2.Call;
import retrofit2.Response;

public final class BotState {
  private static final BotState instance = new BotState();

  private final Observable playerStateObservable;
  private final Observable hasAdminObservable;
  private final Observable musicApisObservable;

  @NonNull
  private PlayerState playerState;
  private boolean hasAdmin;
  @NonNull
  private List<MusicApi> apis;

  private final Handler handler;

  private final Runnable notifyPlayerState;
  private final Runnable notifyHasAdmin;
  private final Runnable notifyMusicApis;


  private BotState() {
    this.playerStateObservable = new Observable();
    this.hasAdminObservable = new Observable();
    this.musicApisObservable = new Observable();

    playerState = PlayerState.EMPTY;
    hasAdmin = true;
    apis = Collections.emptyList();

    this.handler = new Handler(Looper.getMainLooper());
    this.notifyPlayerState = new Runnable() {
      @Override
      public void run() {
        playerStateObservable.notifyObservers(playerState);
      }
    };
    this.notifyHasAdmin = new Runnable() {
      @Override
      public void run() {
        hasAdminObservable.notifyObservers(hasAdmin);
      }
    };
    this.notifyMusicApis = new Runnable() {
      @Override
      public void run() {
        musicApisObservable.notifyObservers(apis);
      }
    };
  }

  public static BotState getInstance() {
    return instance;
  }

  public void resetObservers() {
    playerStateObservable.deleteObservers();
    hasAdminObservable.deleteObservers();
    musicApisObservable.deleteObservers();
  }

  /**
   * Add a listener to player state changes. The observer will be called on the UI thread.<br>
   * Passing null does nothing.
   *
   * @param observer the observer
   */
  public void addPlayerStateObserver(@Nullable Observer observer) {
    playerStateObservable.addObserver(observer);
  }

  public void deletePlayerStateObserver(@Nullable Observer observer) {
    playerStateObservable.deleteObserver(observer);
  }

  /**
   * Add a listener to the hasAdmin field. The observer will be called on the UI thread.<br>
   * Passing null does nothing.
   *
   * @param observer the observer
   */
  public void addHasAdminObserver(@Nullable Observer observer) {
    hasAdminObservable.addObserver(observer);
  }

  public void deleteHasAdminObserver(@Nullable Observer observer) {
    hasAdminObservable.deleteObserver(observer);
  }

  /**
   * Add a listener to the list of available music APIs. The observer will be called on the UI thread.<br>
   * Passing null does nothing.
   *
   * @param observer the observer
   */
  public void addMusicApisObserver(@Nullable Observer observer) {
    musicApisObservable.addObserver(observer);
  }

  public void deleteMusicApisObserver(@Nullable Observer observer) {
    musicApisObservable.deleteObserver(observer);
  }

  @NonNull
  public PlayerState getPlayerState() {
    return playerState;
  }

  public void setPlayerState(@Nullable PlayerState playerState) {
    synchronized (playerStateObservable) {
      if (!this.playerState.equals(playerState)) {
        this.playerState = playerState;
        handler.post(notifyPlayerState);
      }
    }
  }

  public boolean hasAdmin() {
    return hasAdmin;
  }

  public void hasAdmin(boolean hasAdmin) {
    synchronized (hasAdminObservable) {
      if (this.hasAdmin != hasAdmin) {
        this.hasAdmin = hasAdmin;
        handler.post(notifyHasAdmin);
      }
    }
  }

  public List<MusicApi> getMusicApis() {
    return apis;
  }

  public void setMusicApis(List<MusicApi> apis) {
    synchronized (musicApisObservable) {
      if (!this.apis.equals(apis)) {
        this.apis = apis;
        handler.post(notifyMusicApis);
      }
    }
  }

  /**
   * A retrofit callback that updates the player state on success.
   */
  public static final class PlayerStateCallback extends DummyCallback<PlayerState> {
    @Override
    public void onResponse(Call<PlayerState> call, Response<PlayerState> response) {
      if (response.isSuccessful()) {
        PlayerState state = response.body();

        if (state != null) {
          BotState.getInstance().setPlayerState(state);
        }
      }
    }
  }

  /**
   * A retrofit callback that updates the hasAdmin state on success.
   */
  public static final class HasAdminCallback extends DummyCallback<Boolean> {
    @Override
    public void onResponse(Call<Boolean> call, Response<Boolean> response) {
      if (response.isSuccessful()) {
        Boolean hasAdmin = response.body();
        if (hasAdmin != null) {
          BotState.getInstance().hasAdmin(hasAdmin);
        }
      }
    }
  }

  /**
   * A retrofit callback that updates the musicApis state on success.
   */
  public static final class MusicApisCallback extends DummyCallback<List<MusicApi>> {
    @Override
    public void onResponse(Call<List<MusicApi>> call, Response<List<MusicApi>> response) {
      if (response.isSuccessful()) {
        List<MusicApi> apis = response.body();

        if (apis != null) {
          BotState.getInstance().setMusicApis(apis);
        }
      }
    }
  }
}
