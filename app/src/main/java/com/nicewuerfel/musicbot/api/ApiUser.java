package com.nicewuerfel.musicbot.api;


import android.os.Parcel;
import android.os.Parcelable;

import java.util.Collections;
import java.util.List;

public class ApiUser implements Parcelable {
  private final String username;
  private final List<String> permissions;

  public ApiUser(String username, List<String> permissions) {
    this.username = username;
    this.permissions = permissions;
  }

  protected ApiUser(Parcel in) {
    username = in.readString();
    permissions = in.createStringArrayList();
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

  public String getUsername() {
    return username;
  }

  public List<String> getPermissions() {
    return Collections.unmodifiableList(permissions);
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
