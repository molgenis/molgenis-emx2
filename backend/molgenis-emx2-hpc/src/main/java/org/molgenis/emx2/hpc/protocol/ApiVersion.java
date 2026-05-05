package org.molgenis.emx2.hpc.protocol;

/** Protocol version constant and validation. */
public final class ApiVersion {

  public static final String CURRENT = "2025-01";
  public static final String HEADER_NAME = "X-EMX2-API-Version";

  private ApiVersion() {}

  /**
   * Validates that the provided version string matches the current protocol version.
   *
   * @throws IllegalArgumentException if the version is null or doesn't match
   */
  public static void validate(String version) {
    if (version == null || version.isBlank()) {
      throw new IllegalArgumentException(
          "Missing required header " + HEADER_NAME + "; expected " + CURRENT);
    }
    if (!CURRENT.equals(version)) {
      throw new IllegalArgumentException(
          "Unsupported API version '"
              + version
              + "'; this server supports "
              + CURRENT
              + ". Update your daemon.");
    }
  }
}
