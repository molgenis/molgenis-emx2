package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

public class TestValidationWithJavaScriptBindings {

  private static final String schemaName =
      TestValidationWithJavaScriptBindings.class.getSimpleName();
  private static GraphqlSession session;

  @BeforeAll
  public static void setup() {
    session = new GraphqlSession("admin");
    Database database = session.getDatabase();
    Schema schema = database.dropCreateSchema(schemaName);
    JavaScriptBindings.getBindingsForSession(session);

    Table table1 =
        schema.create(
            table(
                "Test1",
                column("id").setPkey(),
                column("name"),
                column("age").setVisible("name").setType(ColumnType.INT)));
    table1.insert(row("id", 1, "name", "jan", "age", 11));
  }

  @Test
  public void testValidationWithSimplePostClient_shouldSucceed() {
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

    Table table2 =
        session
            .getDatabase()
            .getSchema(schemaName)
            .create(
                table(
                    "Test2",
                    column("id").setPkey(),
                    column("name"),
                    column("age")
                        .setValidation(validationScript)
                        .setVisible("name")
                        .setType(ColumnType.INT)));
    // Age 10 is not present in test1
    table2.insert(row("id", 1, "name", "jan", "age", 10));

    assertThrows(
        MolgenisException.class, () -> table2.insert(row("id", 2, "name", "jan", "age", 11)));
  }
}
