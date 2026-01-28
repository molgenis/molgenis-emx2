package org.molgenis.emx2.fairmapper.rdf;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.molgenis.emx2.fairmapper.PathValidator;

public class LocalRdfSource implements RdfSource {
  private final Path basePath;

  public LocalRdfSource(Path basePath) {
    this.basePath = basePath;
  }

  @Override
  public Model fetch(String pathOrUrl) throws IOException {
    Path filePath = PathValidator.validateWithinBase(basePath, pathOrUrl);
    try (InputStream in = Files.newInputStream(filePath)) {
      return Rio.parse(in, "", RDFFormat.TURTLE);
    }
  }
}
