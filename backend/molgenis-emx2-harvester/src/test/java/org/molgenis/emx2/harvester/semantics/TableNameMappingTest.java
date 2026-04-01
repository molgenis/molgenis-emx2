package org.molgenis.emx2.harvester.semantics;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class TableNameMappingTest {

  private static final SimpleValueFactory VALUE_FACTORY = SimpleValueFactory.getInstance();

  @Test
  void shouldGetTableNames() {
    Database database = TestDatabaseFactory.getTestDatabase();
    Schema schema = database.dropCreateSchema(getClass().getSimpleName());

    SchemaMetadata metadata =
        schema
            .getMetadata()
            .create(
                TableMetadata.table("a").setSemantics("lnc:a"),
                TableMetadata.table("b").setSemantics("foaf:b"),
                TableMetadata.table("c"));

    TableNameMapping tableSemantics = new TableNameMapping(schema);

    Statement s1 =
        statement(
            "http://example.org/subject",
            "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
            "http://purl.bioontology.org/ontology/LNC/a");

    Statement s2 =
        statement(
            "http://example.org/subject",
            "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
            "http://xmlns.com/foaf/0.1/b");

    assertEquals("a", tableSemantics.getTableNameFromStatements(List.of(s1)).orElseThrow());
    assertEquals("b", tableSemantics.getTableNameFromStatements(List.of(s2)).orElseThrow());
  }

  private static Statement statement(String subject, String semantic, String value) {
    return VALUE_FACTORY.createStatement(
        VALUE_FACTORY.createIRI(subject),
        VALUE_FACTORY.createIRI(semantic),
        VALUE_FACTORY.createLiteral(value));
  }
}
