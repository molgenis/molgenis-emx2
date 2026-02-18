package org.molgenis.emx2;

import java.util.List;

/**
 * Per-column access overrides for a role on a table.
 *
 * <p>Unlisted columns inherit the default from the table-level permission: if the role has UPDATE
 * on the table, unlisted columns are editable; if only SELECT, they are readonly.
 *
 * @param editable columns that are visible and updatable
 * @param readonly columns that are visible but not updatable
 * @param hidden columns that are not visible in API responses
 */
public record ColumnAccess(List<String> editable, List<String> readonly, List<String> hidden) {

  public ColumnAccess {
    editable = editable == null ? null : List.copyOf(editable);
    readonly = readonly == null ? null : List.copyOf(readonly);
    hidden = hidden == null ? null : List.copyOf(hidden);
  }
}
