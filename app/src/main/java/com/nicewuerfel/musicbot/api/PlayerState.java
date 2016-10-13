package com.nicewuerfel.musicbot.api;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Collections;
import java.util.List;

public final class PlayerState implements Parcelable {

  public static final PlayerState EMPTY = new PlayerState();

  @Nullable
  private final Song current_song;
  @NonNull
  private final List<Song> last_played;
  @NonNull
  private final List<Song> queue;
  private final boolean paused;

  private PlayerState() {
    this.current_song = Song.UNKNOWN;
    this.last_played = Collections.<Song>emptyList();
    this.queue = Collections.<Song>emptyList();
    this.paused = false;
  }

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

  @NonNull
  public Song getCurrentSong() {
    return current_song == null ? Song.UNKNOWN : current_song;
  }

  @NonNull
  public List<Song> getLastPlayed() {
    return last_played;
  }

  @NonNull
  public List<Song> getQueue() {
    return queue;
  }

  @NonNull
  public List<Song> getCombinedLastPlayedAndQueue() {
    return new CompositeUnmodifiableList<>(new LastPlayedList(last_played), queue);
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    PlayerState that = (PlayerState) o;

    if (paused != that.paused) return false;
    if (current_song != null ? !current_song.equals(that.current_song) : that.current_song != null)
      return false;
    if (!last_played.equals(that.last_played)) return false;
    return queue.equals(that.queue);
  }

  @Override
  public int hashCode() {
    int result = current_song != null ? current_song.hashCode() : 0;
    result = 31 * result + last_played.hashCode();
    result = 31 * result + queue.hashCode();
    result = 31 * result + (paused ? 1 : 0);
    return result;
  }

  @Override
  public String toString() {
    return "PlayerState{" +
        "current_song=" + current_song +
        ", last_played=" + last_played +
        ", queue=" + queue +
        ", paused=" + paused +
        '}';
  }
}
