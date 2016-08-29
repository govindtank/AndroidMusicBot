package com.nicewuerfel.musicbot.api;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import retrofit2.Response;

public class SongContentProvider extends ContentProvider {

  public static final String AUTHORITY = "com.nicewuerfel.musicbot.api.song_content_provider";
  public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/search");

  private static final int SEARCH_SUGGEST = 1;

  private static final UriMatcher uriMatcher;

  static {
    uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    uriMatcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGEST);
    uriMatcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_SUGGEST);
  }

  private static MusicApi api = null;

  public SongContentProvider() {
  }

  public static MusicApi getApi() {
    return api;
  }

  public static void setApi(MusicApi api) {
    SongContentProvider.api = api;
  }

  public static List<Song> songs = Collections.emptyList();

  @Override
  public boolean onCreate() {
    return true;
  }

  @Nullable
  @Override
  public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
    if (api == null) {
      return null;
    }
    switch (uriMatcher.match(uri)) {
      case SEARCH_SUGGEST:
        String query = uri.getLastPathSegment().toLowerCase();
        Response<List<Song>> response;
        try {
          if (query.equals(SearchManager.SUGGEST_URI_PATH_QUERY)) {
            if (!api.isSongProvider()) {
              return null;
            }
            response = ApiConnector.getService().getSuggestions(api.getApiName()).execute();
          } else {
            response = ApiConnector.getService().searchSong(api.getApiName(), query).execute();
          }
        } catch (IOException e) {
          return null;
        }
        return processResponse(response);
      default:
        throw new IllegalArgumentException("Unknown URI: " + uri);
    }
  }

  /**
   * Processes a response, sets the static songs field and returns the song list.
   *
   * @param response the response
   * @return a cursor or null
   */
  private Cursor processResponse(Response<List<Song>> response) {
    if (response.isSuccessful()) {
      List<Song> list = response.body();
      if (list == null) {
        return null;
      }

      MatrixCursor cursor = new MatrixCursor(Song.columns, list.size());
      int i = 0;
      for (Song song : list) {
        cursor.addRow(song.toColumnValues(i++));
      }
      songs = Collections.unmodifiableList(list);
      return cursor;
    } else {
      return null;
    }
  }

  @Nullable
  @Override
  public String getType(Uri uri) {
    switch (uriMatcher.match(uri)) {
      case SEARCH_SUGGEST:
        return SearchManager.SUGGEST_MIME_TYPE;
      default:
        throw new IllegalArgumentException("Unknown URL " + uri);
    }
  }

  @Nullable
  @Override
  public Uri insert(Uri uri, ContentValues contentValues) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int delete(Uri uri, String s, String[] strings) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
    throw new UnsupportedOperationException();
  }
}

