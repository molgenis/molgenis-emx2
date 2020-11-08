package org.molgenis.emx2;

import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.impl.SQLDataType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.utils.TypeUtils.getArrayType;
import static org.molgenis.emx2.utils.TypeUtils.toJooqType;

public class Column {
  private TableMetadata table;
  private String columnName;
  private ColumnType columnType = STRING;

  // relationships
  private String refTable;
  // for composite key
  private String[] refFrom = new String[0];
  private String[] refTo = new String[0];
  private String refJsTemplate;
  // for refback
  private String mappedBy;

  // options
  private Integer position = null; // column order
  private int key = 0; // 1 is primary key 2..n is secondary keys
  private boolean nullable = false;
  private String validationScript = null;
  private String computed = null;

  public String getRdfTemplate() {
    return rdfTemplate;
  }

  public void setRdfTemplate(String rdfTemplate) {
    this.rdfTemplate = rdfTemplate;
  }

  private String rdfTemplate = null;

  // todo implement below
  private boolean readonly = false;
  private String description = null;
  private String defaultValue = null;
  private boolean indexed = false;
  private boolean cascadeDelete = false;

  public Column(Column column) {
    copy(column);
  }

  public Column(TableMetadata table, Column column) {
    this.table = table;
    // todo validate
    copy(column);
  }

  /* copy constructor to prevent changes on in progress data */
  private void copy(Column column) {
    columnName = column.columnName;
    columnType = column.columnType;
    position = column.position;
    nullable = column.nullable;
    key = column.key;
    readonly = column.readonly;
    description = column.description;
    defaultValue = column.defaultValue;
    indexed = column.indexed;
    refTable = column.refTable;
    refTo = column.refTo;
    refFrom = column.refFrom;
    mappedBy = column.mappedBy;
    validationScript = column.validationScript;
    computed = column.computed;
    description = column.description;
    cascadeDelete = column.cascadeDelete;
    rdfTemplate = column.rdfTemplate;
  }

  public static Column column(String name) {
    return new Column(name);
  }

  public static Column column(String name, ColumnType type) {
    return new Column(name).setType(type);
  }

  public Column(String columnName) {
    if (!columnName.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
      throw new MolgenisException(
          "Invalid column name '"
              + columnName
              + "': Column must start with a letter or underscore, followed by letters, underscores or numbers");
    }
    this.columnName = columnName;
  }

  public Column(TableMetadata table, String columnName) {
    this.table = table;
    this.columnName = columnName;
  }

  public TableMetadata getTable() {
    return table;
  }

  public String getName() {
    return columnName;
  }

  public ColumnType getColumnType() {
    return columnType;
  }

  public SchemaMetadata getSchema() {
    return getTable().getSchema();
  }

  public String getRefTableName() {
    return this.refTable;
  }

  public TableMetadata getRefTable() {
    if (this.refTable != null && getTable() != null) {
      // self relation
      if (this.refTable.equals(getTable().getTableName())) {
        return getTable(); // this table
      }
      // other relation
      SchemaMetadata schema = getSchema();
      if (schema != null) {
        TableMetadata result = schema.getTableMetadata(this.refTable);
        if (result == null) {
          throw new MolgenisException(
              "Internal error",
              "Column.getRefTable failed for column '"
                  + getName()
                  + "' because refTable '"
                  + getRefTableName()
                  + "' does not exist in schema '"
                  + schema.getName()
                  + "'");
        }
        return result;
      }
    }
    return null;
  }

  public Column setKey(int key) {
    this.key = key;
    return this;
  }

  public int getKey() {
    return this.key;
  }

  public Boolean isReadonly() {
    return readonly;
  }

  public Column setReadonly(boolean readonly) {
    this.readonly = readonly;
    return this;
  }

  public boolean isNullable() {
    return nullable;
  }

  public Boolean isCascadeDelete() {
    return cascadeDelete;
  }

  public Column setNullable(boolean nillable) {
    this.nullable = nillable;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public Column setDescription(String description) {
    this.description = description;
    return this;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public Column setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
    return this;
  }

  public String getMappedBy() {
    return mappedBy;
  }

  public Column setMappedBy(String columnName) {
    this.mappedBy = columnName;
    return this;
  }

  public Column setIndex(boolean indexed) {
    this.indexed = indexed;
    return this;
  }

  public Column setCascadeDelete(boolean cascadeDelete) {
    if (cascadeDelete && !REF.equals(this.columnType)) {
      throw new MolgenisException(
          "Set casecadeDelete=true failed", "Columnn " + getName() + " must be of type REF");
    }
    this.cascadeDelete = cascadeDelete;
    return this;
  }

  public Boolean isIndexed() {
    return indexed;
  }

  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(getName()).append(" ");
    if (isReference()) {
      builder
          .append("" + getColumnType().toString().toLowerCase() + "(")
          .append(refTable)
          .append(")");
    }
    if (getKey() > 0) {
      builder.append(" key" + getKey());
    }
    if (Boolean.TRUE.equals(isNullable())) builder.append(" nullable");
    return builder.toString();
  }

  public Column setType(ColumnType type) {
    if (type == null) {
      throw new MolgenisException("Add column failed", "Type was null for column " + getName());
    }
    this.columnType = type;
    return this;
  }

  public Column setRefTable(String refTable) {
    this.refTable = refTable;
    return this;
  }

  public Column setTable(TableMetadata table) {
    this.table = table;
    return this;
  }

  public String getTableName() {
    if (this.table != null) return this.table.getTableName();
    return null;
  }

