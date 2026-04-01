package org.molgenis.emx2.harvester;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.TriplePattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.harvester.semantics.TableSemantics;
import org.molgenis.emx2.rdf.mappers.NamespaceMapper;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class SparqlGeneratedSelectTest {

  private static final ValueFactory VALUE_FACTORY = SimpleValueFactory.getInstance();

  Database database;
  Schema schema;
  NamespaceMapper namespaceMapper;
  TriplePattern mainPattern = null;
  List<GraphPattern> referencePatterns = new ArrayList<>();

  @BeforeEach
  void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    schema = database.getSchema("pet-store");
    namespaceMapper = new NamespaceMapper("http://localhost:8080/", schema);
  }

  @Test
  void generatedSparqlSelect() throws IOException {
    TableSemantics tableSemantics = new TableSemantics(schema);

    Table table = schema.getTable("Pet");
    TableMetadata tableMetadata = table.getMetadata();
    Repository repository = new SailRepository(new MemoryStore());

    try (RepositoryConnection conn = repository.getConnection();
        InputStream stream = readTtl("gen-query-pet-store.ttl")) {
      conn.add(stream, RDFFormat.TURTLE);

      SelectQuery query = generateQuery(tableMetadata, tableSemantics);

      System.out.println("-- Result " + "-".repeat(50) + "\n");
      System.out.println(query.getQueryString());
      System.out.println("-".repeat(60) + "\n");

      TupleQueryResult evaluate = conn.prepareTupleQuery(query.getQueryString()).evaluate();
      System.out.println("-- QUERY RESULT " + "-".repeat(44));
      evaluate.stream().forEach(System.out::println);
      System.out.println("-".repeat(60));
    }
  }

  private SelectQuery generateQuery(TableMetadata tableMetadata, TableSemantics tableSemantics) {
    SelectQuery query = Queries.SELECT();
    addPrefixes(query, namespaceMapper);

    Variable pet = SparqlBuilder.var(tableMetadata.getTableName());
    List<GraphPattern> queryPatterns = new ArrayList<>();

    Set<String> semantics =
        tableSemantics.getMappingsForTableColumns(tableMetadata.getTableName()).keySet();

    for (String semantic : semantics) {
      Column column =
          tableSemantics
              .getMappingForSemantic(tableMetadata.getTableName(), semantic)
              .orElseThrow()
              .column();

      Variable columnVariable = SparqlBuilder.var(column.getName());
      IRI semanticIRI = VALUE_FACTORY.createIRI(semantic);

      if (column.isRequired()) {
        addToMainPattern(pet, semanticIRI, columnVariable);
      } else {
        TriplePattern has = pet.has(semanticIRI, columnVariable);
        Optional<TriplePattern> reference = resolveReferences(column, columnVariable, query, has);
        if (reference.isPresent()) {
          queryPatterns.add(GraphPatterns.optional(has, reference.get()));
        } else {
          queryPatterns.add(GraphPatterns.optional(has));
        }
      }

      query.select(columnVariable);
    }

    query.select(pet).where(mainPattern);
    queryPatterns.forEach(query::where);
    referencePatterns.forEach(query::where);
    return query;
  }

  private Optional<TriplePattern> resolveReferences(
      Column column, Variable columnVariable, SelectQuery query, TriplePattern has) {
    if (!column.isReference()) {
      return Optional.empty();
    }

    for (Reference reference : column.getReferences()) {
      TableMetadata referencingTable = schema.getTable(reference.getTargetTable()).getMetadata();

      Column referencingColumn = referencingTable.getColumn(reference.getTargetColumn());

      if (TableType.ONTOLOGIES.equals(referencingTable.getTableType())) {
        IRI ontologySemantic = VALUE_FACTORY.createIRI("rdfs:label");
        Variable ontologyName =
            SparqlBuilder.var(referencingTable.getTableName() + referencingColumn.getName());
        query.select(ontologyName);

        TriplePattern referencePattern = columnVariable.has(ontologySemantic, ontologyName);
        return Optional.ofNullable(referencePattern);
      }
    }

    return Optional.empty();
  }

  private void addToMainPattern(Variable pet, IRI semantic, Variable columnVariable) {
    if (mainPattern == null) {
      mainPattern = pet.has(semantic, columnVariable);
    } else {
      mainPattern.andHas(semantic, columnVariable);
    }
  }

  private void addPrefixes(SelectQuery query, NamespaceMapper namespaceMapper) {
    Set<Namespace> namespaces = namespaceMapper.getAllNamespaces();
    namespaces.forEach(query::prefix);
  }

  private InputStream readTtl(String path) {
    return SparqlSelectTransformerTest.class.getResourceAsStream(path);
  }
}
