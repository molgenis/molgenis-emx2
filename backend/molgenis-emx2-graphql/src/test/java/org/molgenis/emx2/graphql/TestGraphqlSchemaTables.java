package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.graphql.GraphqlExecutor.convertExecutionResultToJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class TestGraphqlSchemaTables {

  private static final String PARENT_SCHEMA =
      TestGraphqlSchemaTables.class.getSimpleName() + "Parent";
  private static final String CHILD_SCHEMA =
      TestGraphqlSchemaTables.class.getSimpleName() + "Child";
  private static final String PARENT_TABLE = "Shape type";
  private static final String PARENT_TABLE_ID = "ShapeType";
  private static final String PARENT_COLUMN = "name";
  private static final String CHILD_TABLE = "MyShape";
  private static final String CHILD_COLUMN = "surface";
  private static final String NON_INHERITING_TABLE = "Note";
  private static final String NON_INHERITING_COLUMN = "id";
  private static final String TABLES_QUERY =
      "{_schema{tables{name,inheritName,inheritSchemaName,inheritId,columns{name,inherited}}}}";

  private static GraphqlExecutor graphql;

  @BeforeAll
  static void setup() {
    Database database = TestDatabaseFactory.getTestDatabase();
    database.becomeAdmin();
    database.dropSchemaIfExists(CHILD_SCHEMA);
    database.dropSchemaIfExists(PARENT_SCHEMA);
    Schema parent = database.createSchema(PARENT_SCHEMA);
    parent.create(table(PARENT_TABLE).add(column(PARENT_COLUMN).setPkey()));
    Schema child = database.createSchema(CHILD_SCHEMA);
    child.create(
        table(CHILD_TABLE)
            .setInheritNames(PARENT_TABLE)
            .setImportSchema(PARENT_SCHEMA)
            .add(column(CHILD_COLUMN)));
    child.create(table(NON_INHERITING_TABLE).add(column(NON_INHERITING_COLUMN).setPkey()));
    graphql = new GraphqlExecutor(child);
  }

  @Test
  void schemaReturnsInheritSchemaName() throws IOException {
    JsonNode child = tableByName(execute(TABLES_QUERY), CHILD_TABLE);
    assertEquals(PARENT_TABLE, child.get(GraphqlConstants.INHERIT_NAME).asText());
    assertEquals(PARENT_SCHEMA, child.get(GraphqlConstants.INHERIT_SCHEMA_NAME).asText());
  }

  @Test
  void schemaReturnsNullInheritSchemaNameForNonInheritingTable() throws IOException {
    JsonNode note = tableByName(execute(TABLES_QUERY), NON_INHERITING_TABLE);
    JsonNode inheritSchemaName = note.path(GraphqlConstants.INHERIT_SCHEMA_NAME);
    assertTrue(inheritSchemaName.isMissingNode() || inheritSchemaName.isNull(), note.toString());
  }

  @Test
  void changeMutationPreservesInheritSchemaName() throws IOException {
    assertEquals(
        PARENT_SCHEMA,
        tableByName(execute(TABLES_QUERY), CHILD_TABLE)
            .get(GraphqlConstants.INHERIT_SCHEMA_NAME)
            .asText());

    execute(
        """
        mutation{change(tables:[
          {name:"MyShape",inheritName:"%s",inheritSchemaName:"%s",columns:[{name:"surface"}]},
          {name:"Note"}
        ]){message}}"""
            .formatted(PARENT_TABLE, PARENT_SCHEMA));

    assertEquals(
        PARENT_SCHEMA,
        tableByName(execute(TABLES_QUERY), CHILD_TABLE)
            .get(GraphqlConstants.INHERIT_SCHEMA_NAME)
            .asText());
  }

  @Test
  void schemaReturnsPascalCaseInheritIdForInheritingTable() throws IOException {
    JsonNode child = tableByName(execute(TABLES_QUERY), CHILD_TABLE);
    assertEquals(
        PARENT_TABLE_ID, child.path(GraphqlConstants.INHERIT_ID).asText(), child.toString());
  }

  @Test
  void schemaReturnsNoInheritIdForNonInheritingTable() throws IOException {
    JsonNode note = tableByName(execute(TABLES_QUERY), NON_INHERITING_TABLE);
    JsonNode inheritId = note.path(GraphqlConstants.INHERIT_ID);
    assertTrue(inheritId.isMissingNode() || inheritId.isNull(), note.toString());
  }

  @Test
  void schemaMarksOnlyParentColumnsInheritedForInheritingTable() throws IOException {
    JsonNode child = tableByName(execute(TABLES_QUERY), CHILD_TABLE);
    assertTrue(isInherited(child, PARENT_COLUMN), child.toString());
    assertFalse(isInherited(child, CHILD_COLUMN), child.toString());
  }

  @Test
  void schemaMarksNoColumnInheritedForNonInheritingTable() throws IOException {
    JsonNode note = tableByName(execute(TABLES_QUERY), NON_INHERITING_TABLE);
    assertFalse(isInherited(note, NON_INHERITING_COLUMN), note.toString());
  }

  private static boolean isInherited(JsonNode table, String columnName) {
    for (JsonNode column : table.path(GraphqlConstants.COLUMNS)) {
      if (columnName.equals(column.path(GraphqlConstants.NAME).asText())) {
        return column.path(GraphqlConstants.INHERITED).asBoolean();
      }
    }
    return fail("column '" + columnName + "' not found in table " + table);
  }

  private static JsonNode tableByName(JsonNode data, String tableName) {
    for (JsonNode table : data.at("/_schema/tables")) {
      if (tableName.equals(table.path("name").asText())) {
        return table;
      }
    }
    return fail("table '" + tableName + "' not found in _schema");
  }

  private JsonNode execute(String query) throws IOException {
    String result = convertExecutionResultToJson(graphql.executeWithoutSession(query));
    JsonNode node = new ObjectMapper().readTree(result);
    if (node.get("errors") != null) {
      throw new MolgenisException(node.get("errors").get(0).get("message").asText());
    }
    return node.get("data");
  }
}
