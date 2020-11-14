package org.molgenis.emx2.json;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.molgenis.emx2.Setting;
import org.molgenis.emx2.TableMetadata;

public class Table {
  private String name;
  private String[] pkey;
  private String inherit;
  private String description;
  private String externalSchema;
  private Collection<String[]> unique = new ArrayList<>();
  private Collection<Column> columns = new ArrayList<>();
  private List<Setting> settings = new ArrayList<>();

  public Table() {
    // for json serialisation
  }

  public Table(TableMetadata tableMetadata) {
    this.name = tableMetadata.getTableName();
    this.inherit = tableMetadata.getInherit();
    this.description = tableMetadata.getDescription();
    this.settings =
        tableMetadata.getSettings().entrySet().stream()
            .map(entry -> new Setting(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
    for (org.molgenis.emx2.Column column : tableMetadata.getColumns()) {
      this.columns.add(new Column(column, tableMetadata));
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

  public List<Setting> getSettings() {
    return settings;
  }

  public void setSettings(List<Setting> settings) {
    this.settings = settings;
  }

  public String getExternalSchema() {
    return externalSchema;
  }

  public void setExternalSchema(String externalSchema) {
    this.externalSchema = externalSchema;
  }
}
