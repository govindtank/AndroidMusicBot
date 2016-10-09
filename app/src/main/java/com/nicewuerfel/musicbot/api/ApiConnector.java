package com.nicewuerfel.musicbot.api;


import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.ImageView;

import com.nostra13.universalimageloader.cache.memory.impl.LRULimitedMemoryCache;
import com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.imageaware.ImageAware;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import id.ridsatrio.optio.Optional;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiConnector {

  private static final Retrofit.Builder builder;
  private static SharedPreferences preferences;
  private static String defaultUrl;
  private static String currentUrl;
  private static BotService service;
  private static boolean trustEveryone = false;

  private static LruMemoryCache images = new LruMemoryCache((int) (Runtime.getRuntime().maxMemory() * (15 / 100f)));

  static {
    builder = new Retrofit.Builder().addConverterFactory(GsonConverterFactory.create());
    updateAuth();
  }

  @Nullable
  private static String getToken() {
    if (preferences == null) {
      return null;
    }

    return preferences.getString("bot_token", null);
  }

  private static void updateAuth() {
    OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
    if (trustEveryone) {
      try {
        X509TrustManager manager = new X509TrustManager() {
          @Override
          public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
          }

          @Override
          public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
          }

          @Override
          public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
          }
        };

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{manager}, null);
        SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        httpClient.sslSocketFactory(sslSocketFactory, manager);
        httpClient.hostnameVerifier(new HostnameVerifier() {
          @Override
          public boolean verify(String hostname, SSLSession session) {
            return true;
          }
        });
      } catch (NoSuchAlgorithmException | KeyManagementException e) {
      }
    }

    httpClient.addInterceptor(new Interceptor() {
      @Override
      public Response intercept(Interceptor.Chain chain) throws IOException {
        Request original = chain.request();

        String token = getToken();
        if (token == null) {
          return chain.proceed(original);
        }

        // Request customization: add request headers
        Request.Builder requestBuilder = original.newBuilder()
            .header("Authorization", token)
            .method(original.method(), original.body());

        Request request = requestBuilder.build();
        return chain.proceed(request);
      }
    });
    builder.client(httpClient.build());
  }

  /**
   * Returns a BotService instance.
   *
   * @return a BotService instance
   * @throws IllegalStateException if {@link #getService(SharedPreferences, String)} has never been called
   */
  public static BotService getService() {
    if (preferences == null) {
      throw new IllegalStateException("Not initialized");
    }

    boolean updatedTrust = false;
    boolean trustEveryone = preferences.getBoolean("bot_trust", false);
    if (trustEveryone != ApiConnector.trustEveryone) {
      ApiConnector.trustEveryone = trustEveryone;
      updatedTrust = true;
      updateAuth();
    }

    String url = preferences.getString("bot_url", defaultUrl);
    if (updatedTrust || !url.equals(currentUrl)) {
      currentUrl = url;
      service = builder.baseUrl(url).build().create(BotService.class);
    }

    return service;
  }

  /**
   * Returns the BotService instance. This only needs to be called once.
   *
   * @param sharedPreferences the {@link SharedPreferences} to read the bot url from
   * @param defaultUrl        the default bot url
   * @return a BotService instance
   */
  public static BotService getService(SharedPreferences sharedPreferences, String defaultUrl) {
    if (sharedPreferences == null) {
      throw new NullPointerException("preferences are null");
    }
    if (defaultUrl == null) {
      throw new NullPointerException("default URL is null");
    }
    ApiConnector.preferences = sharedPreferences;
    ApiConnector.defaultUrl = defaultUrl;
    updateAuth();
    return getService();
  }

  /**
   * @return this user
   */
  @NonNull
  public static Optional<ApiUser> getUser() {
    String token = getToken();
    if (token == null) {
      return Optional.empty();
    }

    return Optional.of(ApiUser.fromToken(token));
  }

  /**
   * Whether this user is admin.
   *
   * @return True, if this user is an admin. Defaults to False.
   */
  public static boolean isAdmin() {
    Optional<ApiUser> user = getUser();
    return user.isPresent() && user.get().hasPermission("admin");
  }

  /**
   * Asynchronously tries to retrieve the album art for the given song and displays it in the given view.
   *
   * @param song         the song to load
   * @param albumArtView the view to display the album art in
   */
  public static void displayAlbumArt(@NonNull final Song song, @NonNull final ImageView albumArtView) {
    String albumArtUrl = song.getAlbumArtUrl();
    if (albumArtUrl != null) {
      ImageLoader imageLoader = ImageLoader.getInstance();
      if (!imageLoader.isInited()) {
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(albumArtView.getContext())
            .memoryCache(images)
            .build();
        imageLoader.init(config);
      }
      imageLoader.displayImage(albumArtUrl, albumArtView);
    } else {
      Bitmap cacheImage = images.get(song.getSongId());
      if (cacheImage == null) {
        ApiConnector.getService().getAlbumArt(song.getSongId()).enqueue(new DummyCallback<ResponseBody>() {
          @Override
          public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
            if (response.isSuccessful()) {
              ResponseBody body = response.body();
              if (body != null) {
                Bitmap bitmap = BitmapFactory.decodeStream(response.body().byteStream());
                images.put(song.getSongId(), bitmap);
                albumArtView.setImageBitmap(bitmap);
              }
            }
          }
        });
      } else {
        albumArtView.setImageBitmap(cacheImage);
      }
    }
  }
}

