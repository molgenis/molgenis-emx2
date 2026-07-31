package org.molgenis.emx2.io.emx2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.io.emx2.Emx2.COLUMN_NAME;
import static org.molgenis.emx2.io.emx2.Emx2.TABLE_EXTENDS;
import static org.molgenis.emx2.io.emx2.Emx2.TABLE_NAME;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.SchemaMetadata;

/**
 * Locks Phase E6: a single-file CSV with interleaved root+module column rows produces an export
 * that preserves that interleaved order, and the order survives a second round-trip.
 */
class Emx2CrossModuleColumnOrderTest {

  private static final String ROOT = "Root";
  private static final String MODULE = "Mod";
  private static final String TABLE_TYPE_KEY = "tableType";
  private static final String MODULE_TYPE_VALUE = "MODULE";

  @Test
  void interleavedColumnOrderPreservedAcrossModuleBoundary() {
    List<Row> authoredRows = buildInterleavedAuthoredRows();

    SchemaMetadata firstImport = Emx2.fromRowList(authoredRows);
    List<Row> firstExport = columnRowsOnly(Emx2.toRowList(firstImport));

    assertInterleavedOrder(firstExport);

    SchemaMetadata secondImport = Emx2.fromRowList(Emx2.toRowList(firstImport));
    List<Row> secondExport = columnRowsOnly(Emx2.toRowList(secondImport));

    assertInterleavedOrder(secondExport);
  }

  @Test
  void explicitPositionInterleavedColumnOrderPreservedAcrossModuleBoundary() {
    List<Row> authoredRows = buildInterleavedAuthoredRowsWithExplicitPositions();

    SchemaMetadata firstImport = Emx2.fromRowList(authoredRows);
    List<Row> firstExport = columnRowsOnly(Emx2.toRowList(firstImport));

    assertInterleavedOrder(firstExport);
  }

  private List<Row> buildInterleavedAuthoredRows() {
    return List.of(
        tableRow(ROOT, null, false),
        tableRow(MODULE, ROOT, true),
        columnRow(ROOT, "colA"),
        columnRow(MODULE, "colX"),
        columnRow(ROOT, "colB"),
        columnRow(MODULE, "colY"));
  }

  private List<Row> buildInterleavedAuthoredRowsWithExplicitPositions() {
    return List.of(
        tableRow(ROOT, null, false),
        tableRow(MODULE, ROOT, true),
        columnRowWithPosition(ROOT, "colA", 0),
        columnRowWithPosition(MODULE, "colX", 1),
        columnRowWithPosition(ROOT, "colB", 2),
        columnRowWithPosition(MODULE, "colY", 3));
  }

  private Row tableRow(String tableName, String tableExtends, boolean isModule) {
    Row row = new Row();
    row.setString(TABLE_NAME, tableName);
    if (tableExtends != null) row.setString(TABLE_EXTENDS, tableExtends);
    if (isModule) row.setString(TABLE_TYPE_KEY, MODULE_TYPE_VALUE);
    return row;
  }

  private Row columnRow(String tableName, String columnName) {
    Row row = new Row();
    row.setString(TABLE_NAME, tableName);
    row.setString(COLUMN_NAME, columnName);
    return row;
  }

  private Row columnRowWithPosition(String tableName, String columnName, int position) {
    Row row = columnRow(tableName, columnName);
    row.setInt("position", position);
    return row;
  }

  private List<Row> columnRowsOnly(List<Row> rows) {
    return rows.stream().filter(row -> row.getString(COLUMN_NAME) != null).toList();
  }

  private void assertInterleavedOrder(List<Row> columnRows) {
    assertEquals(4, columnRows.size(), "Expected 4 column rows in export");
    assertColumnRow(columnRows.get(0), ROOT, "colA");
    assertColumnRow(columnRows.get(1), MODULE, "colX");
    assertColumnRow(columnRows.get(2), ROOT, "colB");
    assertColumnRow(columnRows.get(3), MODULE, "colY");
  }

  private void assertColumnRow(Row row, String expectedTable, String expectedColumn) {
    assertEquals(
        expectedTable,
        row.getString(TABLE_NAME),
        "Expected tableName=" + expectedTable + " for column " + expectedColumn);
    assertEquals(
        expectedColumn, row.getString(COLUMN_NAME), "Expected columnName=" + expectedColumn);
  }
}
