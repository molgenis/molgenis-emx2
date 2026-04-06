package org.molgenis.emx2;

import static org.jooq.impl.DSL.name;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.utils.TypeUtils.convertToPascalCase;

import java.util.*;
import java.util.stream.Collectors;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.impl.DSL;

public class TableMetadata extends HasLabelsDescriptionsAndSettings<TableMetadata>
    implements Comparable {

  public static final String TABLE_NAME_MESSAGE =
      ": Table name must start with a letter, followed by zero or more letters, numbers, spaces or underscores. A space immediately before or after an underscore is not allowed. The character limit is 31.";
  public static final String SCHEMA_NAME_MESSAGE =
      ": Schema name must start with a letter, followed by zero or more letters, numbers, spaces, dashes or underscores. A space immediately before or after an underscore is not allowed. The character limit is 31.";
  // if a table extends another table (optional)
  private String[] inheritNames = null;
  // to allow indicate that a table should be dropped
  protected boolean drop = false;
  // for refenence to another schema (rare use)
  protected String importSchema = null;
  // columns of the table (required)
  protected Map<String, Column> columns = new LinkedHashMap<>();
  // link to the schema this table is part of (optional)
  private SchemaMetadata schema;
  // name unique within this schema (required)
  protected String tableName;
  // old name, useful for alter table
  private String oldName;
  // use to classify the table, influences display, import, export, etc
  private TableType tableType = TableType.DATA;
  // table semantics, typically an ontology URI
  private String[] semantics = null;
  private String[] profiles;

  public String[] getSemantics() {
    return semantics;
  }

  public TableMetadata setSemantics(String... semantics) {
    this.semantics = semantics;
    return this;
  }

  public String[] getProfiles() {
    return profiles;
  }

  public TableMetadata setProfiles(String... profiles) {
    this.profiles = profiles;
    return this;
  }

  public TableMetadata(String tableName) {
    this.tableName = validateName(tableName);
  }

  private String validateName(String tableName) {
    // max length 31 because of Excel
    // we allow only graphql compatible names PLUS spaces
    if (!tableName.matches(Constants.TABLE_NAME_REGEX)) {
      throw new MolgenisException("Invalid table name '" + tableName + TABLE_NAME_MESSAGE);
    }
    return tableName.trim();
  }

  public TableMetadata(SchemaMetadata schema, String tableName) {
    this(tableName);
    this.schema = schema;
  }

  public TableMetadata(SchemaMetadata schema, TableMetadata metadata) {
    this.clearCache();
    this.schema = schema;
    this.sync(metadata);
  }

  public static TableMetadata table(String tableName) {
    return new TableMetadata(tableName);
  }

  public static TableMetadata table(String tableName, Column... columns) {
    TableMetadata tm = new TableMetadata(tableName);
    for (Column c : columns) {
      tm.add(c);
    }
    return tm;
  }

  public void sync(TableMetadata metadata) {
    // skip if same object!
    if (this != metadata) {
      clearCache();
      this.tableName = metadata.getTableName();
      this.labels = metadata.getLabels();
      this.descriptions = metadata.getDescriptions();
      this.oldName = metadata.getOldName();
      this.setSettingsWithoutReload(metadata.getSettings());
      for (Column c : metadata.columns.values()) {
        this.columns.put(c.getName(), new Column(this, c));
      }
      this.inheritNames = metadata.getInheritNames();
      this.importSchema = metadata.getImportSchema();
      this.semantics = metadata.getSemantics();
      this.profiles = metadata.getProfiles();
      this.tableType = metadata.getTableType();
    }
  }

  public String getTableName() {
    return tableName;
  }

  public String getIdentifier() {
    return convertToPascalCase(getTableName());
  }

  public SchemaMetadata getSchema() {
    return schema;
  }

  public void setSchema(SchemaMetadata schemaMetadata) {
    this.schema = schemaMetadata;
  }

  public List<Column> getColumns() {
    // we want to sort on position,
    // first external schema (because their positions local to that schema)
    // last we attach the 'meta
    Map<String, Column> external = new LinkedHashMap<>(); // external schema has own ordering
    Map<String, Column> internal = new LinkedHashMap<>();
    Map<String, Column> meta = new LinkedHashMap<>();

    if (!getInheritedTables().isEmpty()) {
      // we create copies so we don't need worry on changes
      for (TableMetadata inheritedTable : getInheritedTables()) {
        for (Column col : inheritedTable.getColumns()) {
          if (col.isSystemColumn()) {
            meta.put(col.getName(), col);
            // sorting of external schema is seperate from internal schema
          } else if (!Objects.equals(col.getTable().getSchemaName(), getSchemaName())) {
            external.put(col.getName(), col);
          } else {
            internal.put(col.getName(), col);
          }
        }
      }
    }

    // ignore primary key from child class because that is same as in inheritedTable
    for (Column col : getLocalColumns()) {
      if (!internal.containsKey(col.getName()) && !external.containsKey(col.getName())) {
        if (col.isSystemColumn()) {
          meta.put(col.getName(), new Column(col.getTable(), col));
          // sorting of external schema is seperate from internal schema
        } else {
          internal.put(col.getName(), new Column(col.getTable(), col));
        }
      }
    }

    // sort by position
    List<Column> externalList = new ArrayList<>(external.values());
    List<Column> internalList = new ArrayList<>(internal.values());
    List<Column> metaList = new ArrayList<>(meta.values());

    Collections.sort(externalList);
    Collections.sort(internalList);
    Collections.sort(metaList);

    List<Column> finalResult = new ArrayList<>();
    finalResult.addAll(externalList);
    finalResult.addAll(internalList);
    finalResult.addAll(metaList);
    return finalResult;
  }

  public List<Column> getColumnsWithoutHeadings() {
    return this.getColumns().stream().filter(c -> !c.isHeading()).toList();
  }

  public List<Column> getColumnsForProfiles(String[] activeProfiles) {
    return getColumns().stream()
        .filter(c -> ProfileUtils.matchesActiveProfiles(c.getProfiles(), activeProfiles))
        .toList();
  }

  public List<Column> getNonInheritedColumnsForProfiles(String[] activeProfiles) {
    return getNonInheritedColumns().stream()
        .filter(c -> ProfileUtils.matchesActiveProfiles(c.getProfiles(), activeProfiles))
        .toList();
  }

  public List<String> getPrimaryKeys() {
    List<String> primaryKey = new ArrayList<>();
    for (Column c : getColumns()) {
      if (c.getKey() == 1) {
        primaryKey.add(c.getName());
      }
    }
    return primaryKey;
  }

  public List<Column> getDownloadColumnNames() {
    List<Column> list = new ArrayList<>();
    for (Column column : getColumns()) {
      if (!column.isHeading()) {
        if (column.isFile()) {
          list.add(column(column.getName()));
          list.add(column(column.getName() + "_filename"));
        } else {
          list.add(column);
        }
      }
    }
    return getExpandedColumns(list);
  }

  public List<Column> getMutationColumns() {
    return getExpandedColumns(getStoredColumns());
  }

  /** returns columns including the nested composite key columns, needed to create the table */
  public List<Column> getExpandedColumns(List<Column> columns) {
    Map<String, Column> result =
        new LinkedHashMap<>(); // overlapping references can lead to duplicates
    for (Column c : columns) {
      if (c.isFile()) {
        result.put(c.getName(), new Column(c.getTable(), c.getName()));
        result.put(
            c.getName() + "_contents",
            new Column(c.getTable(), c.getName() + "_contents").setType(FILE));
        result.put(c.getName() + "_mimetype", new Column(c.getTable(), c.getName() + "_mimetype"));
        result.put(c.getName() + "_filename", new Column(c.getTable(), c.getName() + "_filename"));
        result.put(
            c.getName() + "_extension", new Column(c.getTable(), c.getName() + "_extension"));
        result.put(
            c.getName() + "_size", new Column(c.getTable(), c.getName() + "_size").setType(INT));
      } else
      // expand composite keys if necessary
      if (c.isReference()) {
        for (Reference ref : c.getReferences()) {
          if (!ref.isOverlapping()) { // only add overlapping once
            // use old name to find original column
            result.put(ref.getName(), ref.toPrimitiveColumn().setOldName(c.getName()));
          }
        }
      } else {
        result.put(c.getName(), c);
      }
    }
    return new ArrayList<>(result.values());
  }

  public List<Column> getNonInheritedColumns() {
    if (getInheritNames() != null && getInheritNames().length > 0) {
      Set<String> inheritedColumnNames = new HashSet<>();
      for (TableMetadata parent : getInheritedTables()) {
        inheritedColumnNames.addAll(parent.getColumnNames());
      }
      return this.columns.values().stream()
          .filter(c -> !inheritedColumnNames.contains(c.getName()))
          .collect(Collectors.toList());
    } else {
      return new ArrayList<>(this.columns.values());
    }
  }

  public List<Column> getStoredColumns() {
    return getLocalColumns().stream()
        .filter(c -> !c.isHeading() && !c.isRefback())
        .collect(Collectors.toList());
  }

  public List<Column> getLocalColumns() {
    Map<String, Column> result = new LinkedHashMap<>();
    // get primary key from first parent (all parents in diamond share same root PK), always first
    if (!getInheritedTables().isEmpty()) {
      for (Column pkey : getInheritedTables().get(0).getPrimaryKeyColumns()) {
        // rewrite metadata to point to current table instead of parent table
        result.put(pkey.getName(), new Column(pkey).setTable(this));
      }
    }

    // get all implemented columns (keep superclass because of type)
    List<Column> columnList =
        new ArrayList<>(
            columns.values().stream()
                .filter(c -> !c.isSystemColumn())
                .collect(Collectors.toList()));
    Collections.sort(columnList);

    // add meta behind non-meta
    List<Column> metaList =
        new ArrayList<>(
            columns.values().stream().filter(c -> c.isSystemColumn()).collect(Collectors.toList()));
    columnList.addAll(metaList);

    for (Column c : columnList) {
      if (!result.containsKey(c.getName())) {
        result.put(c.getName(), c);
      }
    }

    return new ArrayList<>(result.values());
  }

  public List<String> getColumnNames() {
    return getColumns().stream().map(c -> c.getName()).collect(Collectors.toList());
  }

  public List<String> getLocalColumnNames() {
    List<String> result = new ArrayList<>();
    for (Column c : getLocalColumns()) {
      result.add(c.getName());
    }
    return result;
  }

  public List<String> getNonInheritedColumnNames() {
    // skip inherited
    if (getInheritNames() != null && getInheritNames().length > 0) {
      List<TableMetadata> parents = getInheritedTables();
      return getColumnNames().stream()
          .filter(
              c -> {
                for (TableMetadata parent : parents) {
                  if (parent.getColumn(c) != null) return false;
                }
                return true;
              })
          .collect(Collectors.toList());
    }
    return getColumnNames();
  }

  public List<String> getNonReferencingColumnNames() {
    List<String> result = new ArrayList<>();
    for (Column c : getLocalColumns()) {
      if (!c.isReference()) {
        result.add(c.getName());
      }
    }
    return result;
  }

  public Column getColumn(String name) {
    if (columns.containsKey(name)) return new Column(this, columns.get(name));
    for (TableMetadata parent : getInheritedTables()) {
      Column c = parent.getColumn(name);
      if (c != null) return new Column(c.getTable(), c);
    }
    return null;
  }

  public Column getColumnByIdentifier(String identifier) {
    Column column =
        columns.values().stream()
            .filter(c -> c.getIdentifier().equals(identifier))
            .findFirst()
            .orElse(null);
    if (column == null) {
      for (TableMetadata parent : getInheritedTables()) {
        column = parent.getColumnByIdentifier(identifier);
        if (column != null) break;
      }
    }
    return column;
  }

  public TableMetadata add(Column... column) {
    for (Column c : column) {
      if (c.getColumnType() == ColumnType.EXTENSION
          || c.getColumnType() == ColumnType.EXTENSION_ARRAY) {
        if (getInheritNames() != null && getInheritNames().length > 0) {
          throw new MolgenisException(
              "Cannot add EXTENSION/EXTENSION_ARRAY column '"
                  + getTableName()
                  + "."
                  + c.getName()
                  + "': profile columns must be on the root table, not on a subclass");
        }
      }
      if (!getInheritedTables().isEmpty() && !c.isPrimaryKey()) {
        for (TableMetadata parent : getInheritedTables()) {
          Column existing = parent.getColumn(c.getName());
          if (existing != null) {
            throw new MolgenisException(
                "Cannot add column '"
                    + getTableName()
                    + "."
                    + c.getName()
                    + "': exists in extended table '"
                    + existing.getTableName()
                    + "'");
          }
        }
      }

      if (c.getPosition() == null) {
        c.setPosition(columns.size());
      }
      columns.put(c.getName(), new Column(this, c));
      c.setTable(this);
    }
    return this;
  }

  public TableMetadata alterColumn(Column column) {
    return this.alterColumn(column.getName(), column);
  }

  public TableMetadata alterColumn(String name, Column column) {
    // retain position
    if (column.getPosition() == null) {
      column.setPosition(columns.get(name).getPosition());
    }
    // remove the old
    columns.remove(name);
    // add the new
    columns.put(column.getName(), new Column(this, column));
    column.setTable(this);
    return this;
  }

  public void dropColumn(String name) {
    if (Arrays.asList(getPrimaryKeys()).contains(name))
      throw new MolgenisException("Remove column failed: Column is primary key");
    if (columns.get(name) == null)
      throw new MolgenisException("Remove column failed: Column '" + name + "' unknown");
    columns.remove(name);
  }

  public String[] getInheritNames() {
    return this.inheritNames;
  }

  public List<String> getAllInheritNames() {
    List<String> result = new ArrayList<>();
    result.add(this.getTableName());
    for (TableMetadata parent : getInheritedTables()) {
      for (String name : parent.getAllInheritNames()) {
        if (!result.contains(name)) {
          result.add(name);
        }
      }
    }
    return result;
  }

  public TableMetadata setInheritNames(String... otherTable) {
    if (otherTable == null || otherTable.length == 0 || otherTable[0] == null) {
      this.inheritNames = null;
    } else {
      this.inheritNames = otherTable;
    }
    return this;
  }

  public List<TableMetadata> getInheritedTables() {
    List<TableMetadata> result = new ArrayList<>();
    if (inheritNames != null && inheritNames.length > 0 && getSchema() != null) {
      for (String name : inheritNames) {
        TableMetadata parent = resolveTable(name);
        if (parent != null) {
          result.add(parent);
        } else if (getSchema().getDatabase() != null) {
          throw new MolgenisException(
              "Cannot find table '" + name + "' for inheritance of table '" + getTableName() + "'");
        }
      }
    }
    return result;
  }

  private TableMetadata resolveTable(String tableName) {
    if (getImportSchema() != null && getSchema().getDatabase() != null) {
      if (getSchema().getDatabase().getSchema(getImportSchema()) == null) {
        throw new MolgenisException(
            "Cannot find schema '"
                + getImportSchema()
                + "' for inheritance of table '"
                + tableName
                + "'");
      }
      if (getSchema().getDatabase().getSchema(getImportSchema()).getTable(tableName) == null) {
        throw new MolgenisException(
            "Cannot find table '"
                + tableName
                + "' for inheritance of table '"
                + getTableName()
                + "'");
      }
      return getSchema()
          .getDatabase()
          .getSchema(getImportSchema())
          .getTable(tableName)
          .getMetadata();
    } else {
      return getSchema().getTableMetadata(tableName);
    }
  }

  public void enableRowLevelSecurity() {
    throw new UnsupportedOperationException();
  }

  public String toString() {
    StringBuilder builder = new StringBuilder();
    String name = getTableName();
    if (getInheritNames() != null && getInheritNames().length > 0) {
      if (getImportSchema() != null) {
        name += " extends " + getImportSchema() + "." + String.join(",", getInheritNames());
      } else {
        name += " extends " + String.join(",", getInheritNames());
      }
    }
    builder.append("TABLE(").append(name).append("){");
    for (Column c : getColumns()) {
      builder.append("\n\t").append(c.toString());
    }
    builder.append("\n}");
    return builder.toString();
  }

  public void clearCache() {
    columns = new LinkedHashMap<>();
    inheritNames = null;
    importSchema = null;
  }

  public boolean exists() {
    return !getColumns().isEmpty();
  }

  public TableMetadata removeInheritNames() {
    this.inheritNames = null;
    return this;
  }

  public String getSchemaName() {
    return getSchema().getName();
  }

  public String getQualifiedName() {
    return getSchemaName() + "." + getTableName();
  }

  public List<Column> getPrimaryKeyColumns() {
    return getKey(1);
  }

  public List<Column> getKey(int key) {
    List<Column> keyColumns = new ArrayList<>();
    for (Column c : getStoredColumns()) {
      if (c.getKey() == key) {
        keyColumns.add(c);
      }
    }
    return keyColumns;
  }

  public List<Field<?>> getKeyFields(int key) {
    // references might be overlapping so need to deduplicate via this map
    Map<String, Field<?>> result = new LinkedHashMap<>();
    for (Column c : getKey(key)) {
      if (c.isReference()) {
        for (Reference ref : c.getReferences()) {
          result.put(ref.getName(), ref.getJooqField());
        }
      } else {
        result.put(c.getName(), c.getJooqField());
      }
    }
    return new ArrayList<>(result.values());
  }

  public Map<Integer, List<String>> getKeys() {
    Map<Integer, List<String>> keys = new LinkedHashMap<>();
    for (Column c : getStoredColumns()) {
      if (c.getKey() > 0) {
        if (keys.get(c.getKey()) == null) {
          keys.put(c.getKey(), new ArrayList<>());
        }
        keys.get(c.getKey()).add(c.getName());
      }
    }
    return keys;
  }

  public void removeKey(int key) {
    for (Column c : this.getStoredColumns()) {
      if (c.getKey() == key) {
        c.removeKey();
      }
    }
  }

  public Column getLocalColumn(String name) {
    return columns.get(name);
  }

  public org.jooq.Table<Record> getJooqTable() {
    return DSL.table(name(getSchemaName(), getTableName()));
  }

  public List<Field<?>> getPrimaryKeyFields() {
    return getKeyFields(1);
  }

  public String getImportSchema() {
    return importSchema;
  }

  public TableMetadata setImportSchema(String importSchema) {
    this.importSchema = importSchema;
    return this;
  }

  public String getOldName() {
    return oldName;
  }

  public TableMetadata setOldName(String oldName) {
    this.oldName = oldName;
    return this;
  }

  public boolean isDrop() {
    return drop;
  }

  public void drop() {
    this.drop = true;
  }

  public TableMetadata alterName(String name) {
    this.tableName = validateName(tableName);
    return this;
  }

  @Override
  public int compareTo(Object o) {
    if (o instanceof TableMetadata) {
      return getTableName().compareTo(((TableMetadata) o).getTableName());
    }
    return 0;
  }

  public Table getTable() {
    return getSchema().getDatabase().getSchema(this.getSchemaName()).getTable(getTableName());
  }

  public List<Column> getColumnsWithoutMetadata() {
    return getColumns().stream().filter(c -> !c.isSystemColumn()).collect(Collectors.toList());
  }

  public TableType getTableType() {
    return tableType;
  }

  public TableMetadata setTableType(TableType tableType) {
    this.tableType = tableType;
    return this;
  }

  public String getLabel() {
    if (this.getLabels().get("en") != null && !this.getLabels().get("en").trim().equals("")) {
      return this.getLabels().get("en");
    } else {
      return this.getTableName();
    }
  }

  public String getDescription() {
    if (this.getDescriptions().get("en") != null
        && !this.getDescriptions().get("en").trim().equals("")) {
      return this.getDescriptions().get("en");
    } else {
      return null;
    }
  }

  public List<Column> getColumnsIncludingSubclasses() {
    // get all tables in current schema that inherit this
    List<Column> result = new ArrayList<>();
    result.addAll(this.getColumns());
    result.addAll(getColumnsFromSubclasses());
    return result;
  }

  public List<Column> getColumnsIncludingSubclassesExcludingHeadings() {
    return getColumnsIncludingSubclasses().stream().filter(c -> !c.isHeading()).toList();
  }

  private List<Column> getColumnsFromSubclasses() {
    List<Column> result = new ArrayList<>();
    for (TableMetadata table : getSubclassTables()) {
      result.addAll(table.getLocalColumns());
      result.addAll(table.getColumnsFromSubclasses());
    }
    return result;
  }

  public Column getColumnByNameIncludingSubclasses(String columnName) {
    return getColumnsIncludingSubclasses().stream()
        .filter(c -> c.getName().equals(columnName))
        .findFirst()
        .orElseGet(() -> null);
  }

  public Column getColumnByIdIncludingSubclasses(String columnId) {
    return getColumnsIncludingSubclasses().stream()
        .filter(c -> c.getIdentifier().equals(columnId))
        .findFirst()
        .orElseGet(() -> null);
  }

  public boolean hasColumnInParent(String columnName) {
    for (TableMetadata parent : getInheritedTables()) {
      if (parent.getColumn(columnName) != null) return true;
    }
    return false;
  }

  public Column getProfileColumn() {
    TableMetadata root = getRootTable();
    for (Column c : root.getLocalColumns()) {
      if (c.getColumnType() == ColumnType.EXTENSION
          || c.getColumnType() == ColumnType.EXTENSION_ARRAY) {
        return c;
      }
    }
    return null;
  }

  public List<TableMetadata> getSubclassTables() {
    LinkedHashSet<TableMetadata> result = new LinkedHashSet<>();
    for (TableMetadata table : getSchema().getTables()) {
      if (table.getInheritNames() != null) {
        for (String inherit : table.getInheritNames()) {
          if (this.getTableName().equals(inherit)) {
            result.add(table);
            result.addAll(table.getSubclassTables());
            break;
          }
        }
      }
    }
    return new ArrayList<>(result);
  }

  public TableMetadata getRootTable() {
    TableMetadata table = this;
    while (!table.getInheritedTables().isEmpty()) {
      table = table.getInheritedTables().get(0);
    }
    return table;
  }

  public List<TableMetadata> getAllInheritedTables() {
    List<TableMetadata> result = new ArrayList<>();
    for (TableMetadata parent : getInheritedTables()) {
      result.add(parent);
      result.addAll(parent.getAllInheritedTables());
    }
    return result;
  }
}
