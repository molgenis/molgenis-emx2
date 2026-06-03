package org.molgenis.emx2.rdf.generators.query;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.TableMetadata;

public class FileBasedQueryGenerator implements QueryGenerator {

  private final Path path;

  public FileBasedQueryGenerator(Path path) {
    this.path = path;
  }

  @Override
  public String generate(TableMetadata tableMetadata) {
    try {
      return Files.readString(path);
    } catch (IOException e) {
      throw new MolgenisException("No file on path: " + path, e);
    }
  }
}
