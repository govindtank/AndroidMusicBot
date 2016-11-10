package com.nicewuerfel.musicbot.api;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.view.View;
import android.widget.ImageView;

import com.squareup.picasso.LruCache;

import okhttp3.ResponseBody;
import retrofit2.Call;

public final class AlbumArtLoader {

  private static AlbumArtLoader instance = null;

  @NonNull
  private final LruCache images;

  private AlbumArtLoader() {
    images = new LruCache((int) (Runtime.getRuntime().maxMemory() * (8 / 100f)));
  }

  private static synchronized void createInstance() {
    if (instance == null) {
      instance = new AlbumArtLoader();
    }
  }

  @NonNull
  public static AlbumArtLoader getInstance() {
    if (instance == null) {
      createInstance();
    }
    return instance;
  }

  @UiThread
  public void load(@NonNull final Song song, @NonNull final ImageLoadingListener loadingListener) {
    if (song.equals(Song.UNKNOWN)) {
      loadingListener.onLoadingComplete(song, null);
    }

    final String albumArtUrl = song.getAlbumArtUrl();
    if (albumArtUrl != null) {
      throw new IllegalArgumentException("song has an URL");
    } else {
      Bitmap cacheImage = images.get(song.getSongId());
      if (cacheImage != null) {
        loadingListener.onLoadingComplete(song, cacheImage);
        return;
      }

      ApiConnector.getService().getAlbumArt(song.getSongId()).enqueue(new retrofit2.Callback<ResponseBody>() {
        @Override
        public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
          if (!response.isSuccessful()) {
            fail();
          }
          ResponseBody body = response.body();
          if (body != null) {
            Bitmap bitmap = BitmapFactory.decodeStream(response.body().byteStream());
            images.set(song.getSongId(), bitmap);
            loadingListener.onLoadingComplete(song, bitmap);
          }
        }

        @Override
        public void onFailure(Call<ResponseBody> call, Throwable t) {
          fail();
        }

        private void fail() {
          loadingListener.onLoadingComplete(song, null);
        }
      });
    }
  }

  /**
   * Asynchronously tries to retrieve the album art for the given song and displays it in the given view.
   *
   * @param song       the song to load
   * @param imageView  the view to display the album art in
   * @param hideOnFail whether to set visibility of the view to GONE if no image can be loaded
   */
  @UiThread
  public void display(@NonNull Song song, @NonNull final ImageView imageView, boolean hideOnFail) {
    if (hideOnFail) {
      imageView.setVisibility(View.GONE);
    }
    load(song, new ImageLoadingListener() {
      @Override
      public void onLoadingComplete(@NonNull Song song, @Nullable Bitmap bitmap) {
        if (bitmap == null) {
          return;
        }

        imageView.setVisibility(View.VISIBLE);
        imageView.setImageBitmap(bitmap);
      }
    });
  }

  public interface ImageLoadingListener {
    void onLoadingComplete(@NonNull Song song, @Nullable Bitmap bitmap);
  }
}
