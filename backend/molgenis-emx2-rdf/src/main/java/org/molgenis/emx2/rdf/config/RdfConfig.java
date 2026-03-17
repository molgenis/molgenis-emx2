package org.molgenis.emx2.rdf.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.molgenis.emx2.rdf.generators.RdfApiGeneratorFactory;
import org.molgenis.emx2.rdf.writers.WriterFactory;

/**
 * Contains relevant config data for the RDF module. Use {@link RdfConfigReader} to create object
 * with {@link org.molgenis.emx2.Schema} specific values. Use {@link #getDefaults()} to retrieve a
 * singleton containing the default values.
 */
public class RdfConfig {
  private static final RdfConfig DEFAULT_INSTANCE = new RdfConfig(RdfApiGeneratorFactory.SEMANTIC);
  private static final RdfConfig SEMANTIC_INSTANCE = new RdfConfig(RdfApiGeneratorFactory.SEMANTIC);

  @JsonProperty("writer")
  private final WriterFactory writerFactory = WriterFactory.STREAM;

  @JsonProperty("generator")
  private final RdfApiGeneratorFactory rdfApiGeneratorFactory;

  @JsonCreator
  private RdfConfig(@JsonProperty("generator") RdfApiGeneratorFactory rdfApiGeneratorFactory) {
    this.rdfApiGeneratorFactory =
        rdfApiGeneratorFactory != null ? rdfApiGeneratorFactory : RdfApiGeneratorFactory.SEMANTIC;
  }

  public static RdfConfig getDefaults() {
    return DEFAULT_INSTANCE;
  }

  public static RdfConfig semantic() {
    return SEMANTIC_INSTANCE;
  }

  public WriterFactory getWriterFactory() {
    return writerFactory;
  }

  public RdfApiGeneratorFactory getRdfApiGeneratorFactory() {
    return rdfApiGeneratorFactory;
  }
}
