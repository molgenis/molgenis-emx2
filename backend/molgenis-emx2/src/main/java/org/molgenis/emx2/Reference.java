package org.molgenis.emx2;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.molgenis.emx2.utils.TypeUtils.toJooqType;

import java.util.List;
import org.jooq.DataType;
import org.jooq.Field;

/**
 * A single flat, primitive-typed column that backs a reference.
 *
 * <p>A reference {@link Column} (REF, REF_ARRAY, REFBACK, ...) points at another table's primary
 * key. Because that primary key can itself be composite and can in turn contain references, a
 * single logical reference may expand into several physical columns. Each {@code Reference}
 * describes one of those physical columns: its local name, the foreign-key column it points to, and
 * the primitive type used to store it.
 *
 * <p>Instances are produced by {@link Column#getReferences()} and are intended to be read, not
 * constructed or mutated, by callers; treat them as value objects describing the expanded form of a
 * reference.
 *
 * <p>Note the two type accessors describe different things: {@link #getColumnType()} reports the
 * type of the <em>owning</em> reference column (e.g. {@code REF}), while {@link
 * #getPrimitiveType()} is the type actually used to store <em>this</em> physical column's values.
 */
public class Reference {
  /** The reference column this flat column was derived from. */
  private Column column;

  /** Local name of this column (the {@code from} side of the foreign key). */
  private String name;

  /** Name of the column being referenced; for nested keys this is the intermediate target. */
  private String refTo;

  /** Path of identifiers to walk from {@link #column} down to the final target. */
  private List<String> path;

  /** Table holding the final, primitive target column. */
  private String targetTable;

  /** Final, primitive target column reached by following {@link #path}. */
  private String targetColumn;

  /**
   * Type of the owning reference column (REF, REF_ARRAY, ...). This is <em>not</em> the type of
   * this physical column; see {@link #primitiveType} for that.
   */
  private ColumnType columnType;

  /** Primitive type actually used to store this physical column's values. */
  private ColumnType primitiveType;

  /** Whether this column stores an array of values (composite/array references). */
  private boolean isArray;

  /** Whether a value is required. */
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
    this.isArray = isArray;
    this.targetTable = targetTable;
    this.targetColumn = targetColumn;
    this.required = required;
    this.path = path;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getRefTo() {
    return refTo;
  }

  public List<String> getPath() {
    return path;
  }

  public String getTargetTable() {
    return this.targetTable;
  }

  public String getTargetColumn() {
    return this.targetColumn;
  }

  public ColumnType getColumnType() {
    return columnType;
  }

  public ColumnType getPrimitiveType() {
    return this.primitiveType;
  }

  public void setPrimitiveType(ColumnType type) {
    this.primitiveType = type;
  }

  public boolean isArray() {
    return isArray;
  }

  public boolean isRequired() {
    return required;
  }

  public void setColumn(Column column) {
    this.column = column;
  }

  public DataType getJooqType() {
    return toJooqType(getPrimitiveType());
  }

  public Field getJooqField() {
    return field(name(getName()), getJooqType());
  }

  /**
   * An overlapping reference is one whose name does not start with its source column's name,
   * meaning it is shared with (borrowed from) another reference rather than owned by this column
   * alone.
   */
  public boolean isOverlapping() {
    return !getName().startsWith(column.getName());
  }

  public Reference getOverlapping() {
    return column.getRefLinkColumn().getReferences().get(0);
  }

  public boolean isOverlappingRef() {
    return isOverlapping() && getOverlapping().getColumnType().getBaseType().equals(ColumnType.REF);
  }

  /** Returns this reference as a standalone primitive {@link Column}. */
  public Column toPrimitiveColumn() {
    return new Column(this.column.getTable(), this.getName(), true)
        .setType(this.getPrimitiveType())
        .setRequired(this.column.isRequired());
  }
}
