package org.molgenis.emx2.io.tablestore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.TableType;

public class TableStoreForProfile implements TableStore {

  Schema schema;

  public TableStoreForProfile(Schema schema, String template, Boolean includeDemoData) {
    this.schema = schema;
  }

  @Override
  public void writeTable(String name, List<String> columnNames, Iterable<Row> rows) {}

  @Override
  public Iterable<Row> readTable(String name) {
    return null;
  }

  @Override
  public void processTable(String name, RowProcessor processor) {}

  @Override
  public boolean containsTable(String name) {
    return false;
  }

  @Override
  public Collection<String> getTableNames() {
    List<String> tablesToUpdate = new ArrayList<>();
    for (TableMetadata tableMetadata : schema.getMetadata().getTables()) {
      if (tableMetadata.getTableType().equals(TableType.DATA)) {
        tablesToUpdate.add(tableMetadata.getTableName());
      }
    }
    return tablesToUpdate;
  }
}
