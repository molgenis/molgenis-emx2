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
 * <p>Instances are produced by {@link Column#getReferences()}. This is an immutable value object
 * describing the expanded form of a reference; use {@link #withName(String)} to obtain a renamed
 * copy rather than mutating in place.
 *
 * <p>Note the two type accessors describe different things: {@link #getColumnType()} reports the
 * type of the <em>owning</em> reference column (e.g. {@code REF}), while {@link
 * #getPrimitiveType()} is the type actually used to store <em>this</em> physical column's values.
 */
public class Reference {
  /** The reference column this flat column was derived from. */
  private final Column column;

  /** Local name of this column (the {@code from} side of the foreign key). */
  private final String name;

  /**
   * Name of the column this physical column points at; for nested keys this is an intermediate
   * target rather than the final one (see {@link #targetColumn}).
   */
  private final String referencedColumnName;

  /** Path of identifiers to walk from {@link #column} down to the final target. */
  private final List<String> path;

  /** Table holding the final, primitive target column. */
  private final String targetTable;

  /** Final, primitive target column reached by following {@link #path}. */
  private final String targetColumn;

  /**
   * Type of the owning reference column (REF, REF_ARRAY, ...). This is <em>not</em> the type of
   * this physical column; see {@link #primitiveType} for that.
   */
  private final ColumnType columnType;

  /** Primitive type actually used to store this physical column's values. */
  private final ColumnType primitiveType;

  /** Whether this column stores an array of values (composite/array references). */
  private final boolean isArray;

  /** Whether a value is required. */
  private final boolean required;

  public Reference(
      Column column,
      String name,
      String referencedColumnName,
      ColumnType columnType,
      ColumnType primitiveType,
      boolean isArray,
      String targetTable,
      String targetColumn,
      boolean required,
      List<String> path) {
    this.column = column;
    this.name = name;
    this.referencedColumnName = referencedColumnName;
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

  /** Returns a copy of this reference with a different local name. */
  public Reference withName(String name) {
    return new Reference(
        column,
        name,
        referencedColumnName,
        columnType,
        primitiveType,
        isArray,
        targetTable,
        targetColumn,
        required,
        path);
  }

  public String getReferencedColumnName() {
    return referencedColumnName;
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

  public boolean isArray() {
    return isArray;
  }

  public boolean isRequired() {
    return required;
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
