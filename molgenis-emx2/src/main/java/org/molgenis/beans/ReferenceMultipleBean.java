package org.molgenis.beans;

import org.molgenis.*;

public class ReferenceMultipleBean implements ReferenceMultiple {
  Table table;
  Type type;
  String[] nameArray;

  public ReferenceMultipleBean(Table table, Type type, String[] nameArray) {
    this.table = table;
    this.type = type;
    this.nameArray = nameArray;
  }

  @Override
  public Table to(String toTable, String... toColumn) throws MolgenisException {
    if (nameArray == null || nameArray.length != toColumn.length)
      throw new MolgenisException("Ref must have same name of names as toColumns");

    for (int i = 0; i < nameArray.length; i++) {
      table.addColumn(
          new ColumnMetadata(table, nameArray[i], type).setReference(toTable, toColumn[i]));
    }

    return table;
  }

  @Override
  public Table to(String toTable) throws MolgenisException {
    String[] keys = table.getPrimaryKey();
    return to(toTable, keys);
  }

  public Type getType() {
    return this.type;
  }

  public Table getTable() {
    return this.table;
  }

  public String[] getNameArray() {
    return this.nameArray;
  }
}
