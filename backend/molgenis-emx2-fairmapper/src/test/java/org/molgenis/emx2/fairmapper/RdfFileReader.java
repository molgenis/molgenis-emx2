package org.molgenis.emx2.fairmapper;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URL;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

public class RdfFileReader {

  public static SailRepository readFiles(String... filenames) {
    SailRepository repository = new SailRepository(new MemoryStore());
    try (SailRepositoryConnection connection = repository.getConnection()) {
      for (String filename : filenames) {
        URL url = RdfFileReader.class.getResource(filename);
        connection.add(url, RDFFormat.TURTLE);
      }
      connection.commit();
    } catch (IOException e) {
      fail("Unable to set up SailRepository for petstore.ttl", e);
    }

    return repository;
  }
}
