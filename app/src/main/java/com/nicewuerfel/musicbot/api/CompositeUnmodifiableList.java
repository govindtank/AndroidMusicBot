package com.nicewuerfel.musicbot.api;

import java.util.AbstractList;
import java.util.List;

final class CompositeUnmodifiableList<E> extends AbstractList<E> {

  private final List<E> list1;
  private final List<E> list2;

  public CompositeUnmodifiableList(List<E> list1, List<E> list2) {
    this.list1 = list1;
    this.list2 = list2;
  }

  @Override
  public E get(int location) {
    if (location < list1.size()) {
      return list1.get(location);
    } else {
      return list2.get(location - list1.size());
    }
  }

  @Override
  public int size() {
    return list1.size() + list2.size();
  }
}
