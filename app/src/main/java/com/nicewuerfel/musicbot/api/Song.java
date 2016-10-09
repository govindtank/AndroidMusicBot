package com.nicewuerfel.musicbot.api;


import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

public final class Song implements Parcelable {

  @SerializedName("song_id")
  private final String songId;
  @SerializedName("api_name")
  private final String apiName;
  private final String title;
  private final String description;
  private final String albumArtUrl;
  @SerializedName("str_rep")
  private final String stringRep;
  private final String duration;
  @SerializedName("user")
  private final String username;

  private transient boolean isLastPlayed = false;

  protected Song(Parcel in) {
    songId = in.readString();
    apiName = in.readString();
    title = in.readString();
    description = in.readString();
    albumArtUrl = in.readString();
    stringRep = in.readString();
    duration = in.readString();
    username = in.readString();
    isLastPlayed = in.readByte() != 0;
  }

  @NonNull
  public String getSongId() {
    return songId;
  }

  @NonNull
  public String getApiName() {
    return apiName;
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
    return stringRep;
  }

  @Nullable
  public String getDuration() {
    return duration;
  }

  /**
   * Get the username of the user who queued this song.
   *
   * @return the username
   */
  @Nullable
  public String getUsername() {
    return username;
  }

  public boolean isLastPlayed() {
    return isLastPlayed;
  }

  void isLastPlayed(boolean isLastPlayed) {
    this.isLastPlayed = isLastPlayed;
  }

  public String toString() {
    return getStringRepresentation();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Song song = (Song) o;

    return songId.equals(song.songId);
  }

  @Override
  public int hashCode() {
    return songId.hashCode();
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
    parcel.writeString(songId);
    parcel.writeString(apiName);
    parcel.writeString(title);
    parcel.writeString(description);
    parcel.writeString(albumArtUrl);
    parcel.writeString(stringRep);
    parcel.writeString(duration);
    parcel.writeString(username);
    parcel.writeByte((byte) (isLastPlayed ? 1 : 0));
  }
}
