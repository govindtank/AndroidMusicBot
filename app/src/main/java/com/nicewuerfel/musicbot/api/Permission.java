package com.nicewuerfel.musicbot.api;

import android.os.Parcel;
import android.os.Parcelable;

public class Permission implements Parcelable {
  private final String name;
  private final String description;

  public Permission(String name, String description) {
    this.name = name;
    this.description = description;
  }

  protected Permission(Parcel in) {
    name = in.readString();
    description = in.readString();
  }

  public static final Creator<Permission> CREATOR = new Creator<Permission>() {
    @Override
    public Permission createFromParcel(Parcel in) {
      return new Permission(in);
    }

    @Override
    public Permission[] newArray(int size) {
      return new Permission[size];
    }
  };

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(name);
    dest.writeString(description);
  }
}
