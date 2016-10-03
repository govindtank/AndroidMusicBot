package com.nicewuerfel.musicbot.api;


import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.proc.JWTClaimsSetVerifier;

import java.text.ParseException;
import java.util.Collections;
import java.util.List;

public class ApiUser implements Parcelable {
  @NonNull
  private final String username;
  @NonNull
  private final List<String> permissions;

  public ApiUser(@NonNull String username, @NonNull List<String> permissions) {
    if (username == null) {
      throw new NullPointerException("username is null");
    }
    this.username = username;
    if (permissions == null) {
      throw new NullPointerException("permissions is null");
    }
    this.permissions = permissions;
  }

  protected ApiUser(Parcel in) {
    username = in.readString();
    permissions = in.createStringArrayList();
  }

  @NonNull
  public static ApiUser fromToken(@NonNull String token) {
    try {
      JWT jwt = JWTParser.parse(token);
      JWTClaimsSet claims = jwt.getJWTClaimsSet();
      String username = (String) claims.getClaim("name");
      @SuppressWarnings("unchecked")
      List<String> permissions = (List<String>) claims.getClaim("permissions");
      return new ApiUser(username, permissions);
    } catch (ParseException | ClassCastException e) {
      throw new IllegalArgumentException("Malformed token", e);
    }
  }

  public static final Creator<ApiUser> CREATOR = new Creator<ApiUser>() {
    @Override
    public ApiUser createFromParcel(Parcel in) {
      return new ApiUser(in);
    }

    @Override
    public ApiUser[] newArray(int size) {
      return new ApiUser[size];
    }
  };

  @NonNull
  public String getUsername() {
    return username;
  }

  @NonNull
  public List<String> getPermissions() {
    return Collections.unmodifiableList(permissions);
  }

  public boolean hasPermission(@NonNull String permission) {
    switch (permission) {
      case "admin":
        return permissions.contains("admin");
      case "exit":
      case "reset":
        return permissions.contains(permission) || permissions.contains("admin");
      default:
        return permissions.contains(permission) || permissions.contains("admin") || permissions.contains("mod");
    }
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(username);
    dest.writeStringList(permissions);
  }
}
