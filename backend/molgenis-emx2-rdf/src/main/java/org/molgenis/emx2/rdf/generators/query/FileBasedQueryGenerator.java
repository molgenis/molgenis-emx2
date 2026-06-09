package org.molgenis.emx2.rdf.generators.query;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.TableMetadata;

public class FileBasedQueryGenerator implements QueryGenerator {

  private final Map<String, Path> paths;

  public FileBasedQueryGenerator(Map<String, Path> paths) {
    this.paths = paths;
  }

  @Override
  public String generate(TableMetadata tableMetadata) {
    if (!paths.containsKey(tableMetadata.getTableName())) {
      throw new MolgenisException(
          "No file path defined for table: " + tableMetadata.getTableName());
    }

    try {
      return Files.readString(paths.get(tableMetadata.getTableName()));
    } catch (IOException e) {
      throw new MolgenisException("No file on path: " + paths, e);
    }
  }
}
