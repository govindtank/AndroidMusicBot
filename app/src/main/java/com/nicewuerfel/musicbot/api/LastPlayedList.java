package com.nicewuerfel.musicbot.api;


import java.util.AbstractList;
import java.util.List;

final class LastPlayedList extends AbstractList<Song> {

  private final List<Song> songs;

  public LastPlayedList(List<Song> songs) {
    this.songs = songs;
  }

  @Override
  public Song get(int location) {
    Song song = songs.get(location);
    song.isLastPlayed(true);
    return song;
  }

  @Override
  public int size() {
    return songs.size();
  }
}
