package org.molgenis.emx2.web;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class TestValidationWithJavaScriptBindings {

  private static Database database;
  private static final String schemaName =
      TestValidationWithJavaScriptBindings.class.getSimpleName();
  private static Schema schema;

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(schemaName);
    JavaScriptBindings.getBindingsForSession(new MolgenisSession(database));
  }

  @Test
  public void testValidationWithSimplePostClient_shouldThrowWhenValidationFails() {
    // validation script that checks if the age inserted in the Test2 table is present in the Test1
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
            }, "%s"
          );
          return result.Test1 == null;
        })()
        """
            .formatted(schemaName);

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

    // Age 10 is not present in test1
    test2.insert(row("id", 1, "name", "jan", "age", 10));
    // Should throw exception as age 11 is present in Test1 table:
    assertThrows(
        MolgenisException.class, () -> test2.insert(row("id", 2, "name", "jan", "age", 11)));
  }
}
