package com.nicewuerfel.musicbot.api;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public abstract class FinishableCallback<T> implements Callback<T> {
  private boolean isFinished = false;

  public boolean isFinished() {
    return isFinished;
  }

  @Override
  public final void onResponse(Call<T> call, Response<T> response) {
    isFinished = true;
    onCallResponse(call, response);
  }

  /**
   * @see Callback#onResponse(Call, Response)
   */
  public abstract void onCallResponse(Call<T> call, Response<T> response);

  @Override
  public final void onFailure(Call<T> call, Throwable t) {
    isFinished = true;
    onCallFailure(call, t);
  }

  /**
   * @see Callback#onFailure(Call, Throwable)
   */
  public abstract void onCallFailure(Call<T> call, Throwable t);
}
