package com.nicewuerfel.musicbot.api;

import android.support.annotation.Nullable;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Query;

/**
 * The return JavaDocs for all methods describe what the call will return on success, not the call actually returned.
 */
public interface BotService {

  /**
   * Log in to the service.
   *
   * @param user the {@link User}
   * @return a token for further requests
   */
  @PUT("login")
  Call<String> login(@Body User user);

  /**
   * Registers a new user on the service.
   *
   * @param user the {@link User}
   * @return a token for further requests
   */
  @POST("register")
  Call<String> register(@Body User user);


  /**
   * Retrieves all available music APIs.
   *
   * @return the music APIs supported by the server
   */
  @GET("music_apis")
  Call<List<MusicApi>> getMusicApis();

  /**
   * Searches for a song.
   *
   * @param apiName the music API to search with
   * @param query   the search query
   * @return the search results
   */
  @GET("search")
  Call<List<Song>> searchSong(@Query("api_name") String apiName, @Query("query") String query/*, @Query("max_fetch") int maxFetch*/);

  /**
   * Returns a list of suggestions to play next.
   *
   * @param apiName the music API to search with
   * @return the suggested songs
   */
  @GET("suggestions")
  Call<List<Song>> getSuggestions(@Query("api_name") String apiName);

  /**
   * Returns the current state of the player.
   *
   * @return the player state
   */
  @GET("player_state")
  Call<PlayerState> getPlayerState();

  /**
   * Adds a song to the queue.
   *
   * @param song the song to add
   * @return an undefined string response
   */
  @PUT("queue")
  Call<String> queue(@Body Song song);

  /**
   * Removes a song from the queue.
   *
   * @param song the song to remove
   * @return an undefined string response
   */
  @PUT("queue?remove=true")
  Call<String> dequeue(@Body Song song);

  /**
   * Pause or resume the current playback
   *
   * @return the new player state
   */
  @PUT("toggle_pause")
  Call<PlayerState> togglePause();

  /**
   * Move to the next song in the queue.
   *
   * @return the new player state
   */
  @PUT("next_song")
  Call<PlayerState> nextSong();

  /**
   * Moves a song already in the queue somewhere else in the queue.
   *
   * @param moveRequestBody a {@link MoveRequestBody} instance
   * @param after           if not null, <i>toMove</i> will be moved after the <i>other</i> song
   * @return the new player state
   */
  @PUT("move")
  Call<PlayerState> moveSong(@Body MoveRequestBody moveRequestBody, @Nullable @Query("after_other") Object after);
}
