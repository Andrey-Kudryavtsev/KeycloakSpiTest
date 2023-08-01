package ru.intabia.kkst;

public enum AuthSource {
  LOGIN(1),
  PASSPORT(2),
  PHONE(4);

  private final int value;

  AuthSource(int value) {
    this.value = value;
  }

  public String getValue() {
    return String.valueOf(value);
  }
}
