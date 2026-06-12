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
  // backing store for all parent table names (supports multiple/diamond inheritance)
  protected List<String> inheritNames = new ArrayList<>();
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
  // profiles to which this table belongs
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
      this.inheritNames = new ArrayList<>(metadata.getInheritNames());
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

    for (TableMetadata parent : getInheritedTables()) {
      // we create copies so we don't need worry on changes
      for (Column col : parent.getColumns()) {
        if (col.isSystemColumn()) {
          meta.put(col.getName(), col);
          // sorting of external schema is seperate from internal schema
        } else if (!Objects.equals(col.getTable().getSchemaName(), getSchemaName())) {
          external.putIfAbsent(col.getName(), col);
        } else {
          internal.putIfAbsent(col.getName(), col);
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

  private Set<String> allParentColumnNames() {
    Set<String> result = new HashSet<>();
    for (TableMetadata parent : getInheritedTables()) {
      result.addAll(parent.getColumnNames());
    }
    return result;
  }

  public List<Column> getNonInheritedColumns() {
    if (!getInheritNames().isEmpty()) {
      Set<String> parentColumnNames = allParentColumnNames();
      return this.columns.values().stream()
          .filter(c -> !parentColumnNames.contains(c.getName()))
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
    for (TableMetadata parent : getInheritedTables()) {
      for (Column pkey : parent.getPrimaryKeyColumns()) {
        result.computeIfAbsent(pkey.getName(), name -> new Column(pkey).setTable(this));
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
    if (!getInheritNames().isEmpty()) {
      Set<String> parentColumnNames = allParentColumnNames();
      return getColumnNames().stream()
          .filter(name -> !parentColumnNames.contains(name))
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
      Column col = parent.getColumn(name);
      if (col != null) return new Column(col.getTable(), col);
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
    for (Column col : column) {
      for (TableMetadata parent : getInheritedTables()) {
        if (parent.getColumn(col.getName()) != null && !col.isPrimaryKey()) {
          throw new MolgenisException(
              "Cannot add column '"
                  + getTableName()
                  + "."
                  + col.getName()
                  + "': exists in extended table '"
                  + parent.getTableName()
                  + "'");
        }
      }

      if (col.getPosition() == null) {
        col.setPosition(columns.size());
      }
      columns.put(col.getName(), new Column(this, col));
      col.setTable(this);
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

  public List<String> getInheritNames() {
    return Collections.unmodifiableList(inheritNames);
  }

  public List<String> getAllInheritNames() {
    List<String> result = new ArrayList<>();
    Set<String> seen = new LinkedHashSet<>();
    Set<String> onStack = new LinkedHashSet<>();
    collectAllInheritNames(result, seen, onStack);
    return result;
  }

  private void collectAllInheritNames(List<String> result, Set<String> seen, Set<String> onStack) {
    if (!onStack.add(getTableName())) {
      throw new MolgenisException(
          "Cyclic inheritance detected for table '" + getTableName() + "'. Cycle path: " + onStack);
    }
    if (seen.add(getTableName())) {
      result.add(getTableName());
    }
    for (TableMetadata parent : getInheritedTables()) {
      parent.collectAllInheritNames(result, seen, onStack);
    }
    onStack.remove(getTableName());
  }

  public TableMetadata setInheritNames(String... names) {
    return setInheritNames(names == null ? List.of() : List.of(names));
  }

  public TableMetadata setInheritNames(List<String> names) {
    inheritNames.clear();
    if (names != null) {
      for (String name : names) {
        if (name != null && !inheritNames.contains(name)) {
          inheritNames.add(name);
        }
      }
    }
    return this;
  }

  public List<TableMetadata> getInheritedTables() {
    if (inheritNames.isEmpty()) return Collections.emptyList();
    List<TableMetadata> result = new ArrayList<>();
    for (String parentName : inheritNames) {
      TableMetadata parent = resolveParentTable(parentName);
      if (parent != null) {
        result.add(parent);
      }
    }
    return result;
  }

  private TableMetadata resolveParentTable(String parentName) {
    if (getSchema() == null) return null;
    if (getImportSchema() != null && getSchema().getDatabase() != null) {
      if (getSchema().getDatabase().getSchema(getImportSchema()) == null) {
        throw new MolgenisException(
            "Cannot find schema '"
                + getImportSchema()
                + " for inheritance of table '"
                + parentName
                + "'");
      }
      if (getSchema().getDatabase().getSchema(getImportSchema()).getTable(parentName) == null) {
        throw new MolgenisException(
            "Cannot find table '" + parentName + "' for inheritance of table.");
      }
      return getSchema()
          .getDatabase()
          .getSchema(getImportSchema())
          .getTable(parentName)
          .getMetadata();
    } else {
      TableMetadata resolved = getSchema().getTableMetadata(parentName);
      if (resolved == null && getSchema().getDatabase() != null) {
        throw new MolgenisException(
            "Cannot inherit "
                + getSchemaName()
                + "."
                + parentName
                + ": not found"
                + " (declared parent of '"
                + getTableName()
                + "')");
      }
      return resolved;
    }
  }

  public void enableRowLevelSecurity() {
    throw new UnsupportedOperationException();
  }

  public String toString() {
    StringBuilder builder = new StringBuilder();
    String name = getTableName();
    List<String> parents = getInheritNames();
    if (!parents.isEmpty()) {
      if (getImportSchema() != null) {
        name +=
            " extends "
                + parents.stream()
                    .map(p -> getImportSchema() + "." + p)
                    .collect(Collectors.joining(", "));
      } else {
        name += " extends " + String.join(", ", parents);
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
    inheritNames = new ArrayList<>();
    importSchema = null;
  }

  public boolean exists() {
    return !getColumns().isEmpty();
  }

  public TableMetadata removeInherit() {
    this.inheritNames.clear();
    return this;
  }

  public String getSchemaName() {
    return getSchema().getName();
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
    for (TableMetadata subclass : getSubclassTables()) {
      result.addAll(subclass.getLocalColumns());
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

  public List<TableMetadata> getSubclassTables() {
    if (getSchema() == null) return Collections.emptyList();
    List<TableMetadata> result = new ArrayList<>();
    Set<String> emitted = new LinkedHashSet<>();
    collectSubclassTablesDeduped(result, emitted);
    return result;
  }

  private void collectSubclassTablesDeduped(List<TableMetadata> result, Set<String> emitted) {
    if (getSchema() == null) return;
    for (TableMetadata table : getSchema().getTables()) {
      if (table.getInheritNames().contains(getTableName())) {
        if (emitted.add(table.getTableName())) {
          result.add(table);
          table.collectSubclassTablesDeduped(result, emitted);
        }
      }
    }
  }

  public TableMetadata getRootTable() {
    List<TableMetadata> parents = getInheritedTables();
    if (parents.isEmpty()) return this;
    Map<String, TableMetadata> roots = new LinkedHashMap<>();
    for (TableMetadata parent : parents) {
      TableMetadata root = parent.getRootTable();
      roots.put(root.getTableName(), root);
    }
    if (roots.size() > 1) {
      throw new MolgenisException(
          "Inheritance DAG for table '"
              + getTableName()
              + "' has multiple roots: "
              + roots.keySet()
              + ". All parents must share a single common root.");
    }
    return roots.values().iterator().next();
  }

  public List<TableMetadata> getAncestorsRootFirst() {
    List<TableMetadata> result = new ArrayList<>();
    Set<String> visited = new LinkedHashSet<>();
    visited.add(getTableName());
    gatherAncestorsPostOrder(this, result, visited);
    return result;
  }

  private static void gatherAncestorsPostOrder(
      TableMetadata table, List<TableMetadata> result, Set<String> visited) {
    for (TableMetadata parent : table.getInheritedTables()) {
      if (visited.add(parent.getTableName())) {
        gatherAncestorsPostOrder(parent, result, visited);
        result.add(parent);
      }
    }
  }

  public List<Column> getDiscriminatorColumns() {
    return getColumns().stream().filter(Column::isDiscriminator).toList();
  }

  public void validateInheritance() {
    getAllInheritNames();
    getRootTable();
    Map<String, String> ownerByColumn = new LinkedHashMap<>();
    for (TableMetadata parent : getInheritedTables()) {
      for (Column col : parent.getNonInheritedColumns()) {
        if (col.isPrimaryKey()) continue;
        String previousOwner = ownerByColumn.put(col.getName(), parent.getTableName());
        if (previousOwner != null && !previousOwner.equals(parent.getTableName())) {
          throw new MolgenisException(
              "Cannot inherit in table '"
                  + getTableName()
                  + "': column '"
                  + col.getName()
                  + "' exists in multiple parents ('"
                  + previousOwner
                  + "' and '"
                  + parent.getTableName()
                  + "'). Column names must be unique across all parents.");
        }
      }
    }
  }
}
