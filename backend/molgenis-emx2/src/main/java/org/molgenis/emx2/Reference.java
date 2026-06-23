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
 * describing the expanded form of a reference; use {@link #withColumnName(String)} to obtain a
 * renamed copy rather than mutating in place.
 *
 * <p>Note the two type accessors describe different things: {@link #getColumnType()} reports the
 * type of the <em>owning</em> reference column (e.g. {@code REF}), while {@link
 * #getPrimitiveType()} is the type actually used to store <em>this</em> physical column's values.
 */
public class Reference {
  /** The reference column this flat column was derived from. */
  private final Column column;

  /**
   * Name of this physical column (the {@code from} side of the foreign key). Derived from the
   * owning {@link #column}'s name but not equal to it: for composite keys a key-part suffix is
   * appended, and overlapping references borrow their name from another column.
   */
  private final String columnName;

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
      String columnName,
      String referencedColumnName,
      ColumnType columnType,
      ColumnType primitiveType,
      boolean isArray,
      String targetTable,
      String targetColumn,
      boolean required,
      List<String> path) {
    this.column = column;
    this.columnName = columnName;
    this.referencedColumnName = referencedColumnName;
    this.columnType = columnType;
    this.primitiveType = primitiveType;
    this.isArray = isArray;
    this.targetTable = targetTable;
    this.targetColumn = targetColumn;
    this.required = required;
    this.path = path;
  }

  public String getColumnName() {
    return columnName;
  }

  /** Returns a copy of this reference with a different column name. */
  public Reference withColumnName(String columnName) {
    return new Reference(
        column,
        columnName,
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

  /**
   * Whether the owning reference column is a singular reference - base type {@code REF}, including
   * flavors such as {@code SELECT}/{@code RADIO}/{@code ONTOLOGY} - rather than an array reference.
   */
  public boolean isRef() {
    return getColumnType().isRef();
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
    return field(name(getColumnName()), getJooqType());
  }

  /**
   * Whether this expanded column is an <em>overlapping reference</em>: a foreign-key column shared
   * with (borrowed from) another reference in the same table through that other column's {@code
   * refLink}. A borrowed part carries the refLink column's name instead of this column's own name,
   * so it does not start with {@link #column}'s name - which is how it is detected here.
   *
   * <p>Callers typically guard with {@code !isOverlapping()} to handle each physical column once:
   * the borrowed copy is owned and emitted by the column it was borrowed from. "Overlapping
   * references" is the user-facing term for the {@code refLink} feature; see the EMX2 schema
   * documentation.
   */
  public boolean isOverlapping() {
    return !getColumnName().startsWith(column.getName());
  }

  /**
   * The reference of the {@code refLink} column this overlapping column borrows from. Assumes a
   * refLink points at a single-column reference (the common case); only {@link #isOverlappingRef()}
   * uses it, and only to inspect the base type.
   */
  private Reference refLinkReference() {
    return column.getRefLinkColumn().getReferences().get(0);
  }

  /**
   * Whether this is an {@link #isOverlapping() overlapping} column whose borrowed-from refLink
   * reference is itself a {@code REF} (rather than a scalar key). SQL generation uses this to
   * decide the value is already available as a plain column instead of needing to be unnested.
   */
  public boolean isOverlappingRef() {
    return isOverlapping() && refLinkReference().isRef();
  }

  /** Returns this reference as a standalone primitive {@link Column}. */
  public Column toPrimitiveColumn() {
    return new Column(this.column.getTable(), this.getColumnName(), true)
        .setType(this.getPrimitiveType())
        .setRequired(this.column.isRequired());
  }
}
