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

  /** Can the active user insert, update, or delete rows in this table? */
  boolean canEdit(TableMetadata table);

  /** Can the active user manage the schema (create/alter/drop tables, manage roles)? */
  boolean canManage();

  /** Is the active user a database-level admin? */
  boolean isAdmin();
}
