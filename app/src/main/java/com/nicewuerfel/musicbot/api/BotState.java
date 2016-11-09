package com.nicewuerfel.musicbot.api;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Observer;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Response;

public final class BotState {
  private static final BotState instance = new BotState();

  @NonNull
  private PlayerState playerState;
  private boolean hasAdmin;
  @NonNull
  private List<MusicApi> apis;

  private final Handler handler;

  private final ScheduledExecutorService executor;

  private enum Index {
    PLAYER_STATE, HAS_ADMIN, MUSIC_APIS
  }

  private final ScheduledFuture[] futures;
  private final Observable[] observables;
  private final Runnable[] notifiers;


  private BotState() {
    observables = new Observable[Index.values().length];
    for (Index index : Index.values()) {
      observables[index.ordinal()] = new Observable();
    }

    playerState = PlayerState.EMPTY;
    hasAdmin = true;
    apis = Collections.emptyList();

    this.handler = new Handler(Looper.getMainLooper());

    notifiers = new Runnable[Index.values().length];
    notifiers[Index.PLAYER_STATE.ordinal()] = new Runnable() {
      @Override
      public void run() {
        observables[Index.PLAYER_STATE.ordinal()].notifyObservers(playerState);
      }
    };
    notifiers[Index.HAS_ADMIN.ordinal()] = new Runnable() {
      @Override
      public void run() {
        observables[Index.HAS_ADMIN.ordinal()].notifyObservers(hasAdmin);
      }
    };
    notifiers[Index.MUSIC_APIS.ordinal()] = new Runnable() {
      @Override
      public void run() {
        observables[Index.MUSIC_APIS.ordinal()].notifyObservers(apis);
      }
    };

    futures = new ScheduledFuture[Index.values().length];
    executor = Executors.newScheduledThreadPool(3);
  }

  public static BotState getInstance() {
    return instance;
  }

  public void resetObservers() {
    synchronized (futures) {
      for (Observable observable : observables) {
        observable.deleteObservers();
      }
      for (ScheduledFuture future : futures) {
        if (future != null) {
          future.cancel(true);
        }
      }
    }
  }

  private void scheduleUpdater(final Index index, final Call<?> call) {
    synchronized (futures) {
      Future future = futures[index.ordinal()];
      if (future != null) {
        if (!future.isDone()) {
          return;
        }
      }
      futures[index.ordinal()] = executor.scheduleWithFixedDelay(new Runnable() {
        @Override
        @SuppressWarnings("unchecked")
        public void run() {
          try {
            Response<?> response = call.clone().execute();
            if (response.isSuccessful()) {
              Object body = response.body();
              if (body != null) {
                switch (index) {
                  case PLAYER_STATE:
                    setPlayerState((PlayerState) body);
                    break;
                  case HAS_ADMIN:
                    hasAdmin((Boolean) body);
                    break;
                  case MUSIC_APIS:
                    setMusicApis((List<MusicApi>) body);
                    break;
                }
              }
            }
          } catch (IOException ignore) {
          }
        }
      }, 1, 3, TimeUnit.SECONDS);
    }
  }

  private void cancelUpdater(Index index) {
    synchronized (futures) {
      Future future = futures[index.ordinal()];
      if (future != null) {
        future.cancel(true);
        futures[index.ordinal()] = null;
      }
    }
  }

  private void addObserver(Observer observer, Index index, Object state, Call<?> updateCall) {
    Observable observable = observables[index.ordinal()];
    observable.addObserver(observer);
    observer.update(observable, state);
    scheduleUpdater(index, updateCall);
  }

  private void deleteObserver(Observer observer, Index index) {
    synchronized (futures) {
      Observable observable = observables[index.ordinal()];
      observable.deleteObserver(observer);
      if (observable.countObservers() == 0) {
        cancelUpdater(index);
      }
    }
  }

  /**
   * Add a listener to player state changes. The observer will be called on the UI thread.
   *
   * @param observer the observer
   */
  public void addPlayerStateObserver(@NonNull Observer observer) {
    addObserver(observer, Index.PLAYER_STATE, getPlayerState(), ApiConnector.getService().getPlayerState());
  }

  public synchronized void deletePlayerStateObserver(@NonNull Observer observer) {
    deleteObserver(observer, Index.PLAYER_STATE);
  }

  /**
   * Add a listener to the hasAdmin field. The observer will be called on the UI thread.
   *
   * @param observer the observer
   */
  public void addHasAdminObserver(@NonNull Observer observer) {
    addObserver(observer, Index.HAS_ADMIN, hasAdmin(), ApiConnector.getService().hasAdmin());
  }

  public void deleteHasAdminObserver(@NonNull Observer observer) {
    deleteObserver(observer, Index.HAS_ADMIN);
  }

  /**
   * Add a listener to the list of available music APIs. The observer will be called on the UI thread.
   *
   * @param observer the observer
   */
  public void addMusicApisObserver(@NonNull Observer observer) {
    addObserver(observer, Index.MUSIC_APIS, getMusicApis(), ApiConnector.getService().getMusicApis());
  }

  public void deleteMusicApisObserver(@NonNull Observer observer) {
    deleteObserver(observer, Index.MUSIC_APIS);
  }

  @NonNull
  public PlayerState getPlayerState() {
    return playerState;
  }

  public void setPlayerState(@NonNull PlayerState playerState) {
    int index = Index.PLAYER_STATE.ordinal();
    synchronized (observables[index]) {
      if (!this.playerState.equals(playerState)) {
        this.playerState = playerState;
        handler.post(notifiers[index]);
      }
    }
  }

  public boolean hasAdmin() {
    return hasAdmin;
  }

  public void hasAdmin(boolean hasAdmin) {
    int index = Index.HAS_ADMIN.ordinal();
    synchronized (observables[index]) {
      if (this.hasAdmin != hasAdmin) {
        this.hasAdmin = hasAdmin;
        handler.post(notifiers[index]);
      }
    }
  }

  @NonNull
  public List<MusicApi> getMusicApis() {
    return apis;
  }

  public void setMusicApis(@NonNull List<MusicApi> apis) {
    int index = Index.MUSIC_APIS.ordinal();
    synchronized (observables[index]) {
      if (!this.apis.equals(apis)) {
        this.apis = apis;
        handler.post(notifiers[index]);
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
