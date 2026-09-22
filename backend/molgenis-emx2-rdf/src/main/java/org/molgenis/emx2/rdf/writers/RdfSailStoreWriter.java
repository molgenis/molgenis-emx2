package org.molgenis.emx2.rdf.writers;

import static java.util.Objects.requireNonNull;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;

public class RdfSailStoreWriter implements RdfWriter {
  private final SailRepositoryConnection connection;

  public RdfSailStoreWriter(SailRepository repository) {
    connection = requireNonNull(repository).getConnection();
  }

  @Override
  public void processNamespace(Namespace namespace) {
    connection.setNamespace(namespace.getPrefix(), namespace.getName());
  }

  @Override
  public void processTriple(Statement statement) {
    connection.add(statement);
  }

  @Override
  public void processTriple(Resource subject, IRI predicate, Value object) {
    connection.add(subject, predicate, object);
  }

  @Override
  public void close() {
    connection.close();
  }
}
