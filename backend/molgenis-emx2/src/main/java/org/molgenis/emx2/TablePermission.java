package org.molgenis.emx2;

/**
 * Table-level permission for a custom role.
 *
 * <p>{@code select} uses existing {@link Privileges} to express the access level (EXISTS through
 * VIEWER). {@code insert}, {@code update}, {@code delete} are true when granted, null when not.
 */
public record TablePermission(
    String table, Privileges select, Boolean insert, Boolean update, Boolean delete) {}
