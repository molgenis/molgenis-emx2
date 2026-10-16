package org.molgenis.emx2.rdf.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.molgenis.emx2.rdf.generators.RdfApiGeneratorFactory;
import org.molgenis.emx2.rdf.writers.OutputStreamWriterFactory;

/**
 * Contains relevant config data for the RDF module. Use {@link RdfConfigReader} to create object
 * with {@link org.molgenis.emx2.Schema} specific values. Use {@link #getDefaults()} to retrieve a
 * singleton containing the default values.
 */
public final class RdfConfig {
  private static final RdfConfig DEFAULT_INSTANCE = new RdfConfig();

  @JsonProperty("writer")
  @SuppressWarnings("PMD.FinalFieldCouldBeStatic") // set by Jackson
  private final OutputStreamWriterFactory outputStreamWriterFactory =
      OutputStreamWriterFactory.STREAM;

  @JsonProperty("generator")
  @SuppressWarnings("PMD.FinalFieldCouldBeStatic") // set by Jackson
  private final RdfApiGeneratorFactory rdfApiGeneratorFactory = RdfApiGeneratorFactory.SEMANTIC;

  private RdfConfig() {}

  public static RdfConfig getDefaults() {
    return DEFAULT_INSTANCE;
  }

  public OutputStreamWriterFactory getWriterFactory() {
    return outputStreamWriterFactory;
  }

  public RdfApiGeneratorFactory getRdfApiGeneratorFactory() {
    return rdfApiGeneratorFactory;
  }
}
