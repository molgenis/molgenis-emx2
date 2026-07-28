package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.graphql.GraphqlExecutor.convertExecutionResultToJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class TestGraphqlTableRename {

  private static final String SCHEMA = TestGraphqlTableRename.class.getSimpleName();

  private static Database database;

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    database.becomeAdmin();
  }

  @Test
  public void renamedTableStillAcceptsInsertsViaChangeMutation() {
    Schema schema = database.dropCreateSchema(SCHEMA);
    schema.create(table("Plain").add(column("name").setPkey()));
    schema.getTable("Plain").insert(row("name", "before"));

    change(
        schema,
        """
        mutation{change(tables:[{name:"Renamed",oldName:"Plain"}]){message}}""");

    database.clearCache();
    Schema reloaded = database.getSchema(SCHEMA);
    reloaded.getTable("Renamed").insert(row("name", "after"));
    assertEquals(2, reloaded.getTable("Renamed").retrieveRows().size());
  }

  private static void change(Schema schema, String mutation) {
    JsonNode node;
    try {
      String result =
          convertExecutionResultToJson(new GraphqlExecutor(schema).executeWithoutSession(mutation));
      node = new ObjectMapper().readTree(result);
    } catch (Exception e) {
      throw new MolgenisException("could not run graphql mutation", e);
    }
    if (node.get("errors") != null) {
      throw new MolgenisException(node.get("errors").get(0).get("message").asText());
    }
  }
}
