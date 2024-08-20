package org.molgenis.emx2.web;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class TestValidationWithBindings {

  private static Database database;
  private static final String schemaName = TestValidationWithBindings.class.getSimpleName();
  private static Schema schema;

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(schemaName);
    JavaScriptBindings.getBindingsForSession(new MolgenisSession(database));
  }

  @Test
  public void testValidationWithSimplePostClient() {
    String validationScript =
        """
        (function () {
          let result = simplePostClient(
            `query Test1( $filter:Test1Filter, $orderby:Test1orderby ) {
               Test1( filter:$filter, limit:20, offset:0, orderby:$orderby ) {
                 name age
               }
            }`,
            {
              filter: {
                age: {
                  equals: age
                }
              },
              orderby: {}
            }, "TestValidationWithBindings"
          );
          console.log(result);
          return result.Test1 == null;
        })()
        """;

    Table test1 =
        schema.create(
            table(
                "Test1",
                column("id").setPkey(),
                column("name"),
                column("age").setVisible("name").setType(ColumnType.INT)));
    test1.insert(row("id", 1, "name", "jan", "age", 11));

    Table test2 =
        schema.create(
            table(
                "Test2",
                column("id").setPkey(),
                column("name"),
                column("age")
                    .setValidation(validationScript)
                    .setVisible("name")
                    .setType(ColumnType.INT)));

    test2.insert(row("id", 1, "name", "jan", "age", 10));
    // Should throw exception as age 11 is present in Test1 table:
    assertThrows(
        MolgenisException.class, () -> test2.insert(row("id", 2, "name", "jan", "age", 11)));
  }
}
