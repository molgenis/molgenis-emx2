package org.molgenis.emx2;

import java.util.Locale;

public record Username(String value) {

  public Username(String value) {
    this.value = normalize(value);
  }

  private static String normalize(String value) {
    if (value == null) {
      throw new MolgenisException("Username cannot be null");
    }

    String newValue = value.trim().toLowerCase(Locale.ROOT);

    if (newValue.isBlank()) {
      throw new MolgenisException("Username cannot be blank");
    }

    return newValue;
  }

  @Override
  public String toString() {
    return value;
  }
}
