package org.molgenis.emx2.io.format;

public class MolgenisFileRow {
  private String table;
  private String column;
  private String definition;
  private String description;

  public MolgenisFileRow(String table, String column, String definition) {
    this.table = table == null ? "" : table.trim();
    this.column = column == null ? "" : column.trim();
    this.definition = definition == null ? "" : definition.trim();
  }

  public MolgenisFileRow(String table, String column, String definition, String description) {
    this(table, column, definition);
    this.description = description == null ? "" : description.trim();
  }

  public String getTable() {
    return table;
  }

  public String getColumn() {
    return column;
  }

  public String getDefinition() {
    return definition;
  }

  public String getDescription() {
    return description;
  }

  public String toString() {
    return String.format("{table='%s', column='%s', definitions='%s'}", table, column, definition);
  }
}
