package org.molgenis.emx2;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.Constants.COMPOSITE_REF_SEPARATOR;
import static org.molgenis.emx2.Constants.SYS_COLUMN_NAME_PREFIX;
import static org.molgenis.emx2.utils.TypeUtils.*;

import java.util.*;
import java.util.stream.Collectors;
import org.javers.core.metamodel.annotation.DiffIgnore;
import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.impl.SQLDataType;

public class Column extends HasLabelsDescriptionsAndSettings<Column> implements Comparable<Column> {

  // basics
  private TableMetadata table; // table this column is part of
  private String columnName; // short name, first character A-Za-z followed by AZ-a-z_0-1
  private ColumnType columnType = STRING; // type of the column

  // transient for enabling migrations
  @DiffIgnore private String oldName; // use this when wanting to change name
  @DiffIgnore private boolean drop; // use this for migrations, i.e. explicit CREATE, ALTER, DROP

  // relationships
  private String refSchema; // for cross schema references
  private String refTable; // table referenced
  private String refLink; // to allow a reference value to depend on another reference.
  private String refLabel; // template string influencing how ref value is shown
  private String refBack; // for REFBACK, indicate the column to be used for linkback

  @DiffIgnore
  private Integer position =
      null; // column order within the table. During import/export these may change

  private int key = 0; // 1 is primary key 2..n is secondary keys
  private boolean required = false;
  private String validation = null;
  private String visible = null; // javascript expression to influence vibility
  private String computed = null; // javascript expression to compute a value, overrides updates
  private String[] semantics = null; // json ld expression
  // todo implement below, or remove
  private Boolean readonly = false;
  private String defaultValue = null;
  private boolean indexed = false;
  private boolean cascadeDelete = false;

  public Column(Column column) {
    copy(column);
  }

  public Column(TableMetadata table, Column column) {
    this.table = table;
    copy(column);
  }

  public Column(String columnName) {
    this(columnName, false);
  }

  public Column(String columnName, boolean skipValidation) {
    this.columnName = validateName(columnName, skipValidation);
  }

  public Column(TableMetadata table, String columnName) {
    this(columnName);
    this.table = table;
  }

  public Column(TableMetadata table, String columnName, boolean skipValidation) {
    this(columnName, skipValidation);
    this.table = table;
  }

  public static Column column(String name) {
    return new Column(name);
  }

  public static Column column(String name, ColumnType type) {
    return new Column(name).setType(type);
  }

  private String validateName(String columnName, boolean skipValidation) {
    if (!skipValidation && !columnName.matches("[a-zA-Z][a-zA-Z0-9_ ]*")) {
      throw new MolgenisException(
          "Invalid column name '"
              + columnName
              + "': Column must start with a letter, followed by letters, underscores, a space or numbers, i.e. [a-zA-Z][a-zA-Z0-9_]*");
    }
    if (!skipValidation && (columnName.contains("_ ") || columnName.contains(" _"))) {
      throw new MolgenisException(
          "Invalid column name '" + columnName + "': column names cannot contain '_ ' or '_ '");
    }
    return columnName.trim();
  }

  public String[] getSemantics() {
    return semantics;
  }

  public Column setSemantics(String... semantics) {
    this.semantics = semantics;
    return this;
  }

  /* copy constructor to prevent changes on in progress data */
  private void copy(Column column) {
    columnName = column.columnName;
    labels = column.labels;
    oldName = column.oldName;
    drop = column.drop;
    columnType = column.columnType;
    position = column.position;
    required = column.required;
    key = column.key;
    readonly = column.readonly;
    defaultValue = column.defaultValue;
    indexed = column.indexed;
    refTable = column.refTable;
    refLink = column.refLink;
    refSchema = column.refSchema;
    refBack = column.refBack;
    validation = column.validation;
    refLabel = column.refLabel;
    computed = column.computed;
    descriptions = column.descriptions;
    cascadeDelete = column.cascadeDelete;
    semantics = column.semantics;
    visible = column.visible;
  }

  public TableMetadata getTable() {
    return table;
  }

  public Column setTable(TableMetadata table) {
    this.table = table;
    return this;
  }

  public String getName() {
    return columnName;
  }

  public String getIdentifier() {
    return convertToCamelCase(getName());
  }

  public Column setName(String columnName) {
    this.columnName = columnName;
    return this;
  }

