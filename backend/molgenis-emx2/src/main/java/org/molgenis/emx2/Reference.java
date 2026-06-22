package org.molgenis.emx2;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.molgenis.emx2.utils.TypeUtils.toJooqType;

import java.util.List;
import org.jooq.DataType;
import org.jooq.Field;

public class Reference {
  private Column column;
  private String name;
  private String refTo; // intermediate target, might be table in the middle
  private List<String> path;
  private String targetTable; // final target
  private String targetColumn; // final target

  public List<String> getPath() {
    return path;
  }

  private ColumnType columnType;
  private ColumnType primitiveType;
  private boolean isArray;
  private boolean required;

  public Reference(
      Column column,
      String name,
      String refTo,
      ColumnType columnType,
      ColumnType primitiveType,
      boolean isArray,
      String targetTable,
      String targetColumn,
      boolean required,
      List<String> path) {
    this.column = column;
    this.name = name;
    this.refTo = refTo;
    this.columnType = columnType;
    this.primitiveType = primitiveType;
    this.targetTable = targetTable;
    this.isArray = isArray;
    this.targetColumn = targetColumn;
    this.required = required;
    this.path = path;
  }

  public String getName() {
    return name;
  }

  public String getRefTo() {
    return refTo;
  }

  public ColumnType getColumnType() {
    return columnType;
  }

  public DataType getJooqType() {
    return toJooqType(getPrimitiveType());
  }

  public boolean isRequired() {
    return required;
  }

  public Field getJooqField() {
    return field(name(getName()), getJooqType());
  }

  public void setName(String name) {
    this.name = name;
  }

  public ColumnType getPrimitiveType() {
    return this.primitiveType;
  }

  public Reference getOverlapping() {
    return column.getRefLinkColumn().getReferences().get(0);
  }

  public boolean isOverlapping() {
    return !getName().startsWith(column.getName());
  }

  public void setPrimitiveType(ColumnType type) {
    this.primitiveType = type;
  }

  public void setColumn(Column column) {
    this.column = column;
  }

  public String getTargetTable() {
    return this.targetTable;
  }

  public String getTargetColumn() {
    return this.targetColumn;
  }

  public boolean isArray() {
    return isArray;
  }

  public boolean isOverlappingRef() {
    return isOverlapping() && getOverlapping().getColumnType().getBaseType().equals(ColumnType.REF);
  }

  public Column toPrimitiveColumn() {
    return new Column(this.column.getTable(), this.getName(), true)
        .setType(this.getPrimitiveType())
        .setRequired(this.column.isRequired());
  }
}
