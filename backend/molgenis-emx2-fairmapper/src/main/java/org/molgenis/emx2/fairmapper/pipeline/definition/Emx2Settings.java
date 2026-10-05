package org.molgenis.emx2.fairmapper.pipeline.definition;

import org.molgenis.emx2.MolgenisException;

/**
 * The EMX2 instance a harvest reads schema metadata from and uploads to.
 *
 * <pre>
 * emx2:
 *   endpoint: https://emx2.example.org
 *   token: my-token
 * </pre>
 *
 * @param endpoint base URL of the EMX2 instance
 * @param token authentication token for the EMX2 instance
 */
public record Emx2Settings(String endpoint, String token) {

  void validate() {
    if (endpoint == null || token == null) {
      throw new MolgenisException("Invalid pipeline file: emx2 requires endpoint and token");
    }
  }
}
