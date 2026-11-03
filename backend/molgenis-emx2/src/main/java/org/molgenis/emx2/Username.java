package org.molgenis.emx2;

public record Username(String value) {

  public Username(String value) {
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
  public String toString() {
    return value;
  }
}
