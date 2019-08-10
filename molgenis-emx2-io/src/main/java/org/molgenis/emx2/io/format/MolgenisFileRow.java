package org.molgenis.emx2.io.format;

public class MolgenisFileRow {
  private String table;
  private String column;
  private String properties;
  private String description;

  public MolgenisFileRow(String table, String column, String properties) {
    this.table = table == null ? "" : table.trim();
    this.column = column == null ? "" : column.trim();
    this.properties = properties == null ? "" : properties.trim();
  }

  public MolgenisFileRow(String table, String column, String properties, String description) {
    this(table, column, properties);
    this.description = description == null ? "" : description.trim();
  }

  public String getTable() {
    return table;
  }

  public String getColumn() {
    return column;
  }

  public String getProperties() {
    return properties;
  }

  public String getDescription() {
    return description;
  }

  public String toString() {
    return String.format("{table='%s', column='%s', definitions='%s'}", table, column, properties);
  }
}
