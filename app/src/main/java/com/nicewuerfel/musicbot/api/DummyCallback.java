package com.nicewuerfel.musicbot.api;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DummyCallback<T> extends FinishableCallback<T> {
  @Override
  public void onCallResponse(Call<T> call, Response<T> response) {
  }

  @Override
  public void onCallFailure(Call<T> call, Throwable t) {
  }
}
