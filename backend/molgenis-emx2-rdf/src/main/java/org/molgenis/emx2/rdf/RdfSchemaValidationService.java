package org.molgenis.emx2.rdf;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.rdf.config.RdfConfig;
import org.molgenis.emx2.rdf.config.RdfConfigReader;
import org.molgenis.emx2.rdf.generators.RdfApiGenerator;
import org.molgenis.emx2.rdf.writers.RdfWriter;
import org.molgenis.emx2.rdf.writers.ShaclResultWriter;

public class RdfSchemaValidationService implements RdfService<RdfApiGenerator> {
  private final RdfConfig config;
  private final RdfWriter writer;
  private final RdfApiGenerator generator;

  public RdfSchemaValidationService(
      String baseUrl, Schema schema, RDFFormat format, OutputStream out, File[] shaclFiles)
      throws IOException {
    this.config = RdfConfigReader.read(schema);
    this.writer = new ShaclResultWriter(out, format, shaclFiles);
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
