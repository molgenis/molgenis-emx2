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
 * <p>Example: the schema declares a reference column {@code contact} (type {@code REF}) pointing at
 * table {@code Contacts}, whose primary key is the composite {@code (resource, name)} where {@code
 * resource} is itself a reference to {@code Resources.id}. Because that key is composite there is
 * no single physical {@code contact} column; {@link Column#getReferences()} expands it into two
 * physical columns, each described by one {@code Reference}:
 *
 * <pre>
 *   columnName        referencedColumnName  targetTable  targetColumn  path
 *   contact.resource  resource              Resources    id            [resource, id]
 *   contact.name      name                  Contacts     name          [name]
 * </pre>
 *
 * <p>{@code referencedColumnName} is the column this physical column points straight at - here the
 * {@code resource} / {@code name} key columns of {@code Contacts}. {@code targetColumn} is where
 * you end up after following {@code path} all the way to a plain (non-reference) column. For {@code
 * contact.resource} these differ: it points at {@code Contacts.resource}, but that is itself a
 * reference, so following it on reaches {@code Resources.id}. For {@code contact.name} they
 * coincide, because {@code Contacts.name} is already a plain column. (Had {@code Contacts} used a
 * single-column key, the expansion would collapse to one {@code Reference} named just {@code
 * contact}.)
 *
 * <p>Instances are produced by {@link Column#getReferences()}. This is an immutable value object
 * describing the expanded form of a reference; use {@link #withColumnName(String)} to obtain a
 * renamed copy rather than mutating in place.
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
   * Column this physical column points straight at in a single FK step. For a direct reference this
   * is the final target; for a nested composite key it is an <em>intermediate</em> column on the
   * way there (the final destination is {@link #targetColumn} in {@link #targetTable}, reached via
   * {@link #path}).
   */
  private final String referencedColumnName;

  /** Path of identifiers to walk from {@link #column} down to the final target. */
  private final List<String> path;

  /** Table holding the final, primitive target column reached by following {@link #path}. */
  private final String targetTable;

  /**
   * Final, primitive target column reached by following {@link #path}. Equals {@link
   * #referencedColumnName} for a direct reference; differs for nested composite keys.
   */
  private final String targetColumn;

  /** Primitive type actually used to store this physical column's values. */
  private final ColumnType primitiveType;

  /**
   * Whether this physical column stores an array and so must be unnested when queried. True when
   * the owning reference is an array reference (e.g. REF_ARRAY) or, for a composite key part that
   * is itself a reference, when that key part is array-typed.
   */
  private final boolean isArray;

  /**
   * Whether this physical column requires a value: true when the owning reference column is
   * required or the referenced key part is. Read while expanding nested references to propagate
   * requiredness upward; {@link #toPrimitiveColumn()} instead re-derives it from the owning column.
   */
  private final boolean required;

  public Reference(
      Column column,
      String columnName,
      String referencedColumnName,
      ColumnType primitiveType,
      boolean isArray,
      String targetTable,
      String targetColumn,
      boolean required,
      List<String> path) {
    this.column = column;
    this.columnName = columnName;
    this.referencedColumnName = referencedColumnName;
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

  /**
   * Whether the owning reference column is a singular reference - base type {@code REF}, including
   * flavors such as {@code SELECT}/{@code RADIO}/{@code ONTOLOGY} - rather than an array reference.
   */
  public boolean isRef() {
    return column.isRef();
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
   * Whether this column is borrowed from another reference in the same table via that column's
   * {@code refLink} - the user-facing "overlapping references" feature. A borrowed part carries the
   * other column's name, so it does not start with this {@link #column}'s name (how it is
   * detected).
   *
   * <p>Guard with {@code !isOverlapping()} to process each physical column once; the borrowed copy
   * is owned by the column it came from.
   *
   * @see <a
   *     href="https://molgenis.github.io/molgenis-emx2/#/molgenis/use_schema?id=reflink">refLink
   *     schema documentation</a>
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
