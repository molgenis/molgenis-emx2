package org.molgenis.emx2.rdf;

import java.io.OutputStream;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.molgenis.emx2.rdf.generators.RdfApiGeneratorFactory;
import org.molgenis.emx2.rdf.generators.RdfApiPaths;
import org.molgenis.emx2.rdf.writers.RdfWriter;
import org.molgenis.emx2.rdf.writers.WriterFactory;

public class RdfService2 implements AutoCloseable {
  private final RdfConfig config;
  private final RdfWriter writer;
  private final RdfApiPaths generator;

  public RdfApiPaths getGenerator() {
    return generator;
  }

  public RdfService2(String baseUrl, RDFFormat format, OutputStream out) {
    this.config = new RdfConfig(); // placeholder
    this.writer = WriterFactory.valueOf(config.getWriter()).create(out, format);
    this.generator = RdfApiGeneratorFactory.valueOf(config.getGenerator()).create(writer, baseUrl);
  }

  @Override
  public void close() {
    writer.close();
  }
}