  public String getValidationScript() {
    return validationScript;
  }

  public Column setValidationScript(String validationScript) {
    this.validationScript = validationScript;
    return this;
  }

  public Column setPkey() {
    return this.setKey(1);
  }

  public Column removeKey() {
    this.key = 0;
    return this;
  }

  public Column setName(String columnName) {
    this.columnName = columnName;
    return this;
  }

  public Column getMappedByColumn() {
    return getRefTable().getColumn(getMappedBy());
  }

  public DataType getJooqType() {
    return toJooqType(getPrimitiveColumnType());
  }

  public Field getJooqField() {
    return field(name(getName()), getJooqType());
  }

  public org.jooq.Table getJooqTable() {
    return getTable().getJooqTable();
  }

  public boolean isReference() {
    return REF.equals(getColumnType())
        || MREF.equals(getColumnType())
        || REF_ARRAY.equals(getColumnType())
        || REFBACK.equals(getColumnType());
  }

  public String getSchemaName() {
    return getTable().getSchemaName();
  }

  public String getComputed() {
    return computed;
  }

  public Column setComputed(String computed) {
    this.computed = computed;
    return this;
  }

  public String[] getRefFrom() {
    return this.refFrom;
  }

  public Column setRefFrom(String... refFrom) {
    this.refFrom = refFrom;
    return this;
  }

  public Integer getPosition() {
    return position;
  }

  public Column setPosition(Integer position) {
    this.position = position;
    return this;
  }

  public String[] getRefTo() {
    return refTo;
  }

  public Column setRefTo(String... refTo) {
    this.refTo = refTo;
    return this;
  }

  public List<Field> getJooqFileFields() {
    return List.of(
        field(name(getName() + "_id"), SQLDataType.VARCHAR),
        field(name(getName() + "_mimetype"), SQLDataType.VARCHAR),
        field(name(getName() + "_extension"), SQLDataType.VARCHAR),
        field(name(getName() + "_size"), SQLDataType.INTEGER),
        field(name(getName() + "_contents"), SQLDataType.BINARY));
  }

  public boolean isArray() {
    return this.columnType.toString().endsWith("ARRAY") || this.columnType.equals(REFBACK);
  }

  /** will return self in case of single, and multiple in case of composite key wrapper */
  public List<Reference> getReferences() {

    // no ref
    if (getRefTable() == null) return new ArrayList<>();

    List<Reference> refColumns = new ArrayList<>();

    // check if primary key exists
    List<Column> pkeys = getRefTable().getPrimaryKeyColumns();
    if (pkeys.size() == 0) {
      throw new MolgenisException(
          "Error in column '"
              + getName()
              + "': Reference to "
              + getRefTableName()
              + " fails because that table has no primary key");
    }

    // create name map
    Map<String, String> nameLookup = new LinkedHashMap<>();
    if (refTo != null && refFrom != null && refTo.length > 0 && refTo.length == refFrom.length) {
      for (int i = 0; i < refTo.length; i++) {
        nameLookup.put(refTo[i], refFrom[i]);
      }
    } else {
      nameLookup.put(getRefTable().getPrimaryKeys().get(0), getName());
    }

    // create the refs
    for (Column keyPart : pkeys) {
      if (keyPart.isReference()) {
        for (Reference ref : keyPart.getReferences()) {
          ColumnType type = ref.getPrimitiveType();
          if (!REF.equals(getColumnType())) {
            type = getArrayType(type);
          }

          List<String> path = ref.getPath();
          path.add(0, keyPart.getName());

          String name = nameLookup.get(ref.getName());
          if (name == null)
            throw new MolgenisException(
                "get references failed: no name mapping for ref " + ref.getName());

          refColumns.add(
              new Reference(
                  this,
                  name,
                  ref.getName(),
                  getColumnType(),
                  type,
                  ref.isNullable() || this.isNullable(),
                  path));
        }
      } else {
        ColumnType type = keyPart.getColumnType();

        // all but ref is array
        if (!REF.equals(getColumnType())) {
          type = getArrayType(type);
        }

        String name = nameLookup.get(keyPart.getName());
        if (name == null)
          throw new MolgenisException(
              "get references failed: no name mapping for ref " + keyPart.getName());

        // create the ref
        refColumns.add(
            new Reference(
                this,
                name,
                keyPart.getName(),
                getColumnType(),
                type,
                keyPart.isNullable() || this.isNullable(),
                new ArrayList<>(List.of(keyPart.getName()))));
      }
    }

    return refColumns;
  }

  public ColumnType getPrimitiveColumnType() {
    if (isReference()) {
      List<Reference> refs = getReferences();
      if (refs.size() == 1) {
        return refs.get(0).getPrimitiveType();
      } else {
        throw new MolgenisException(
            "Cannot get columnType for column '"
                + getTableName()
                + "."
                + getName()
                + "': composite key");
      }
    } else return getColumnType();
  }

  public String getRefJsTemplate() {
    if (refJsTemplate == null) {
      // we concat all columns unless already shown in another column
      StringBuilder result = new StringBuilder();
      for (Reference ref : getReferences()) {
        if (!ref.isOverlapping()) {
          result.append(".${" + ref.getPath().stream().collect(Collectors.joining(".")) + "}");
        }
      }
      return result.toString().replaceFirst("[.]", "");
    }
    return refJsTemplate;
  }

  public Column setRefJsTemplate(String refJsTemplate) {
    this.refJsTemplate = refJsTemplate;
    return this;
  }
}
