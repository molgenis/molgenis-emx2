package org.molgenis.emx2.rdf.generators.query.generators;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Statements;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

public class SparqlQueryTestUtils {

  static void assertHasResults(TupleQueryResult result, List<Map<String, String>> expectedResults) {
    Iterator<Map<String, String>> iterator = expectedResults.iterator();
    while (result.hasNext()) {
      BindingSet actual = result.next();

      if (!iterator.hasNext()) {
        fail("Found more results than expected");
      }
      Map<String, String> expected = iterator.next();

      assertEquals(
          actual.getBindingNames(),
          expected.keySet(),
          () -> "Binding names of actual: " + actual + " does not match expected: " + expected);

      for (String bindingName : actual.getBindingNames()) {
        assertEquals(
            expected.get(bindingName),
            actual.getValue(bindingName).stringValue(),
            () -> "Values of actual: " + actual + " does not match expected: " + expected);
      }
    }

    if (iterator.hasNext()) {
      fail("Found less results than expected");
    }
  }

  static SailRepository repository(Statement... statements) {
    SailRepository repository = new SailRepository(new MemoryStore());
    try (RepositoryConnection connection = repository.getConnection()) {
      for (Statement statement : statements) {
        connection.add(statement);
      }
    }
    return repository;
  }

  static Statement statement(String subject, IRI predicate, String object) {
    return Statements.statement(iri(subject), predicate, literal(object), null);
  }

  static TupleQueryResult executeQuery(SailRepositoryConnection connection, String query) {
    TupleQuery prepared = connection.prepareTupleQuery(QueryLanguage.SPARQL, query);
    return prepared.evaluate();
  }
}
