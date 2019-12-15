package org.molgenis.emx2;

import org.molgenis.emx2.utils.MolgenisException;

public class ReferenceMultiple {
  TableMetadata table;
  ColumnType columnType;
  String[] nameArray;

  public ReferenceMultiple(TableMetadata table, ColumnType columnType, String[] nameArray) {
    this.table = table;
    this.columnType = columnType;
    this.nameArray = nameArray;
  }

  public TableMetadata to(String toTable, String... toColumn) {
    if (nameArray == null || nameArray.length != toColumn.length)
      throw new MolgenisException("Multiple reference must have same length of names as toColumns");

    for (int i = 0; i < nameArray.length; i++) {
      table.addColumn(
          new Column(table, nameArray[i], columnType).setReference(toTable, toColumn[i]));
    }

    return table;
  }

  public TableMetadata to(String toTable) {
    String[] keys = table.getPrimaryKey();
    return to(toTable, keys);
  }

  public ColumnType getColumnType() {
    return this.columnType;
  }

  public TableMetadata getTable() {
    return this.table;
  }

  public String[] getNameArray() {
    return this.nameArray;
  }
}
