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
   * Needs admin, mod or queue_remove permission.
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

  /**
   * Checks whether there is an admin registered on the server.
   *
   * @return whether there is the admin
   */
  @GET("has_admin")
  Call<Boolean> hasAdmin();

  /**
   * Checks whether this user is admin.
   *
   * @return whether this user is the admin
   */
  @GET("is_admin")
  Call<Boolean> isAdmin();

  /**
   * Checks whether this user has one of the given permissions.
   *
   * @param neededPermissions the permissions to check for
   * @return whether the users permissions aren't disjoint with the given permissions
   */
  @GET("has_permission")
  Call<Boolean> hasPermission(@Body List<String> neededPermissions);

  /**
   * Returns the own permissions.
   *
   * @return a list of permissions
   */
  @GET("get_permissions")
  Call<List<String>> getPermissions();

  /**
   * Claim admin rights on the server.
   * There can only be one admin.
   * Fails if this user is already admin.
   *
   * @return an admin token
   */
  @GET("claim_admin")
  Call<String> claimAdmin();

  /**
   * Returns a list of available permissions a user can be granted.
   * Needs admin permission.
   *
   * @return a list of Permission objects.
   */
  @GET("get_available_permissions")
  Call<List<Permission>> getAvailablePermissions();

  /**
   * Returns a list of users of the API.
   * Needs admin permission.
   *
   * @return a list of ApiUser objects.
   */
  @GET("get_users")
  Call<List<ApiUser>> getUsers();


  /**
   * Grants a permission to a user.
   * Needs admin permission.
   * Takes effect after user logs out and in again.
   *
   * @param username   the users username
   * @param permission the permission to grant
   * @return 'OK' on success
   */
  @PUT("grant_permission")
  Call<String> grantPermission(String username, Permission permission);

  /**
   * Revokes a permission from a user.
   * Needs admin permission.
   * Takes effect after user logs out and in again.
   *
   * @param username   the users username
   * @param permission the permission to revoke
   * @return 'OK' on success
   */
  @PUT("revoke_permission")
  Call<String> revokePermission(String username, Permission permission);

  @PUT("exit_bot")
  Call<String> exitBot();

  @PUT("reset_bot")
  Call<String> resetBot();

  @GET("get_available_offline_playlists")
  Call<List<Playlist>> getAvailablePlaylists();

  @GET("get_active_playlist")
  Call<Playlist> getActivePlaylist();

  @PUT("mark_active")
  Call<String> setActivePlaylist(@Query("playlist_id") String playlistId);
}
