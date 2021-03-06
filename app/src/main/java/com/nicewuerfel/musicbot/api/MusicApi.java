package com.nicewuerfel.musicbot.api;

import android.os.Parcel;
import android.os.Parcelable;

public final class MusicApi implements Parcelable {

  private String api_name;
  private String api_pretty_name;
  private boolean is_song_provider;

  protected MusicApi(Parcel in) {
    api_name = in.readString();
    api_pretty_name = in.readString();
    is_song_provider = in.readByte() != 0;
  }

  public static final Creator<MusicApi> CREATOR = new Creator<MusicApi>() {
    @Override
    public MusicApi createFromParcel(Parcel in) {
      return new MusicApi(in);
    }

    @Override
    public MusicApi[] newArray(int size) {
      return new MusicApi[size];
    }
  };

  public String getName() {
    return api_name;
  }

  public String getPrettyName() {
    return api_pretty_name;
  }

  public boolean isSongProvider() {
    return is_song_provider;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel parcel, int i) {
    parcel.writeString(api_name);
    parcel.writeString(api_pretty_name);
    parcel.writeByte((byte) (is_song_provider ? 1 : 0));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    MusicApi musicApi = (MusicApi) o;

    return api_name.equals(musicApi.api_name);

  }

  @Override
  public int hashCode() {
    return api_name.hashCode();
  }

  @Override
  public String toString() {
    return "MusicApi{" +
        "api_name='" + api_name + '\'' +
        ", api_pretty_name='" + api_pretty_name + '\'' +
        ", is_song_provider=" + is_song_provider +
        '}';
  }
}
