package com.nicewuerfel.musicbot.api;


import android.content.SharedPreferences;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Objects;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiConnector {

  private static final Retrofit.Builder builder;
  private static SharedPreferences preferences;
  private static String defaultUrl;
  private static String currentUrl;
  private static BotService service;
  private static String token = null;
  private static boolean trustEveryone = false;

  static {
    builder = new Retrofit.Builder().addConverterFactory(GsonConverterFactory.create());
    updateAuth();
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

    boolean updatedPreference = false;
    String token = preferences.getString("bot_token", null);
    if (token != null) {
      if (!token.equals(ApiConnector.token)) {
        ApiConnector.token = token;
        updatedPreference = true;
      }
    }

    boolean trustEveryone = preferences.getBoolean("bot_trust", false);
    if (trustEveryone != ApiConnector.trustEveryone) {
      ApiConnector.trustEveryone = trustEveryone;
      updateAuth();
      updatedPreference = true;
    }


    String url = preferences.getString("bot_url", defaultUrl);
    if (updatedPreference || !url.equals(currentUrl)) {
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
    ApiConnector.preferences = Objects.requireNonNull(sharedPreferences);
    ApiConnector.defaultUrl = Objects.requireNonNull(defaultUrl);
    updateAuth();
    return getService();
  }
}

