package org.molgenis.emx2;

import java.util.List;

/** Describes a role and its table-level permissions. */
public record Role(
    String name, String description, boolean isSystemRole, List<TablePermission> permissions) {}
