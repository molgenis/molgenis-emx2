package org.molgenis.emx2.fairmapper.extractors;

import java.io.IOException;
import java.net.URI;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.molgenis.emx2.MolgenisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteRdfExtractor implements RdfExtractor {

  private static final Logger logger = LoggerFactory.getLogger(RemoteRdfExtractor.class);

  @Override
  public void addRdfToRepository(SailRepository repository, URI endpoint) {
    try (RepositoryConnection conn = repository.getConnection()) {
      logger.info("Extracting rdf from endpoint: {}", endpoint);
      conn.add(endpoint.toURL());
    } catch (IOException e) {
      throw new MolgenisException("Unable to add RDF data from endpoint URL: " + endpoint, e);
    }
  }
}
