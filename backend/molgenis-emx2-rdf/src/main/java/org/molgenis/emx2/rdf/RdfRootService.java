package org.molgenis.emx2.rdf;

import java.io.OutputStream;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.molgenis.emx2.rdf.generators.RootRdfGenerator;
import org.molgenis.emx2.rdf.writers.RdfStreamWriter;
import org.molgenis.emx2.rdf.writers.RdfWriter;

public class RdfRootService implements RdfService<RootRdfGenerator> {
  private final RdfWriter writer;
  private final RootRdfGenerator generator;

  public RdfRootService(String baseUrl, RDFFormat format, OutputStream out) {
    this.writer = new RdfStreamWriter(out, format);
    this.generator = new RootRdfGenerator(writer, baseUrl);
  }

  @Override
  public RootRdfGenerator getGenerator() {
    return generator;
  }

  @Override
  public void close() {
    writer.close();
  }
}
