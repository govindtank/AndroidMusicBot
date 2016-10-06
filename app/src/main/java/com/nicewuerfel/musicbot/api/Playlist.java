package com.nicewuerfel.musicbot.api;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public final class Playlist implements Parcelable {
  @NonNull
  @SerializedName("playlist_id")
  private final String id;
  @NonNull
  @SerializedName("playlist_name")
  private final String name;


  protected Playlist(Parcel in) {
    id = in.readString();
    name = in.readString();
  }

  @NonNull
  public String getId() {
    return id;
  }

  @NonNull
  public String getName() {
    return name;
  }

  public static final Creator<Playlist> CREATOR = new Creator<Playlist>() {
    @Override
    public Playlist createFromParcel(Parcel in) {
      return new Playlist(in);
    }

    @Override
    public Playlist[] newArray(int size) {
      return new Playlist[size];
    }
  };

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(id);
    dest.writeString(name);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Playlist playlist = (Playlist) o;

    return id.equals(playlist.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  @Override
  public String toString() {
    return name;
  }
}
