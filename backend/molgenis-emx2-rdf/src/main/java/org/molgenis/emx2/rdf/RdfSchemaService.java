package org.molgenis.emx2.rdf;

import java.io.OutputStream;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.rdf.config.RdfConfig;
import org.molgenis.emx2.rdf.config.RdfConfigReader;
import org.molgenis.emx2.rdf.generators.RdfApiGenerator;
import org.molgenis.emx2.rdf.writers.RdfWriter;

public class RdfSchemaService implements RdfService<RdfApiGenerator> {
  private final RdfConfig config;
  private final RdfWriter writer;
  private final RdfApiGenerator generator;

  public RdfSchemaService(String baseUrl, Schema schema, RDFFormat format, OutputStream out) {
    this.config = RdfConfigReader.read(schema);
    this.writer = config.getWriterFactory().create(out, format);
    this.generator = config.getRdfApiGeneratorFactory().create(writer, baseUrl);
  }

  @Override
  public RdfApiGenerator getGenerator() {
    return generator;
  }

  @Override
  public void close() {
    writer.close();
  }
}
