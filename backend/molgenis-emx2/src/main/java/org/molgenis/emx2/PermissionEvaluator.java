package org.molgenis.emx2;

/**
 * Single authority for authorization decisions. Combines system roles and table-level grants into a
 * unified answer, so callers don't need to understand both permission models.
 */
public interface PermissionEvaluator {

  /** Can the active user read rows from this table? */
  boolean canView(TableMetadata table);

  /** What aggregate level does the active user have on this table? */
  AggregateLevel getAggregateLevel(TableMetadata table);

  default boolean tablePermissionAtLeast(TableMetadata table, AggregateLevel level) {
    return getAggregateLevel(table).isAtLeast(level);
  }

  /** Can the active user insert rows into this table? */
  boolean canInsert(TableMetadata table);

  /** Can the active user update rows in this table? */
  boolean canUpdate(TableMetadata table);

  /** Can the active user delete rows from this table? */
  boolean canDelete(TableMetadata table);

  /** Can the active user manage the schema (create/alter/drop tables, manage roles)? */
  boolean canManage();

  /** Is the active user a database-level admin? */
  boolean isAdmin();
}
