package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.datamodels.util.CompareTools;

class SqlRowProcessorTest {

  private final Database database = TestDatabaseFactory.getTestDatabase();

  @Test
  void autoIdGetsSkipped() {
    TableMetadata tableMetadata = table("Test", new Column("myCol").setType(ColumnType.AUTO_ID));

    final Row row = new Row("myCol", null);
    SqlRowProcessor computer = new SqlRowProcessor(database, tableMetadata.getColumns());
    computer.validateAndCompute(row);
    assertNull(row.getString("myCol"));
  }

  @Test
  void testWorkingValidationForEmailArray() {
    List<Column> columns = List.of(column("SPAM blocklist", ColumnType.EMAIL_ARRAY));
    Row row = row("SPAM blocklist", "bob@example.com,ros@example.com");

    SqlRowProcessor computer = new SqlRowProcessor(database, columns);
    assertDoesNotThrow(() -> computer.validateAndCompute(row));
  }

  @Test
  void shouldHandleComputedDependencies() {
    List<Column> columns =
        List.of(
            // Depends on C and B
            column("a").setComputed("c + b"),
            // Depends on C
            column("b").setComputed("c"),
            // Depends on nothing
            column("c"));

    Row row = row("c", "1");
    SqlRowProcessor computer = new SqlRowProcessor(database, columns);
    computer.validateAndCompute(row);
    CompareTools.assertEquals(row, row("c", "1", "b", "1", "a", "11"));
  }

  @Test
  void shouldKeepValueWhenVisibleDependsOnComputedDeclaredBefore() {
    List<Column> columns =
        List.of(
            column("name"),
            column("nameComputed").setComputed("name"),
            column("filler1"),
            column("filler2"),
            column("filler3"),
            column("otherNames").setVisible("nameComputed == 'Piet'"));

    Row row = row("name", "Piet", "otherNames", "OtherPiet");
    new SqlRowProcessor(database, columns).validateAndCompute(row);
    assertEquals("OtherPiet", row.getString("otherNames"));
  }

  @Test
  void shouldKeepValueWhenVisibleDependsOnComputedDeclaredAfter() {
    List<Column> columns =
        List.of(
            column("name"),
            column("otherNames").setVisible("nameComputed == 'Piet'"),
            column("filler1"),
            column("filler2"),
            column("filler3"),
            column("nameComputed").setComputed("name"));

    Row row = row("name", "Piet", "otherNames", "OtherPiet");
    new SqlRowProcessor(database, columns).validateAndCompute(row);
    assertEquals("OtherPiet", row.getString("otherNames"));
  }

  @Test
  void shouldClearValueWhenVisibleEvaluatesToFalse() {
    List<Column> columns =
        List.of(
            column("name"),
            column("otherNames").setVisible("nameComputed == 'Piet'"),
            column("filler1"),
            column("nameComputed").setComputed("name"));

    Row row = row("name", "Klaas", "otherNames", "OtherPiet");
    new SqlRowProcessor(database, columns).validateAndCompute(row);
    assertNull(row.getString("otherNames"));
  }

  @Test
  void shouldOrderOnIdentifierWhenColumnNamesAreMultiWord() {
    List<Column> columns =
        List.of(
            column("name"),
            column("other names").setVisible("nameComputed == 'Piet'"),
            column("filler1"),
            column("name computed").setComputed("name"));

    Row row = row("name", "Piet", "other names", "OtherPiet");
    new SqlRowProcessor(database, columns).validateAndCompute(row);
    assertEquals("OtherPiet", row.getString("other names"));
  }

  @Test
  void shouldHandleDependenciesBetweenNonComputedColumns() {
    List<Column> columns =
        List.of(
            column("otherNames").setVisible("name == 'Piet'"),
            column("filler1"),
            column("name").setDefaultValue("Piet"));

    Row row = row("otherNames", "OtherPiet");
    new SqlRowProcessor(database, columns).validateAndCompute(row);
    assertEquals("OtherPiet", row.getString("otherNames"));
  }

  @Test
  void shouldThrowOnCircularDependency() {
    List<Column> columns =
        List.of(
            // Depends on C and B
            column("a").setComputed("b"),
            // Depends on C
            column("b").setComputed("a"));

    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> new SqlRowProcessor(database, columns));
    assertEquals("Circular dependency between b and a", exception.getMessage());
  }

  @Test
  void shouldAllowRequiredExpressionsThatReferToEachOther() {
    List<Column> columns =
        List.of(
            column("id").setPkey(),
            column("email").setRequired("if (phone == null) 'email or phone required'"),
            column("phone").setRequired("if (email == null) 'email or phone required'"));

    SqlRowProcessor processor = new SqlRowProcessor(database, columns);

    assertDoesNotThrow(
        () -> processor.validateAndCompute(row("id", "1", "email", "jan@example.com")));
    assertDoesNotThrow(() -> processor.validateAndCompute(row("id", "2", "phone", "0612345678")));
    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> processor.validateAndCompute(row("id", "3")));
    assertTrue(exception.getMessage().contains("email or phone required"));
  }
}
