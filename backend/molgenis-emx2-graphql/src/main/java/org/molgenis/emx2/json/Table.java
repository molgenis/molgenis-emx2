package org.molgenis.emx2.json;

import org.molgenis.emx2.TableMetadata;

import java.util.ArrayList;
import java.util.Collection;

public class Table {
  private String name;
  private String[] pkey;
  private String inherit;
  private String description;
  private Collection<String[]> unique = new ArrayList<>();
  private Collection<Column> columns = new ArrayList<>();

  public Table() {}

  public Table(TableMetadata tableMetadata) {
    this.name = tableMetadata.getTableName();
    this.inherit = tableMetadata.getInherit();
    this.description = tableMetadata.getDescription();
    for (org.molgenis.emx2.Column column : tableMetadata.getColumns()) {
      this.columns.add(new Column(column));
    }
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Collection<String[]> getUnique() {
    return unique;
  }

  public void setUnique(Collection<String[]> unique) {
    this.unique = unique;
  }

  public Collection<Column> getColumns() {
    return columns;
  }

  public void setColumns(Collection<Column> columns) {
    this.columns = columns;
  }

  public String[] getPkey() {
    return pkey;
  }

  public void setPkey(String[] pkey) {
    this.pkey = pkey;
  }

  public String getInherit() {
    return inherit;
  }

  public void setInherit(String inherit) {
    this.inherit = inherit;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
