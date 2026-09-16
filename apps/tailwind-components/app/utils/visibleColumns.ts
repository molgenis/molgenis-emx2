import type { IColumn } from "../../../metadata-utils/src/types";

export interface VisibleColumnsOptions {
  /** Keep only fields whose label contains this, case-insensitively. */
  term?: string;
  /** Keep the `mg_` system columns, which are hidden by default. */
  showMgColumns?: boolean;
}

/**
 * Which columns a record should be asked to render. Choosing this is the caller's
 * job, so that a record takes columns and needs no switches of its own.
 */
export function visibleColumns(
  columns: IColumn[],
  options: VisibleColumnsOptions = {}
): IColumn[] {
  const term = options.term?.trim().toLowerCase();
  return columns.filter((column) => {
    // HEADING and SECTION carry the structure, so they survive every filter.
    if (column.columnType === "HEADING" || column.columnType === "SECTION") {
      return true;
    }
    if (!options.showMgColumns && column.id.startsWith("mg_")) {
      return false;
    }
    return !term || column.label.toLowerCase().includes(term);
  });
}
