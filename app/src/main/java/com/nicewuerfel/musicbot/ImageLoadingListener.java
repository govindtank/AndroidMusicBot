package com.nicewuerfel.musicbot;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.nicewuerfel.musicbot.api.Song;

public interface ImageLoadingListener {
  void onLoadingComplete(@NonNull Song song, @Nullable Bitmap bitmap);
}