  public String getQualifiedName() {
    return getTableName() + "." + getName();
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

  public String getRefTableIdentifier() {
    return convertToPascalCase(this.getRefTableName());
  }

  public TableMetadata getRefTable() {
    SchemaMetadata schema = getSchema();
    if (this.refSchema != null) {
      try {
        schema = getSchema().getDatabase().getSchema(this.refSchema).getMetadata();
      } catch (Exception e) {
        throw new MolgenisException(
            "refSchema '"
                + this.refSchema
                + "' cannot be found for column '"
                + getTableName()
                + "."
                + getName()
                + "'. Do you have access permissions?");
      }
    }

    if (this.refTable != null && getTable() != null) {
      // self relation
      if (this.refTable.equals(getTable().getTableName())) {
        return getTable(); // this table
      }

      // other relation
      if (schema != null) {
        return schema.getTableMetadata(this.refTable);
      }
    }
    return null;
  }

  public Column setRefTable(String refTable) {
    this.refTable = refTable;
    return this;
  }

  public int getKey() {
    return this.key;
  }

  public Column setKey(int key) {
    this.key = key;
    return this;
  }

  public Boolean isReadonly() {
    return readonly;
  }

  public Column setReadonly(Boolean readonly) {
    this.readonly = readonly;
    return this;
  }

  public boolean isRequired() {
    return required;
  }

  public Column setRequired(boolean required) {
    this.required = required;
    return this;
  }

  public Boolean isCascadeDelete() {
    return cascadeDelete;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public Column setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
    return this;
  }

  public String getRefBack() {
    return refBack;
  }

  public Column setRefBack(String columnName) {
    this.refBack = columnName;
    return this;
  }

  public Column setIndex(Boolean indexed) {
    this.indexed = indexed;
    return this;
  }

  public Column setCascadeDelete(Boolean cascadeDelete) {
    if (cascadeDelete && !isRef()) {
      throw new MolgenisException(
          "Set casecadeDelete=true failed: columnn " + getName() + " must be of type REF");
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
    if (Boolean.TRUE.equals(isRequired())) builder.append(" required");
    return builder.toString();
  }

  public Column setType(ColumnType type) {
    if (type == null) {
      throw new MolgenisException("Add column failed: type was null for column " + getName());
    }
    this.columnType = type;
    return this;
  }

  public String getTableName() {
    if (this.table != null) return this.table.getTableName();
    return null;
  }

  public String getValidation() {
    return validation;
  }

  public Column setValidation(String validation) {
    this.validation = validation;
    return this;
  }

  public Column setPkey() {
    if (getColumnType().isArray()) {
      throw new MolgenisException(
          "Set primary key failed on column '"
              + getName()
              + "': Cannot make primary key from an array column");
    }
    return this.setKey(1).setRequired(true);
  }

  public Column removeKey() {
    this.key = 0;
    return this;
  }

  public Column getRefBackColumn() {
    return getRefTable().getColumn(getRefBack());
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

  public boolean isRef() {
    return getColumnType().isRef();
  }

  public boolean isReference() {
    return getColumnType().isReference();
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

  public Integer getPosition() {
    return position;
  }

  public Column setPosition(Integer position) {
    this.position = position;
    return this;
  }

  public List<Field> getJooqFileFields() {
    return List.of(
        field(name(getName()), SQLDataType.VARCHAR),
        field(name(getName() + "_mimetype"), SQLDataType.VARCHAR),
        field(name(getName() + "_extension"), SQLDataType.VARCHAR),
        field(name(getName() + "_size"), SQLDataType.INTEGER),
        field(name(getName() + "_contents"), SQLDataType.BINARY));
  }

  public Boolean isArray() {
    return this.columnType.isArray();
  }

  /** will return self in case of single, and multiple in case of composite key wrapper */
  public List<Reference> getReferences() {

    // no ref
    if (getRefTable() == null) {
      return new ArrayList<>();
    }

    List<Column> pkeys = getRefTable().getPrimaryKeyColumns();
    List<Reference> refColumns = new ArrayList<>();

    // check if primary key exists
    if (pkeys.size() == 0) {
      throw new MolgenisException(
          "Error in column '"
              + getName()
              + "': Reference to "
              + getRefTableName()
              + " fails because that table has no primary key");
    }

    // create the refs
    Column refLink = getRefLinkColumn();
    for (Column keyPart : pkeys) {
      if (keyPart.isReference()) {
        for (Reference ref : keyPart.getReferences()) {
          ColumnType type = ref.getPrimitiveType();
          if (!isRef()) {
            type = getArrayType(type);
          }
          List<String> path = ref.getPath();
          path.add(0, keyPart.getIdentifier());
          String name = null;
          if (refLink != null) {
            for (Reference overlap : refLink.getReferences()) {
              if (overlap.getTargetTable().equals(ref.getTargetTable())
                  && overlap.getTargetColumn().equals(ref.getTargetColumn())) {
                name = overlap.getName();
              }
            }
          }
          if (name == null) {
            name = getName();
            if (pkeys.size() > 1) {
              name += COMPOSITE_REF_SEPARATOR + ref.getName();
            }
          }
          refColumns.add(
              new Reference(
                  this,
                  name,
                  ref.getName(),
                  getColumnType(),
                  type,
                  keyPart.getColumnType().isArray(),
                  ref.getTargetTable(),
                  ref.getTargetColumn(),
                  ref.isRequired() || this.isRequired(),
                  path));
        }
      } else {
        ColumnType type = keyPart.getColumnType();

        // all but ref is array
        if (!isRef()) {
          type = getArrayType(type);
        }

        // create the ref
        String name = getName();
        if (pkeys.size() > 1) {
          name += COMPOSITE_REF_SEPARATOR + keyPart.getName();
        }
        refColumns.add(
            new Reference(
                this,
                name,
                keyPart.getName(),
                getColumnType(),
                type,
                getColumnType().isArray(),
                getRefTableName(),
                keyPart.getName(),
                keyPart.isRequired() || this.isRequired(),
                new ArrayList<>(List.of(keyPart.getIdentifier()))));
      }
    }

    // clean up in case only one
    if (refColumns.stream().filter(r -> r.getName().startsWith(getName())).count() == 1) {
      refColumns =
          refColumns.stream()
              .map(
                  r -> {
                    if (r.getName().startsWith(getName())) r.setName(getName());
                    return r;
                  })
              .collect(Collectors.toList());
    }

    // remove duplicates
    HashSet<Object> seen = new HashSet<>();
    refColumns.removeIf(e -> !seen.add(e.getName()));
    return refColumns;
  }

  public ColumnType getPrimitiveColumnType() {
    if (isReference()) {
      List<Reference> refs = getReferences();
      if (refs.size() == 1) {
        return refs.get(0).getPrimitiveType().getBaseType();
      } else {
        throw new MolgenisException(
            "Cannot get columnType for column '"
                + getTableName()
                + "."
                + getName()
                + "': composite key");
      }
    } else return getColumnType().getBaseType();
  }

  public String getRefLabel() {
    return this.refLabel;
  }

  public String getRefLabelDefault() {
    if (!isReference()) return null;
    // we concat all columns unless already shown in another column
    StringBuilder result = new StringBuilder();
    for (Reference ref : getReferences()) {
      if (!ref.isOverlapping()) {
        result.append(".${" + ref.getPath().stream().collect(Collectors.joining(".")) + "}");
      }
    }
    return result.toString().replaceFirst("[.]", "");
  }

  public Column setRefLabel(String refLabel) {
    this.refLabel = refLabel;
    return this;
  }

  public String getRefSchema() {
    if (refSchema != null) {
      return refSchema;
    } else if (getRefTable() != null) {
      return getRefTable().getSchemaName();
    } else return getSchemaName();
  }

  public Column setRefSchema(String refSchema) {
    this.refSchema = refSchema;
    return this;
  }

  public String getVisible() {
    return visible;
  }

  public Column setVisible(String visible) {
    this.visible = visible;
    return this;
  }

  public String getOldName() {
    return oldName;
  }

  public Column setOldName(String oldName) {
    this.oldName = oldName;
    return this;
  }

  public boolean isDrop() {
    return drop;
  }

  public Column drop() {
    this.drop = true;
    return this;
  }

  public String getRefLink() {
    return refLink;
  }

  public void setRefLink(String refLink) {
    this.refLink = refLink;
  }

  public Column getRefLinkColumn() {
    if (refLink != null) {
      return getTable().getColumn(refLink);
    }
    return null;
  }

  public boolean isPrimaryKey() {
    return getKey() == 1;
  }

  public boolean isRefArray() {
    return getColumnType().isRefArray();
  }

  public boolean isRefback() {
    return getColumnType().isRefback();
  }

  public boolean isFile() {
    return getColumnType().isFile();
  }

  public boolean isSystemColumn() {
    return this.getName().startsWith(SYS_COLUMN_NAME_PREFIX);
  }

  public boolean isHeading() {
    return this.getColumnType().isHeading();
  }

  @Override
  public int compareTo(Column o) {
    if (this.getPosition() > o.getPosition()) {
      return 1;
    } else if (this.getPosition() < o.getPosition()) {
      return -1;
    } else {
      return this.getName().compareTo(o.getName());
    }
  }

  public boolean isOntology() {
    return this.getColumnType().equals(ONTOLOGY) || this.getColumnType().equals(ONTOLOGY_ARRAY);
  }

  public String getRootTableName() {
    TableMetadata table = this.getTable();
    while (table.getInherit() != null) {
      table = table.getInheritedTable();
    }
    return table.getTableName();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Column column = (Column) o;
    return table.getSchemaName().equals(column.getSchemaName())
        && table.equals(column.table)
        && columnName.equals(column.columnName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(table, columnName);
  }
}
