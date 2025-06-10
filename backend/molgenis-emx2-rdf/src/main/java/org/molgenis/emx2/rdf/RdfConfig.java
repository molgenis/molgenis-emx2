package org.molgenis.emx2.rdf;

import org.molgenis.emx2.rdf.generators.RdfApiGeneratorFactory;
import org.molgenis.emx2.rdf.writers.WriterFactory;

// todo: replace placeholder with full implementation that uses advaned setting to store values
public class RdfConfig {
  private final WriterFactory writerFactory = WriterFactory.valueOf("MODEL");
  private final RdfApiGeneratorFactory rdfApiGeneratorFactory = RdfApiGeneratorFactory.valueOf("EMX2");

  public WriterFactory getWriterFactory() {
    return writerFactory;
  }

  public RdfApiGeneratorFactory getRdfApiGeneratorFactory() {
    return rdfApiGeneratorFactory;
  }
}
