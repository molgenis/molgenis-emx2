import type { IColumn } from "../../../metadata-utils/src/types";

export function isEmptyValue(val: any): boolean {
  if (val === null || val === undefined || val === "") return true;
  if (Array.isArray(val) && val.length === 0) return true;
  if (
    typeof val === "object" &&
    !Array.isArray(val) &&
    Object.keys(val).length === 0
  )
    return true;
  return false;
}

export function isTopSection(
  column: { id: string; label?: string } | null | undefined
): boolean {
  if (!column) return false;
  return (
    column.id === "_top" ||
    column.id === "mg_top_of_form" ||
    column.label === "_top"
  );
}

export function buildRefbackFilter(
  columnType: string,
  refBackId: string | undefined,
  parentRowId: Record<string, any> | undefined
): Record<string, any> | undefined {
  if (columnType === "REFBACK" && refBackId && parentRowId) {
    const keyFilter: Record<string, any> = {};
    for (const [key, val] of Object.entries(parentRowId)) {
      keyFilter[key] = { equals: val };
    }
    return { [refBackId]: keyFilter };
  }
  return undefined;
}

export function getRowLabel(
  row: Record<string, any>,
  rowLabelTemplate?: string
): string {
  if (rowLabelTemplate) {
    return rowLabelTemplate.replace(/\$\{(\w+)\}/g, (_, key) => row[key] ?? "");
  }
  return row.label || row.name || row.id || JSON.stringify(row);
}

export function filterDataColumns(
  columns: IColumn[],
  hideColumns?: string[]
): IColumn[] {
  const hiddenSet = new Set(hideColumns || []);
  return columns.filter(
    (c) =>
      c.columnType !== "SECTION" &&
      c.columnType !== "HEADING" &&
      !c.id.startsWith("mg_") &&
      !hiddenSet.has(c.id)
  );
}

export function filterNonEmptyColumns(
  columns: IColumn[],
  rows: Record<string, any>[]
): IColumn[] {
  if (!rows.length) return columns;
  return columns.filter((col) =>
    rows.some((row) => !isEmptyValue(row[col.id]))
  );
}
