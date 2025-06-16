package org.molgenis.emx2.rdf.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.molgenis.emx2.rdf.generators.RdfApiGeneratorFactory;
import org.molgenis.emx2.rdf.writers.WriterFactory;

/**
 * Contains relevant config data for the RDF module. Use {@link RdfConfigReader} to create object
 * with {@link org.molgenis.emx2.Schema} specific values. Use {@link #getDefaults()} to
 * retrieve a singleton containing the default values.
 */
public class RdfConfig {
  private static final RdfConfig DEFAULT_INSTANCE = new RdfConfig();

  @JsonProperty("writer")
  private final WriterFactory writerFactory = WriterFactory.MODEL;

  @JsonProperty("generator")
  private final RdfApiGeneratorFactory rdfApiGeneratorFactory = RdfApiGeneratorFactory.EMX2;

  private RdfConfig() {}

  public static RdfConfig getDefaults() {
    return DEFAULT_INSTANCE;
  }

  public WriterFactory getWriterFactory() {
    return writerFactory;
  }

  public RdfApiGeneratorFactory getRdfApiGeneratorFactory() {
    return rdfApiGeneratorFactory;
  }
}
