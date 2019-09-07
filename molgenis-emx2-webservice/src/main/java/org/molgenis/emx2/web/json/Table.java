package org.molgenis.emx2.web.json;

import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.utils.MolgenisException;

import java.util.ArrayList;
import java.util.Collection;

public class Table {
  private String name;
  private String[] pkey;
  private Collection<String[]> uniques = new ArrayList<>();
  private Collection<Column> columns = new ArrayList<>();

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

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String[] getPkey() {
    return pkey;
  }

  public void setPkey(String[] pkey) {
    this.pkey = pkey;
  }

  public Collection<String[]> getUniques() {
    return uniques;
  }

  public void setUniques(Collection<String[]> uniques) {
    this.uniques = uniques;
  }

  public Collection<Column> getColumns() {
    return columns;
  }

  public void setColumns(Collection<Column> columns) {
    this.columns = columns;
  }
}
