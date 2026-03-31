import type { IColumn } from "../../../metadata-utils/src/types";

const REF_COLUMN_TYPES = ["REF", "SELECT", "RADIO"];
const REF_ARRAY_COLUMN_TYPES = ["REF_ARRAY", "MULTISELECT", "CHECKBOX"];

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

export function isRefColumn(columnType: string): boolean {
  return REF_COLUMN_TYPES.includes(columnType);
}

export function isRefArrayColumn(columnType: string): boolean {
  return REF_ARRAY_COLUMN_TYPES.includes(columnType);
}

export function getDetailColumns(
  columns: IColumn[],
  data: Record<string, any>
): IColumn[] {
  const explicit = columns.filter(
    (c) => c.role === "DETAIL" && data[c.id] != null
  );
  if (explicit.length > 0) return explicit;
  return columns
    .filter(
      (c) =>
        !c.role &&
        (!c.key || c.key === 0) &&
        c.columnType !== "HEADING" &&
        c.columnType !== "SECTION" &&
        !c.id.startsWith("mg_") &&
        data[c.id] != null
    )
    .slice(0, 5);
}

export function getDescriptionColumn(
  columns: IColumn[],
  data: Record<string, any>
): IColumn | undefined {
  return columns.find((c) => c.role === "DESCRIPTION" && data[c.id] != null);
}

export function getLogoColumn(
  columns: IColumn[],
  data: Record<string, any>
): IColumn | undefined {
  return columns.find((c) => c.role === "LOGO" && data[c.id] != null);
}

export function getRoleText(value: any): string {
  if (value == null) return "";
  if (typeof value === "string") return value;
  if (Array.isArray(value))
    return value.map((item) => item?.name ?? String(item)).join(", ");
  if (typeof value === "object" && value.name) return value.name;
  if (typeof value === "object")
    return Object.values(value).filter(Boolean).join(" ");
  return String(value);
}

export function getTitleText(
  columns: IColumn[],
  data: Record<string, any>
): string {
  return columns
    .filter((c) => c.role === "TITLE")
    .map((c) => getRoleText(data[c.id]))
    .filter(Boolean)
    .join(" ");
}

export function getSubtitleText(
  columns: IColumn[],
  data: Record<string, any>
): string {
  return columns
    .filter((c) => c.role === "SUBTITLE")
    .map((c) => getRoleText(data[c.id]))
    .filter(Boolean)
    .join(" ");
}

export function filterColumnsByRole(columns: IColumn[]): IColumn[] {
  const nonInternal = columns.filter((c) => c.role !== "INTERNAL");
  const withRoles = nonInternal.filter((c) => c.role);
  if (withRoles.length === 0) return nonInternal;
  const titleCols = withRoles.filter((c) => c.role === "TITLE");
  const otherCols = withRoles.filter((c) => c.role !== "TITLE");
  return [...titleCols, ...otherCols];
}

export function getListColumns(
  columns: IColumn[],
  options?: {
    layout?: "TABLE" | "CARDS" | "LIST" | "LINKS";
    hideColumns?: string[];
    visibleColumns?: string[];
    rows?: Record<string, any>[];
  }
): IColumn[] {
  const opts = options || {};
  const hiddenSet = new Set(opts.hideColumns || []);

  let result = columns.filter(
    (c) =>
      c.role !== "INTERNAL" &&
      c.columnType !== "SECTION" &&
      c.columnType !== "HEADING" &&
      !c.id.startsWith("mg_") &&
      !hiddenSet.has(c.id)
  );

  if (opts.visibleColumns?.length) {
    result = opts.visibleColumns
      .map((id) => result.find((c) => c.id === id))
      .filter(Boolean) as IColumn[];
  } else if (opts.layout && opts.layout !== "TABLE") {
    const roleCols = result.filter((c) => c.role);
    if (roleCols.length > 0) {
      const titleCols = roleCols.filter((c) => c.role === "TITLE");
      const otherCols = roleCols.filter((c) => c.role !== "TITLE");
      result = [...titleCols, ...otherCols];
    } else {
      const keyCols = result.filter((c) => c.key && c.key > 0);
      const otherCols = result
        .filter((c) => !c.key || c.key === 0)
        .slice(0, 5 - keyCols.length);
      result = [...keyCols, ...otherCols];
    }
  }

  if (opts.rows?.length) {
    result = result.filter((col) =>
      opts.rows!.some((row) => !isEmptyValue(row[col.id]))
    );
  }

  return result;
}

export function hasOntologyHierarchy(value: any): boolean {
  if (!Array.isArray(value)) return false;
  return value.some((item) => item?.parent != null);
}

export function buildRefHref(
  schemaId: string,
  refTableId: string,
  refSchemaId: string | undefined,
  rowKey: Record<string, any>
): string {
  const schema = refSchemaId || schemaId;
  const slug = Object.values(rowKey)
    .filter((v) => typeof v === "string" || typeof v === "number")
    .join("-");
  return `/${schema}/${refTableId}/${encodeURIComponent(
    slug
  )}?keys=${encodeURIComponent(JSON.stringify(rowKey))}`;
}
