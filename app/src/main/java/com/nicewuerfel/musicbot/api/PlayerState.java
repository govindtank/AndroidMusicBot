package com.nicewuerfel.musicbot.api;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import java.util.List;

public class PlayerState implements Parcelable {

  private Song current_song;
  private List<Song> last_played;
  private List<Song> queue;
  private boolean paused;

  protected PlayerState(Parcel in) {
    current_song = in.readParcelable(Song.class.getClassLoader());
    last_played = in.createTypedArrayList(Song.CREATOR);
    queue = in.createTypedArrayList(Song.CREATOR);
    paused = in.readByte() != 0;
  }

  public static final Creator<PlayerState> CREATOR = new Creator<PlayerState>() {
    @Override
    public PlayerState createFromParcel(Parcel in) {
      return new PlayerState(in);
    }

    @Override
    public PlayerState[] newArray(int size) {
      return new PlayerState[size];
    }
  };

  @Nullable
  public Song getCurrentSong() {
    return current_song;
  }

  public List<Song> getLastPlayed() {
    return last_played;
  }

  public List<Song> getQueue() {
    return queue;
  }

  public boolean isPaused() {
    return paused;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel parcel, int i) {
    parcel.writeParcelable(current_song, i);
    parcel.writeTypedList(last_played);
    parcel.writeTypedList(queue);
    parcel.writeByte((byte) (paused ? 1 : 0));
  }
}
