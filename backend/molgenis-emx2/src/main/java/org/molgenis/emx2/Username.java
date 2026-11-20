package org.molgenis.emx2;

public record Username(String value) {

  public Username(String value) {
    this.value = normalize(value);
  }

  private static String normalize(String value) {
    if (value == null) {
      throw new MolgenisException("Username cannot be null");
    }

    if (value.isBlank()) {
      throw new MolgenisException("Username cannot be blank");
    }

    return value.trim();
  }

  @Override
  public String toString() {
    return value;
  }
}
