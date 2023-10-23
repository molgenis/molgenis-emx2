package org.molgenis.emx2.json;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.TableType;

public class Table {
  private String name;
  private String oldName;
  private boolean drop;
  private String[] pkey;
  private String inherit;
  private List<LanguageValue> labels = new ArrayList<>();
  private List<LanguageValue> descriptions = new ArrayList<>();
  private String externalSchema;
  private Collection<String[]> unique = new ArrayList<>();
  private Collection<Column> columns = new ArrayList<>();
  private List<Setting> settings = new ArrayList<>();
  private String[] semantics;
  private String id;
  private TableType tableType;

  public Table() {
    // for json serialisation
  }

  public Table(SchemaMetadata schema, TableMetadata tableMetadata) {
    this(schema, tableMetadata, false);
  }

  public Table(SchemaMetadata schema, TableMetadata tableMetadata, boolean minimal) {
    this.name = tableMetadata.getTableName();
    this.labels =
        tableMetadata.getLabels().entrySet().stream()
            .map(entry -> new LanguageValue(entry.getKey(), entry.getValue()))
            .toList();
    this.id = tableMetadata.getIdentifier();
    this.drop = tableMetadata.isDrop();
    this.oldName = tableMetadata.getOldName();
    this.inherit = tableMetadata.getInheritedTable().getIdentifier();
    this.descriptions =
        tableMetadata.getDescriptions().entrySet().stream()
            .map(entry -> new LanguageValue(entry.getKey(), entry.getValue()))
            .toList();
    this.semantics = tableMetadata.getSemantics();
    this.settings =
        tableMetadata.getSettings().entrySet().stream()
            .map(entry -> new Setting(entry.getKey(), entry.getValue()))
            .toList();
    this.externalSchema = tableMetadata.getSchemaName();
    for (org.molgenis.emx2.Column column : tableMetadata.getColumns()) {
      this.columns.add(new Column(column, tableMetadata, minimal));
    }
    this.tableType = tableMetadata.getTableType();
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getOldName() {
    return oldName;
  }

  public void setOldName(String oldName) {
    this.oldName = oldName;
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

  public List<LanguageValue> getDescriptions() {
    return descriptions;
  }

  public void setDescriptions(List<LanguageValue> descriptions) {
    this.descriptions = descriptions;
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

  public String[] getSemantics() {
    return semantics;
  }

  public void setSemantics(String[] semantics) {
    this.semantics = semantics;
  }

  public boolean getDrop() {
    return drop;
  }

  public void setDrop(boolean drop) {
    this.drop = drop;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public TableType getTableType() {
    return tableType;
  }

  public void setTableType(TableType tableType) {
    this.tableType = tableType;
  }

  public List<LanguageValue> getLabels() {
    return labels;
  }

  public void setLabels(List<LanguageValue> labels) {
    this.labels = labels;
  }
}
