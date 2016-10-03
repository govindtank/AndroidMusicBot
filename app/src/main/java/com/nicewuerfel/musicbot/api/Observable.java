package com.nicewuerfel.musicbot.api;

/**
 * An Observable subclass which always returns true for hasChanged.
 */
public final class Observable extends java.util.Observable {

  /**
   * @return true
   */
  @Override
  public boolean hasChanged() {
    return true;
  }
}
