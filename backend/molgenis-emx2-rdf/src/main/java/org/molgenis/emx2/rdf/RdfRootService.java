package org.molgenis.emx2.rdf;

import java.io.OutputStream;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.molgenis.emx2.rdf.generators.RootRdfGenerator;
import org.molgenis.emx2.rdf.writers.RdfModelWriter;
import org.molgenis.emx2.rdf.writers.RdfWriter;

public class RdfRootService implements AutoCloseable {
  private final RdfWriter writer;
  private final RootRdfGenerator generator;

  public RootRdfGenerator getGenerator() {
    return generator;
  }

  public RdfRootService(String baseUrl, RDFFormat format, OutputStream out) {
    this.writer = new RdfModelWriter(out, format);
    this.generator = new RootRdfGenerator(writer, baseUrl);
  }

  @Override
  public void close() {
    writer.close();
  }
}
