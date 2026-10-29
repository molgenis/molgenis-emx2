import type { IColumn } from "../../../metadata-utils/src/types";

export interface VisibleColumnsOptions {
  term?: string;
  showMgColumns?: boolean;
}

export function visibleColumns(
  columns: IColumn[],
  options: VisibleColumnsOptions = {}
): IColumn[] {
  const term = options.term?.trim().toLowerCase();
  return columns.filter((column) => {
    if (column.columnType === "HEADING" || column.columnType === "SECTION") {
      return true;
    }
    if (!options.showMgColumns && column.id.startsWith("mg_")) {
      return false;
    }
    return !term || column.label.toLowerCase().includes(term);
  });
}
