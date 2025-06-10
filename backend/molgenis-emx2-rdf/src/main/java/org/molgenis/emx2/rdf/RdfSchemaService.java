package org.molgenis.emx2.rdf;

import java.io.OutputStream;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.molgenis.emx2.rdf.generators.RdfApiGeneratorFactory;
import org.molgenis.emx2.rdf.generators.RdfApiGenerator;
import org.molgenis.emx2.rdf.writers.RdfWriter;
import org.molgenis.emx2.rdf.writers.WriterFactory;

public class RdfSchemaService implements AutoCloseable {
  private final RdfConfig config;
  private final RdfWriter writer;
  private final RdfApiGenerator generator;

  public RdfApiGenerator getGenerator() {
    return generator;
  }

  public RdfSchemaService(String baseUrl, RDFFormat format, OutputStream out) {
    this.config = new RdfConfig(); // placeholder
    this.writer = WriterFactory.valueOf(config.getWriter()).create(out, format);
    this.generator = RdfApiGeneratorFactory.valueOf(config.getGenerator()).create(writer, baseUrl);
  }

  @Override
  public void close() {
    writer.close();
  }
}
