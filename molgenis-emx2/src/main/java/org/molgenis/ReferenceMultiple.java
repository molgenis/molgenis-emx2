package org.molgenis;

import org.molgenis.utils.MolgenisException;

public class ReferenceMultiple {
  TableMetadata table;
  Type type;
  String[] nameArray;

  public ReferenceMultiple(TableMetadata table, Type type, String[] nameArray) {
    this.table = table;
    this.type = type;
    this.nameArray = nameArray;
  }

  public TableMetadata to(String toTable, String... toColumn) throws MolgenisException {
    if (nameArray == null || nameArray.length != toColumn.length)
      throw new MolgenisException(
          "invalid_foreign_key",
          "Multiple reference must have same length of names as toColumns",
          "TODO error detail");

    for (int i = 0; i < nameArray.length; i++) {
      table.addColumn(new Column(table, nameArray[i], type).setReference(toTable, toColumn[i]));
    }

    return table;
  }

  public TableMetadata to(String toTable) throws MolgenisException {
    String[] keys = table.getPrimaryKey();
    return to(toTable, keys);
  }

  public Type getType() {
    return this.type;
  }

  public TableMetadata getTable() {
    return this.table;
  }

  public String[] getNameArray() {
    return this.nameArray;
  }
}
