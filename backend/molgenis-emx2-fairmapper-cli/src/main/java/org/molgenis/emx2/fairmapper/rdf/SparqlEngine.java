package org.molgenis.emx2.fairmapper.rdf;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.molgenis.emx2.fairmapper.FairMapperException;

public class SparqlEngine {
  private static final int MAX_TRIPLES = 100_000;
  private static final int QUERY_TIMEOUT_SECONDS = 30;

  public static Model construct(Model input, String sparql) {
    if (input.size() > MAX_TRIPLES) {
      throw new FairMapperException("Input too large: " + input.size() + " triples");
    }
    SailRepository repo = new SailRepository(new MemoryStore());
    repo.init();
    try (RepositoryConnection conn = repo.getConnection()) {
      conn.add(input);
      GraphQuery query = conn.prepareGraphQuery(sparql);
      query.setMaxExecutionTime(QUERY_TIMEOUT_SECONDS);
      return QueryResults.asModel(query.evaluate());
    } finally {
      repo.shutDown();
    }
  }
}
