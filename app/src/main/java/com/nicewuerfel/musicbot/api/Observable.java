package com.nicewuerfel.musicbot.api;

public class Observable extends java.util.Observable {

  @Override
  public void notifyObservers() {
    setChanged();
    super.notifyObservers();
  }

  @Override
  public void notifyObservers(Object data) {
    setChanged();
    super.notifyObservers(data);
  }
}
