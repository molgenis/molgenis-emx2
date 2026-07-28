package org.molgenis.emx2.web;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.graphql.GraphqlConstants.ADMIN;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class TestValidationWithJavaScriptBindings {

  private static Database database;
  private static final String schemaName =
      TestValidationWithJavaScriptBindings.class.getSimpleName();
  private static Schema schema;
  private static Table table1;
  private static Table table2;

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(schemaName);

    database.setBindings(JavaScriptBindings.getBindingsForUser(ADMIN));

    // validation script that checks if the age inserted in the Test2 table is present in the Test1
    String validationScript =
        """
        (function () {
          let result = simplePostClient(
            `query Test1( $filter:Test1Filter, $orderby:[Test1orderby] ) {
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

    table1 =
        schema.create(
            table(
                "Test1",
                column("id").setPkey(),
                column("name"),
                column("age").setVisible("name").setType(ColumnType.INT)));
    table1.insert(row("id", 1, "name", "jan", "age", 11));

    table2 =
        schema.create(
            table(
                "Test2",
                column("id").setPkey(),
                column("name"),
                column("age")
                    .setValidation(validationScript)
                    .setVisible("name")
                    .setType(ColumnType.INT)));
  }

  @AfterAll
  public static void after() {
    database.dropSchema(schemaName);
  }

  @Test
  public void testValidationWithSimplePostClient_shouldSucceed() {
    // Age 10 is not present in test1
    table2.insert(row("id", 1, "name", "jan", "age", 10));
  }

  @Test
  public void testValidationWithSimplePostClient_shouldFail() {
    assertThrows(
        MolgenisException.class, () -> table2.insert(row("id", 2, "name", "jan", "age", 11)));
  }

  // The variables argument is converted from GraalJS values to plain Java (JavaScriptBindings
  // asMap/toJava); this passes every value shape that conversion distinguishes. Undeclared
  // variables are ignored by GraphQL, so only the conversion itself is exercised.
  @Test
  public void testSimplePostClientConvertsAllVariableShapes() {
    String validationScript =
        """
        (function () {
          let result = simplePostClient(
            `query Test1( $filter:Test1Filter ) {
               Test1( filter:$filter ) { name age }
            }`,
            {
              filter: { age: { equals: age } },
              extra: {
                int: 42,
                long: 2**40,
                decimal: 1.5,
                bool: true,
                string: 'txt',
                nothing: null,
                array: [1, 'a', [true]],
                nested: { x: { y: 2 } },
                symbol: Symbol('x')
              }
            }, "%s"
          );
          return result.Test1 == null;
        })()
        """
            .formatted(schemaName);
    Table table4 =
        schema.create(
            table(
                "Test4",
                column("id").setPkey(),
                column("age").setValidation(validationScript).setType(ColumnType.INT)));
    table4.insert(row("id", 1, "age", 10));
    assertThrows(MolgenisException.class, () -> table4.insert(row("id", 2, "age", 11)));
  }

  // A non-object variables argument falls back to an empty variables map
  @Test
  public void testSimplePostClientWithNonObjectVariables() {
    String validationScript =
        """
        (function () {
          let result = simplePostClient(
            `query { Test1 { name age } }`, 42, "%s"
          );
          return result.Test1 != null;
        })()
        """
            .formatted(schemaName);
    Table table5 =
        schema.create(
            table(
                "Test5",
                column("id").setPkey(),
                column("age").setValidation(validationScript).setType(ColumnType.INT)));
    table5.insert(row("id", 1, "age", 10));
  }

  // Without arguments query/variables/schemaId fall back to null/empty; the schema lookup then
  // rejects the null schemaId, so the validation script fails instead of NPE-ing.
  @Test
  public void testSimplePostClientWithoutArgumentsFailsValidation() {
    Table table3 =
        schema.create(
            table(
                "Test3",
                column("id").setPkey(),
                column("age")
                    .setValidation("(function () { simplePostClient(); return true; })()")
                    .setType(ColumnType.INT)));
    assertThrows(MolgenisException.class, () -> table3.insert(row("id", 1, "age", 1)));
  }
}
