package org.molgenis.emx2.web.json;

import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.utils.MolgenisException;

import java.util.ArrayList;
import java.util.Collection;

public class Table {
  public String name;
  public String[] pkey;
  public Collection<String[]> uniques = new ArrayList<>();
  public Collection<Column> columns = new ArrayList<>();

  public Table() {}

  public Table(TableMetadata tableMetadata) {
    this.name = tableMetadata.getTableName();
    if (tableMetadata.getPrimaryKey().length > 1) {
      this.pkey = tableMetadata.getPrimaryKey();
    }
    for (String[] u : tableMetadata.getUniques()) {
      if (u.length > 1) {
        this.uniques.add(u);
      }
    }
    this.uniques = tableMetadata.getUniques();
    for (org.molgenis.emx2.Column column : tableMetadata.getColumns()) {
      this.columns.add(new Column(column));
    }
  }

  public TableMetadata getTableMetadata(SchemaMetadata s) throws MolgenisException {
    TableMetadata tm = new TableMetadata(s, name);
    tm.setPrimaryKey(pkey);
    for (String[] u : uniques) tm.addUnique(u);
    for (Column c : columns) {
      tm.addColumn(c.getColumnMetadata(tm));
    }
    return tm;
  }
}
