package org.molgenis.emx2.utils.generator;

public record AutoIdConfig(Format format, int length) {

  public enum Format {
    LETTERS("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"),
    NUMBERS("0123456789"),
    MIXED(LETTERS.getCharacters() + NUMBERS.getCharacters());

    private final String characters;

    Format(String characters) {
      this.characters = characters;
    }

    public String getCharacters() {
      return characters;
    }
  }
}
