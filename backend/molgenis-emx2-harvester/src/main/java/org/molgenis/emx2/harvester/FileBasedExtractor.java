package org.molgenis.emx2.harvester;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.molgenis.emx2.MolgenisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileBasedExtractor {

  private static final Logger logger = LoggerFactory.getLogger(FileBasedExtractor.class);

  private final SailRepository repository;
  private final List<String> files;

  public FileBasedExtractor(SailRepository repository, List<String> files) {
    this.repository = repository;
    this.files = files;
  }

  public void extract() {
    try (SailRepositoryConnection conn = repository.getConnection()) {
      for (String file : files) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
          logger.info("Extracting file: {}", file);

          conn.add(fileInputStream, RDFFormat.TURTLE);
        } catch (IOException e) {
          throw new MolgenisException(
              "Something went wrong extracting RDF data from file: " + file, e);
        }
      }
      conn.commit();
    }
  }
}
