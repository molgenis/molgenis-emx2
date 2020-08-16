package org.molgenis.emx2;

import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.impl.SQLDataType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
  private String refContraint; // only for composite keys
  private String mappedBy;

  // options
  private Integer position = null; // column order
  private int key = 0; // 1 is primary key 2..n is secondary keys
  private boolean nullable = false;
  private String validationScript = null;
  private String computed = null;

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
    refContraint = column.refContraint;
    mappedBy = column.mappedBy;
    validationScript = column.validationScript;
    computed = column.computed;
    description = column.description;
    cascadeDelete = column.cascadeDelete;
  }

  public static Column column(String name) {
    return new Column(name);
  }

  public static Column column(String name, ColumnType type) {
    return new Column(name).type(type);
  }

  public Column(String columnName) {
    //    if (!columnName.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
    //      throw new MolgenisException(
    //          "Invalid column name '" + columnName + "'",
    //          "Column must start with a letter or underscore, followed by letters, underscores or
    // numbers");
    //    }
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

  public List<Reference> getReferences() {
    List<Reference> refColumns = new ArrayList<>();
    if (getRefTable() == null) return refColumns;
    List<Column> pkeys = getRefTable().getPrimaryKeyColumns();
    if (pkeys.size() == 0) {
      throw new MolgenisException(
          "Error in column '" + getName() + "'",
          "Reference to " + getRefTableName() + " fails because that table has no primary key");
    }

    // first create
    for (Column keyPart : pkeys) {
      if (keyPart.isReference()) {
        for (Reference ref : keyPart.getReferences()) {
          ColumnType type = ref.getColumnType();
          if (!REF.equals(getColumnType())) {
            type = getArrayType(type);
          }
          refColumns.add(
              new Reference(
                  ref.getName(), ref.getName(), type, ref.isNullable() || this.isNullable()));
        }
      } else {
        ColumnType type = keyPart.getColumnType();
        // all but ref is array
        if (!REF.equals(getColumnType())) {
          type = getArrayType(type);
        }
        // create the ref
        refColumns.add(
            new Reference(
                keyPart.getName(),
                keyPart.getName(),
                type,
                keyPart.isNullable() || this.isNullable()));
      }
    }

    // check if maps to existing, otherwise count to see if we need prefixing
    int prefixCount = 0;
    for (Reference ref : refColumns) {
      if (getRefContraintMap().containsKey(ref.getName())) {
        ref.setName(getRefContraintMap().get(ref.getName()));
        ref.setExisting(true);
      } else {
        // if not existing, we might need to prefix if more than one reference
        prefixCount++;
      }
    }

    // if multiple, prefix with column name, otherwise rename to column name
    // (ignore if maps to existing column)
    if (prefixCount > 1) {
      for (Reference ref : refColumns) {
        // don't touch 'existing'
        if (!ref.isExisting()) {
          ref.setName(getName() + "-" + ref.getName());
        }
      }
    } else {
      for (Reference ref : refColumns) {
        // don't touch 'existing'
        if (!ref.isExisting()) {
          refColumns.get(0).setName(getName());
        }
      }
    }

    return refColumns;
  }

  private Map<String, String> getRefContraintMap() {
    Map<String, String> result = new LinkedHashMap<>();
    if (getRefContraint() != null) {
      for (String part : getRefContraint().split(",")) {
        String key = part.substring(0, part.indexOf("=")).trim();
        String value = part.substring(part.indexOf("=") + 1).trim();
        result.put(key, value);
      }
    }
    return result;
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

  public Column key(int key) {
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

  public Column nullable(boolean nillable) {
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

  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }

  public String getMappedBy() {
    return mappedBy;
  }

  public Column mappedBy(String columnName) {
    this.mappedBy = columnName;
    return this;
  }

  public Column index(boolean indexed) {
    this.indexed = indexed;
    return this;
  }

  public Column cascadeDelete(boolean cascadeDelete) {
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

  public Column type(ColumnType type) {
    if (type == null) {
      throw new MolgenisException("Add column failed", "Type was null for column " + getName());
    }
    this.columnType = type;
    return this;
  }

  public Column refTable(String refTable) {
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

  public String getValidation() {
    return validationScript;
  }

  public Column validation(String validationScript) {
    this.validationScript = validationScript;
    return this;
  }

  public Column pkey() {
    return this.key(1);
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
    return toJooqType(getColumnType());
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

  public Column computed(String computed) {
    this.computed = computed;
    return this;
  }

  public String getRefContraint() {
    return this.refContraint;
  }

  public Column refConstraint(String refConstraint) {
    this.refContraint = refConstraint;
    return this;
  }

  public Integer getPosition() {
    return position;
  }

  public Column position(Integer position) {
    this.position = position;
    return this;
  }

  public List<Field> getJooqFileFields() {
    return List.of(
        field(name(getName() + "-id"), SQLDataType.VARCHAR),
        field(name(getName() + "-mimetype"), SQLDataType.VARCHAR),
        field(name(getName() + "-extension"), SQLDataType.VARCHAR),
        field(name(getName() + "-size"), SQLDataType.INTEGER),
        field(name(getName() + "-contents"), SQLDataType.BINARY));
  }
}
