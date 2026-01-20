package org.molgenis.emx2.sql;

import java.util.Objects;
import java.util.Optional;

public class SqlAlias {

  private static final String QUALIFIED_NAME_SEPARATOR = "-";

  private final String name;
  private final SqlAlias parent;
  private final boolean allowsAlias;

  private SqlAlias(String name, SqlAlias parent, boolean allowsAlias) {
    this.name = name;
    this.parent = parent;
    this.allowsAlias = allowsAlias;
  }

  public static SqlAlias withoutAlias(String name) {
    return new SqlAlias(name, null, false);
  }

  public static SqlAlias withoutAlias(String name, SqlAlias parent) {
    return new SqlAlias(name, parent, false);
  }

  public static SqlAlias withAlias(String name) {
    return new SqlAlias(name, null, true);
  }

  public static SqlAlias withAlias(String name, SqlAlias parent) {
    return new SqlAlias(name, parent, true);
  }

  public boolean allowsAlias() {
    return getParent()
        .map(SqlAlias::allowsAlias)
        .map(parentAllows -> allowsAlias || parentAllows)
        .orElse(allowsAlias);
  }

  public String getName() {
    return name;
  }

  public String getQualifiedName() {
    String parentQualifiedName =
        getParent()
            .map(SqlAlias::getQualifiedName)
            .map(n -> n + QUALIFIED_NAME_SEPARATOR)
            .orElse("");

    return parentQualifiedName + name;
  }

  public Optional<SqlAlias> getParent() {
    return Optional.ofNullable(parent);
  }

  public Optional<SqlAlias> getTopParent() {
    return getParent().map(p -> p.getParent().orElse(p));
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    SqlAlias sqlAlias = (SqlAlias) o;
    return allowsAlias == sqlAlias.allowsAlias
        && Objects.equals(name, sqlAlias.name)
        && Objects.equals(parent, sqlAlias.parent);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, parent, allowsAlias);
  }

  @Override
  public String toString() {
    return "SqlAlias{"
        + "name='"
        + name
        + '\''
        + ", parent="
        + parent
        + ", allowsAlias="
        + allowsAlias
        + '}';
  }
}
