package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.graphql.GraphqlExecutor.convertExecutionResultToJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Part;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.BinaryFileWrapper;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class TestGraphqlTableMutationFiles {

  private static final String SCHEMA = TestGraphqlTableMutationFiles.class.getSimpleName();
  private static final String SAVE = "mutation($rows:[DocInput]){save(Doc:$rows){message}}";
  private static final String NAME = "name";
  private static final String FILE = "file";
  private static final String FILES = "{Doc(orderby:{name:ASC}){name file{size filename}}}";
  private static GraphqlExecutor graphql;

  @BeforeAll
  static void setup() {
    Schema schema = TestDatabaseFactory.getTestDatabase().dropCreateSchema(SCHEMA);
    schema.create(table("Doc", column(NAME).setPkey(), column(FILE).setType(ColumnType.FILE)));
    for (String name : List.of("keep", "remove", "replace", "untouched")) {
      schema
          .getTable("Doc")
          .insert(
              row(
                  NAME,
                  name,
                  FILE,
                  new BinaryFileWrapper("text/plain", name + ".txt", "old".getBytes())));
    }
    graphql = new GraphqlExecutor(schema);
  }

  @Test
  void saveGivesEachRowOfAMixedBatchItsOwnFileOutcome() throws Exception {
    Part upload = mock(Part.class);
    when(upload.getInputStream()).thenReturn(new ByteArrayInputStream("brand new".getBytes()));
    when(upload.getContentType()).thenReturn("text/plain");
    when(upload.getSubmittedFileName()).thenReturn("new.txt");

    JsonNode before = execute(FILES, null).path("Doc");
    assertEquals(4, before.size());
    assertEquals(3, before.get(0).at("/file/size").asInt());

    List<Map<String, Object>> rows =
        List.of(
            input("keep", Map.of("id", "x")),
            input("remove", null),
            input("replace", upload),
            input("fresh", Map.of("id", "x")));
    execute(SAVE, Map.of("rows", rows));

    JsonNode after = execute(FILES, null).path("Doc");
    assertEquals(5, after.size());
    assertEquals("keep.txt", byName(after, "keep").at("/file/filename").asText());
    assertEquals(3, byName(after, "keep").at("/file/size").asInt());
    assertTrue(
        byName(after, "remove").path("file").isMissingNode()
            || byName(after, "remove").path("file").isNull());
    assertEquals("new.txt", byName(after, "replace").at("/file/filename").asText());
    assertEquals(9, byName(after, "replace").at("/file/size").asInt());
    assertTrue(
        byName(after, "fresh").path("file").isMissingNode()
            || byName(after, "fresh").path("file").isNull());
    assertEquals(3, byName(after, "untouched").at("/file/size").asInt());
  }

  @Test
  void aStringAsFileValueIsRefusedAndKeepsTheStoredFile() throws Exception {
    JsonNode before = execute(FILES, null);
    assertThrows(
        MolgenisException.class,
        () -> execute(SAVE, Map.of("rows", List.of(input("untouched", "text")))));
    assertEquals(before, execute(FILES, null));
  }

  private static Map<String, Object> input(String name, Object file) {
    Map<String, Object> row = new java.util.LinkedHashMap<>();
    row.put(NAME, name);
    row.put(FILE, file);
    return row;
  }

  private static JsonNode byName(JsonNode docs, String name) {
    for (JsonNode doc : docs) {
      if (name.equals(doc.get(NAME).asText())) {
        return doc;
      }
    }
    throw new IllegalStateException(name);
  }

  private static JsonNode execute(String query, Map<String, Object> variables) throws IOException {
    String result =
        convertExecutionResultToJson(
            variables == null
                ? graphql.executeWithoutSession(query)
                : graphql.executeWithoutSession(query, variables));
    return new ObjectMapper().readTree(result).get("data");
  }
}
