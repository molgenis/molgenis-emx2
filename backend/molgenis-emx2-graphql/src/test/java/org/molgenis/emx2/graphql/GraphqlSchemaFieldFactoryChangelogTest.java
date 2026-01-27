package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.INT;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.tasks.TaskServiceInMemory;

class GraphqlSchemaFieldFactoryChangelogTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private GraphqlApi graphql;

  @BeforeEach
  void setup() {
    Database database = TestDatabaseFactory.getTestDatabase();
    TaskServiceInMemory taskService = new TaskServiceInMemory();

    Schema schema = database.dropCreateSchema(getClass().getSimpleName());
    schema.getMetadata().setSetting(Constants.IS_CHANGELOG_ENABLED, Boolean.TRUE.toString());
    Table person =
        schema.create(
            TableMetadata.table("Person")
                .add(column("ID").setType(INT).setPkey())
                .add(column("First_Name").setKey(2).setRequired(true))
                .add(column("Last_Name").setKey(2).setRequired(true)));

    person.insert(new Row("ID", 1).set("First_Name", "Geralt").set("Last_Name", "of Rivia"));
    person.insert(new Row("ID", 2).set("First_Name", "Jaskier").set("Last_Name", "Dandelion"));
    person.insert(new Row("ID", 3).set("First_Name", "Ciri").set("Last_Name", "of Cintra"));

    graphql = new GraphqlApi(schema, taskService);
  }

  @Test
  void whenQueryingChangesWithLimit_thenLimitNrResults() {
    Change[] changes =
        queryChanges(
            """
                  {
                    _changes(limit: 2) {
                      operation,
                      newRowData
                    }
                  }
            """);
    assertEquals(2, changes.length);
    assertTrue(changes[0].newRowData.contains("Ciri"));
    assertTrue(changes[1].newRowData.contains("Jaskier"));
  }

  @Test
  void whenQueryingChangesWithOffset_thenOffsetNrResults() {
    Change[] changes =
        queryChanges(
            """
                  {
                    _changes(offset: 2) {
                      operation,
                      newRowData
                    }
                  }
            """);
    assertEquals(1, changes.length);
    assertTrue(changes[0].newRowData.contains("Geralt"));
  }

  private Change[] queryChanges(String query) {
    ExecutionResult execute = graphql.execute(query);
    JsonNode jsonNode = MAPPER.valueToTree(execute.toSpecification()).get("data").get("_changes");
    return MAPPER.convertValue(jsonNode, Change[].class);
  }

  record Change(String operation, String newRowData) {}
}
