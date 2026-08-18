package org.molgenis.emx2.fairmapper.postprocessing;

import java.util.List;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.tablestore.InMemoryTableStore;

public class ResolveRefbackPostProcessor implements PostProcessor {

  private static final String SUBJECT = "_subject_";

  private final SchemaMetadata schema;

  public ResolveRefbackPostProcessor(SchemaMetadata schema) {
    this.schema = schema;
  }

  @Override
  public void process(InMemoryTableStore tableStore) {
    for (String tableName : tableStore.getTableNames()) {
      List<Column> columns =
          schema.getTableMetadata(tableName).getColumns().stream()
              .filter(Column::isRefback)
              .toList();

      if (columns.isEmpty()) {
        continue;
      }

      for (Row row : tableStore.readTable(tableName)) {
        for (Column column : columns) {
          resolveRefBack(tableStore, row, column);
        }
      }
    }
  }

  private void resolveRefBack(InMemoryTableStore tableStore, Row row, Column column) {
    if (!row.notNull(SUBJECT + column.getName())) {
      return;
    }

    if (column.isArray()) {
      handleMultiple(column, row, tableStore);
    } else {
      handleSingle(column, row, tableStore);
    }
  }

  private void handleMultiple(Column column, Row fromRow, InMemoryTableStore tableStore) {
    for (String referenceSubject : fromRow.getStringArray(SUBJECT + column.getName())) {
      TableMetadata table = schema.getTableMetadata(column.getRefTableName());
      Row referencing = TableStoreUtils.getRowForSubject(tableStore, table, referenceSubject);
      referencing.setString(SUBJECT + column.getRefBack(), fromRow.getString(SUBJECT));
    }
  }

  private void handleSingle(Column column, Row fromRow, InMemoryTableStore tableStore) {
    String referenceSubject = fromRow.getString(SUBJECT + column.getName());
    TableMetadata table = schema.getTableMetadata(column.getRefTableName());
    Row referencing = TableStoreUtils.getRowForSubject(tableStore, table, referenceSubject);
    referencing.setString(SUBJECT + column.getRefBack(), fromRow.getString(SUBJECT));
  }
}
