package org.molgenis.emx2;

import java.util.Objects;

public class Username {

  private String value;

  public Username(String value) {
    this.value = normalize(value);
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = normalize(value);
  }

  private static String normalize(String value) {
    if (value == null) {
      throw new MolgenisException("Username cannot be null");
    }

    String newValue = value.trim().toLowerCase();

    if (newValue.isBlank()) {
      throw new MolgenisException("Username cannot be blank");
    }

    return newValue;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;

    Username that = (Username) o;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(value);
  }

  @Override
  public String toString() {
    return "CaseInsensitiveString{" + "value='" + value + '\'' + '}';
  }
}
