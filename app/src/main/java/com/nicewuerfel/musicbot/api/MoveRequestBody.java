package com.nicewuerfel.musicbot.api;


public class MoveRequestBody {

  private final Song moving_song_json;
  private final Song other_song_json;

  public MoveRequestBody(Song toMove, Song otherSong) {
    moving_song_json = toMove;
    other_song_json = otherSong;
  }
}
