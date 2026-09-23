package org.molgenis.emx2.rdf.generators.query.generators;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
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

  @SafeVarargs
  static void assertHasResults(TupleQueryResult result, Map<String, String>... expectedResults) {
    Set<Map<String, String>> expected = Arrays.stream(expectedResults).collect(Collectors.toSet());
    Set<Map<String, String>> actual = new HashSet<>();

    while (result.hasNext()) {
      BindingSet binding = result.next();
      actual.add(
          binding.getBindingNames().stream()
              .collect(
                  Collectors.toMap(Function.identity(), b -> binding.getValue(b).stringValue())));
    }

    assertEquals(expected, actual);
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

  static Statement statement(String subject, IRI predicate, IRI object) {
    return Statements.statement(iri(subject), predicate, object, null);
  }

  static TupleQueryResult executeQuery(SailRepositoryConnection connection, String query) {
    TupleQuery prepared = connection.prepareTupleQuery(QueryLanguage.SPARQL, query);
    return prepared.evaluate();
  }

  /**
   * Asserts that the generated query matches the expected query, ignoring the {@code PREFIX}
   * declarations at the top of the generated query.
   */
  static void assertQueryEquals(String expectedQueryWithoutPrefixes, String actualGeneratedQuery) {
    assertEquals(expectedQueryWithoutPrefixes, removePrefixesFromQuery(actualGeneratedQuery));
  }

  private static String removePrefixesFromQuery(String query) {
    return query.replaceAll("PREFIX .*\n", "");
  }
}
