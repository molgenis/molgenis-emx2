package org.molgenis.emx2;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.Constants.COMPOSITE_REF_SEPARATOR;
import static org.molgenis.emx2.utils.TypeUtils.getArrayType;
import static org.molgenis.emx2.utils.TypeUtils.toJooqType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.javers.core.metamodel.annotation.DiffIgnore;
import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.impl.SQLDataType;
import org.molgenis.emx2.utils.TypeUtils;

public class Column {

  // basics
  private TableMetadata table;
  private String columnName; // short name, first character A-Za-z followed by AZ-a-z_0-1
  private ColumnType columnType = STRING;
  private String columnFormat = null; // influences how data is rendered, in addition to type

  // transient for enabling migrations
  @DiffIgnore private String oldName; // use this when wanting to change name
  @DiffIgnore private boolean drop; // use this for migrations, i.e. explicit CREATE, ALTER, DROP

  // relationships
  private String refSchema; // for cross schema references
  private String refTable;
  private String refLink; // to allow a reference to depend on another reference.
  private String refJsTemplate;
  // for refback
  private String mappedBy;

  // options
  private String description = null; // long description of the column
  private Integer position = null; // column order within the table
  private int key = 0; // 1 is primary key 2..n is secondary keys
  private boolean required = false;
  private String validationExpression = null;
  private String visibleExpression = null; // javascript expression to influence vibility
  private String computed = null; // javascript expression to compute a value, overrides updates
  private String jsonldType = null; // json ld expression
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
    if (!skipValidation && !columnName.matches("[a-zA-Z][a-zA-Z0-9_]*")) {
      throw new MolgenisException(
          "Invalid column name '"
              + columnName
              + "': Column must start with a letter, followed by letters, underscores or numbers, i.e. [a-zA-Z][a-zA-Z0-9_]*");
    }
    this.columnName = columnName;
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

  public String getJsonldType() {
    return jsonldType;
  }

  public Column setJsonldType(String jsonldType) {
    this.jsonldType = TypeUtils.toJson(jsonldType);
    return this;
  }

  /* copy constructor to prevent changes on in progress data */
  private void copy(Column column) {
    columnName = column.columnName;
    oldName = column.oldName;
    drop = column.drop;
    columnType = column.columnType;
    position = column.position;
    required = column.required;
    key = column.key;
    readonly = column.readonly;
    description = column.description;
    defaultValue = column.defaultValue;
    indexed = column.indexed;
    refTable = column.refTable;
    refLink = column.refLink;
    refSchema = column.refSchema;
    mappedBy = column.mappedBy;
    validationExpression = column.validationExpression;
    computed = column.computed;
    description = column.description;
    cascadeDelete = column.cascadeDelete;
    jsonldType = column.jsonldType;
    columnFormat = column.columnFormat;
    visibleExpression = column.visibleExpression;
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

  public Column setName(String columnName) {
    this.columnName = columnName;
    return this;
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
                + "'");
      }
    }

    if (this.refTable != null && getTable() != null) {
      // self relation
      if (this.refTable.equals(getTable().getTableName())) {
        return getTable(); // this table
      }

      // inherited pkey field, hard to explain but this is needed for cross schema inheritanc
      if (getTable().getInherit() != null
          && getTable().getInheritedTable().getColumn(this.getName()) != null) {
        schema = getTable().getInheritedTable().getColumn(this.getName()).getSchema();
      }

      // other relation
      if (schema != null) {
        TableMetadata result = schema.getTableMetadata(this.refTable);
        if (result == null) {
          throw new MolgenisException(
              "Internal error: Column.getRefTable failed for column '"
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

  public Column setIndex(Boolean indexed) {
    this.indexed = indexed;
    return this;
  }

  public Column setCascadeDelete(Boolean cascadeDelete) {
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
    if (Boolean.TRUE.equals(isRequired())) builder.append(" required");
    return builder.toString();
  }

  public Column setType(ColumnType type) {
    if (type == null) {
      throw new MolgenisException("Add column failed", "Type was null for column " + getName());
    }
    this.columnType = type;
    return this;
  }

  public String getTableName() {
    if (this.table != null) return this.table.getTableName();
    return null;
  }

  public String getValidationExpression() {
    return validationExpression;
  }

  public Column setValidationExpression(String validationExpression) {
    this.validationExpression = validationExpression;
    return this;
  }

  public Column setPkey() {
    return this.setKey(1).setRequired(true);
  }

  public Column removeKey() {
    this.key = 0;
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
        //        || MREF.equals(getColumnType())
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
    return this.columnType.toString().endsWith("ARRAY") || this.columnType.equals(REFBACK);
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
          if (!REF.equals(getColumnType())) {
            type = getArrayType(type);
          }
          List<String> path = ref.getPath();
          path.add(0, keyPart.getName());
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
                  ref.getTargetTable(),
                  ref.getTargetColumn(),
                  ref.isRequired() || this.isRequired(),
                  path));
        }
      } else {
        ColumnType type = keyPart.getColumnType();

        // all but ref is array
        if (!REF.equals(getColumnType())) {
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
                getRefTableName(),
                keyPart.getName(),
                keyPart.isRequired() || this.isRequired(),
                new ArrayList<>(List.of(keyPart.getName()))));
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

    // remove dupalictes
    HashSet<Object> seen = new HashSet<>();
    refColumns.removeIf(e -> !seen.add(e.getName()));

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
    if (!isReference()) return null;
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

  public String getColumnFormat() {
    return columnFormat;
  }

  public void setColumnFormat(String columnFormat) {
    this.columnFormat = columnFormat;
  }

  public String getVisibleExpression() {
    return visibleExpression;
  }

  public Column setVisibleExpression(String visibleExpression) {
    this.visibleExpression = visibleExpression;
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

  public Column getRefLinkColumn() {
    if (refLink != null) {
      return getTable().getColumn(refLink);
    }
    return null;
  }

  public void setRefLink(String refLink) {
    this.refLink = refLink;
  }
}
