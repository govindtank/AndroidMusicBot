package com.nicewuerfel.musicbot.api;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DummyCallback<T> implements Callback<T> {
  @Override
  public void onResponse(Call<T> call, Response<T> response) {
  }

  @Override
  public void onFailure(Call<T> call, Throwable t) {
  }
}
