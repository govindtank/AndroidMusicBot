package com.nicewuerfel.musicbot.api;


import android.app.SearchManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Song implements Parcelable {

  private String song_id;
  private String api_name;
  private String title;
  private String description;
  private String albumArtUrl;
  private String str_rep;
  private String duration;

  private Song() {
  }

  @NonNull
  public String getSongId() {
    return song_id;
  }

  @NonNull
  public String getApiName() {
    return api_name;
  }

  /**
   * Returns the title of the song. May be "unknown".
   *
   * @return the song title
   */
  @NonNull
  public String getTitle() {
    return title;
  }

  @Nullable
  public String getDescription() {
    return description;
  }

  @Nullable
  public String getAlbumArtUrl() {
    return albumArtUrl;
  }

  @NonNull
  public String getStringRepresentation() {
    return str_rep;
  }

  @Nullable
  public String getDuration() {
    return duration;
  }

  public String toString() {
    return getStringRepresentation();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Song song = (Song) o;

    return song_id.equals(song.song_id);
  }

  @Override
  public int hashCode() {
    return song_id.hashCode();
  }

  protected Song(Parcel in) {
    song_id = in.readString();
    api_name = in.readString();
    title = in.readString();
    description = in.readString();
    albumArtUrl = in.readString();
    str_rep = in.readString();
    duration = in.readString();
  }

  public static final Creator<Song> CREATOR = new Creator<Song>() {
    @Override
    public Song createFromParcel(Parcel in) {
      return new Song(in);
    }

    @Override
    public Song[] newArray(int size) {
      return new Song[size];
    }
  };

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel parcel, int i) {
    parcel.writeString(song_id);
    parcel.writeString(api_name);
    parcel.writeString(title);
    parcel.writeString(description);
    parcel.writeString(albumArtUrl);
    parcel.writeString(str_rep);
    parcel.writeString(duration);
  }

  public Object[] toColumnValues(int id) {
    String title = getTitle();
    if (duration != null) {
      title += " (" + duration + ")";
    }
    String description = getDescription();
    Object[] result = {
        id, title, description
    };
    return result;
  }

  static final String[] columns = {
      BaseColumns._ID,
      SearchManager.SUGGEST_COLUMN_TEXT_1,
      SearchManager.SUGGEST_COLUMN_TEXT_2
  };
  static final Map<String, Integer> columnIndices;

  static {
    Map<String, Integer> indices = new HashMap<>(16);
    for (int i = 0; i < columns.length; i++) {
      indices.put(columns[i], i);
    }
    columnIndices = Collections.unmodifiableMap(indices);
  }
}
