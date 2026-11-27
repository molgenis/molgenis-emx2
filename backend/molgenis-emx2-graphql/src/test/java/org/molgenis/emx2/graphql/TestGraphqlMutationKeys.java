package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.AUTO_ID;
import static org.molgenis.emx2.ColumnType.REF;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.graphql.GraphqlExecutor.convertExecutionResultToJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class TestGraphqlMutationKeys {

  private static final String SCHEMA_NAME = TestGraphqlMutationKeys.class.getSimpleName();
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static Schema schema;
  private static GraphqlExecutor graphqlExecutor;

  @BeforeAll
  static void setup() {
    Database database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(SCHEMA_NAME);
    schema.create(
        table("Pet", column("id").setType(AUTO_ID).setPkey(), column("name")),
        table("Dog").setInheritName("Pet").add(column("breed")),
        table("Owner", column("name").setPkey()),
        table(
            "Ownership",
            column("owner", REF).setRefTable("Owner").setPkey(),
            column("pet", REF).setRefTable("Pet").setPkey()));
    graphqlExecutor = new GraphqlExecutor(schema);
  }

  @BeforeEach
  void emptyTables() {
    schema.getTable("Ownership").truncate();
    schema.getTable("Owner").truncate();
    schema.getTable("Dog").truncate();
    schema.getTable("Pet").truncate();
  }

  @Test
  void insertReturnsGeneratedAutoIdsInInputOrder() throws IOException {
    JsonNode keys =
        execute(
            """
            mutation {
              insert(Pet: [{ name: "first" }, { name: "second" }]) {
                keys { table keys }
              }
            }
            """);

    assertEquals("Pet", keys.get(0).get("table").asText());
    JsonNode petKeys = keys.get(0).get("keys");
    assertEquals(2, petKeys.size());
    String firstId = idOf(petKeys.get(0));
    String secondId = idOf(petKeys.get(1));
    assertNotEquals(firstId, secondId);
    assertEquals("first", nameOfPet(firstId));
    assertEquals("second", nameOfPet(secondId));
  }

  @Test
  void insertIntoSubclassReturnsAutoIdOfParent() throws IOException {
    JsonNode keys =
        execute(
            """
            mutation {
              insert(Dog: [{ name: "rex", breed: "boxer" }]) { keys { table keys } }
            }
            """);

    assertEquals("Dog", keys.get(0).get("table").asText());
    assertEquals("rex", nameOfPet(idOf(keys.get(0).get("keys").get(0))));
  }

  @Test
  void compositeReferenceKeyHasQueryShape() throws IOException {
    schema.getTable("Owner").insert(new Row("name", "anna"));
    String petId = idOf(insertPet("tom"));

    JsonNode keys =
        execute(
            """
            mutation {
              insert(Ownership: [{ owner: { name: "anna" }, pet: { id: "%s" } }]) {
                keys { table keys }
              }
            }
            """
                .formatted(petId));

    JsonNode key = MAPPER.readTree(keys.get(0).get("keys").get(0).asText());
    assertEquals(
        MAPPER.readTree("{\"owner\":{\"name\":\"anna\"},\"pet\":{\"id\":\"%s\"}}".formatted(petId)),
        key);
  }

  @Test
  void saveReturnsKeysOfNewAndExistingRows() throws IOException {
    String existingId = idOf(insertPet("old"));

    JsonNode keys =
        execute(
            """
            mutation {
              save(Pet: [{ id: "%s", name: "renamed" }, { name: "new" }]) { keys { table keys } }
            }
            """
                .formatted(existingId));

    JsonNode petKeys = keys.get(0).get("keys");
    assertEquals(existingId, idOf(petKeys.get(0)));
    assertEquals("new", nameOfPet(idOf(petKeys.get(1))));
  }

  @Test
  void deleteReturnsNoKeys() throws IOException {
    String id = idOf(insertPet("gone"));

    JsonNode keys =
        execute("mutation { delete(Pet: [{ id: \"%s\" }]) { keys { table } } }".formatted(id));

    assertEquals(0, keys.size());
  }

  private JsonNode insertPet(String name) throws IOException {
    JsonNode keys =
        execute(
            "mutation { insert(Pet: [{ name: \"%s\" }]) { keys { table keys } } }".formatted(name));
    return keys.get(0).get("keys").get(0);
  }

  private static String idOf(JsonNode jsonStringKey) throws IOException {
    JsonNode key = MAPPER.readTree(jsonStringKey.asText());
    assertNotNull(key.get("id"), "key has no id: " + key);
    return key.get("id").asText();
  }

  private static String nameOfPet(String id) {
    return schema.getTable("Pet").retrieveRows().stream()
        .filter(row -> id.equals(row.getString("id")))
        .findFirst()
        .orElseThrow(() -> new AssertionError("no pet with id " + id))
        .getString("name");
  }

  /** Executes the mutation and returns the `keys` of its single root field. */
  private JsonNode execute(String query) throws IOException {
    String result = convertExecutionResultToJson(graphqlExecutor.executeWithoutSession(query));
    JsonNode node = MAPPER.readTree(result);
    if (node.get("errors") != null) {
      throw new MolgenisException(node.get("errors").get(0).get("message").asText());
    }
    return node.get("data").elements().next().get("keys");
  }
}
