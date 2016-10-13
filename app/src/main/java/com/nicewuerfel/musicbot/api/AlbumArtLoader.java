package com.nicewuerfel.musicbot.api;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.view.View;
import android.widget.ImageView;

import com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import net.jcip.annotations.ThreadSafe;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.ResponseBody;
import retrofit2.Call;

public final class AlbumArtLoader {

  private static AlbumArtLoader instance = null;

  @NonNull
  private final LruMemoryCache images;
  @NonNull
  private final ImageLoader imageLoader;
  @NonNull
  private final Map<Song, MultiCallback> callbacks;

  private AlbumArtLoader(@NonNull Context context) {
    images = new LruMemoryCache((int) (Runtime.getRuntime().maxMemory() * (15 / 100f)));
    imageLoader = ImageLoader.getInstance();
    if (!imageLoader.isInited()) {
      ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(context)
          .memoryCache(images)
          .build();
      imageLoader.init(config);
    }
    callbacks = new HashMap<>(64);
  }

  private static synchronized void createInstance(@NonNull Context context) {
    if (instance == null) {
      instance = new AlbumArtLoader(context);
    }
  }

  @NonNull
  public static AlbumArtLoader getInstance(@NonNull Context context) {
    if (instance == null) {
      createInstance(context);
    }
    return instance;
  }

  private com.nostra13.universalimageloader.core.listener.ImageLoadingListener addListener(@NonNull Song song, @NonNull ImageLoadingListener loadingListener) {
    synchronized (callbacks) {
      MultiCallback multiCallback = callbacks.get(song);
      if (multiCallback == null) {
        multiCallback = new MultiCallback(song);
        callbacks.put(song, multiCallback);
      }

      multiCallback.addListener(loadingListener);
      return multiCallback;
    }
  }

  @UiThread
  public void load(@NonNull final Song song, @NonNull final ImageLoadingListener loadingListener) {
    if (song.equals(Song.UNKNOWN)) {
      loadingListener.onLoadingComplete(song, null);
    }

    final String albumArtUrl = song.getAlbumArtUrl();
    if (albumArtUrl != null) {
      imageLoader.loadImage(albumArtUrl, addListener(song, loadingListener));
    } else {
      Bitmap cacheImage = images.get(song.getSongId());
      if (cacheImage != null) {
        loadingListener.onLoadingComplete(song, cacheImage);
        return;
      }

      ApiConnector.getService().getAlbumArt(song.getSongId()).enqueue(new DummyCallback<ResponseBody>() {
        @Override
        public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
          if (response.isSuccessful()) {
            ResponseBody body = response.body();
            if (body != null) {
              Bitmap bitmap = BitmapFactory.decodeStream(response.body().byteStream());
              images.put(song.getSongId(), bitmap);
              loadingListener.onLoadingComplete(song, bitmap);
              return;
            }
          }
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

  @ThreadSafe
  private class MultiCallback extends SimpleImageLoadingListener {

    @NonNull
    private final Song song;
    @NonNull
    private final Set<ImageLoadingListener> listeners;

    private MultiCallback(@NonNull Song song) {
      this.song = song;
      listeners = Collections.newSetFromMap(new ConcurrentHashMap<ImageLoadingListener, Boolean>(8));
    }

    private void addListener(@NonNull ImageLoadingListener loadingListener) {
      listeners.add(loadingListener);
    }

    private void onResult(@Nullable Bitmap bitmap) {
      synchronized (callbacks) {
        callbacks.remove(song);
        for (ImageLoadingListener listener : listeners) {
          listener.onLoadingComplete(song, bitmap);
        }
      }
    }

    @Override
    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
      onResult(loadedImage);
    }

    @Override
    public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
      onResult(null);
    }
  }
}
