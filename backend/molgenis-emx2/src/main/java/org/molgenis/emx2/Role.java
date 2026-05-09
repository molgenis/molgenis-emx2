package org.molgenis.emx2;

import java.util.List;

/** Describes a role and its table-level permissions. */
public record Role(
    String name,
    boolean isSystemRole,
    List<TablePermission> permissions,
    String description,
    boolean changeOwner,
    boolean changeGroup) {}
