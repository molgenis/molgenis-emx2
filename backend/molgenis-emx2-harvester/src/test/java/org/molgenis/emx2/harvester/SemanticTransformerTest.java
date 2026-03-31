package org.molgenis.emx2.harvester;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.harvester.semantics.TableNameMapping;
import org.molgenis.emx2.harvester.semantics.TableSemantics;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class SemanticTransformerTest {

  private Schema schema;
  private Table table;

  @BeforeEach
  void setUp() {
    Database db = TestDatabaseFactory.getTestDatabase();
    schema = db.getSchema("pet store");
    table = schema.getTable("Pet");
  }

  @Test
  void buildRowsFromSemantics() throws IOException {
    TableSemantics tableSemantics = new TableSemantics(schema);
    TableNameMapping tableNameMapping = new TableNameMapping(schema);
    Model parse = readRdf();

    Set<String> supportedSemantics = tableSemantics.getSupportedSemantics();

    Map<Resource, List<Statement>> entityStatements =
        groupStatementsByResource(parse, supportedSemantics);

    for (Map.Entry<Resource, List<Statement>> entry : entityStatements.entrySet()) {
      List<Statement> statements = entry.getValue();
      Optional<String> optTableName = tableNameMapping.getTableNameFromStatements(statements);
      if (optTableName.isEmpty()) {
        continue;
      }
      String tableName = optTableName.get();

      System.out.println("-".repeat(200));
      System.out.println(" STATEMENTS");
      System.out.println("-".repeat(200));

      Row row = new Row();
      for (Statement statement : statements) {
        System.out.println(statement.toString());
        String predicate = statement.getPredicate().stringValue();
        Optional<TableSemantics.ColumnMapping> mapping =
            tableSemantics.getMappingForSemantic(tableName, predicate);
        if (mapping.isEmpty()) {
          continue;
        }
        Column column = mapping.get().column();
        RdfToRowValueMapper.mapToColumnType(statement.getObject(), column)
            .ifPresent(val -> row.set(column.getName(), val));
      }

      System.out.println("-".repeat(200));
      System.out.println(" ROW     | " + row);
      System.out.println("-".repeat(200));
      System.out.println("\n");
    }
  }

  private static Map<Resource, List<Statement>> groupStatementsByResource(
      Model parse, Set<String> supportedSemantics) {
    Map<Resource, List<Statement>> entityStatements = new HashMap<>();

    for (Statement statement : parse) {
      if (supportedSemantics.contains(statement.getPredicate().toString())) {
        List<Statement> statements =
            entityStatements.computeIfAbsent(statement.getSubject(), resource -> new ArrayList<>());
        statements.add(statement);
      }
    }
    return entityStatements;
  }

  private Model readRdf() throws IOException {
    Model parse;
    try (InputStream stream =
        SemanticTransformerTest.class.getResourceAsStream("pet-store-semantics.ttl")) {
      parse = Rio.parse(stream, RDFFormat.TURTLE);
    }
    return parse;
  }
}
