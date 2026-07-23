package org.molgenis.emx2.io.emx2;

import java.util.List;

public record MigrationPlan(
    List<TableRef> tableAdds,
    List<TableRef> tableDrops,
    List<TableRename> tableRenames,
    List<ColumnRef> columnAdds,
    List<ColumnRef> columnDrops,
    List<ColumnRename> columnRenames,
    List<ColumnAttributeChange> changes,
    List<String> errors,
    List<String> warnings) {

  public record TableRef(String table) {}

  public record TableRename(String fromTable, String toTable) {}

  public record ColumnRef(String table, String column) {}

  public record ColumnRename(String table, String fromColumn, String toColumn) {}

  public record ColumnAttributeChange(
      String table, String column, String attribute, String oldValue, String newValue) {}
}
