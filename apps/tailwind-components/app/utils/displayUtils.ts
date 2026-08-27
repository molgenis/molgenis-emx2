import type { IColumn } from "../../../metadata-utils/src/types";
import type { DisplayConfig, ResolvedDisplay } from "../types/display";

const DEFAULT_LAYOUT = "TABLE";
const MAX_DETAIL_COLUMNS = 5;

export function resolveDisplay(
  columns: IColumn[],
  config?: DisplayConfig
): ResolvedDisplay {
  const titleColumns = defaultTitleColumns(columns);
  const descriptionColumn = defaultDescriptionColumn(columns);

  const usedColumnIds = new Set([
    ...titleColumns.map((column) => column.id),
    ...(descriptionColumn ? [descriptionColumn.id] : []),
  ]);

  return {
    layout: config?.layout ?? DEFAULT_LAYOUT,
    titleTemplate: config?.titleTemplate ?? asTemplate(titleColumns),
    descriptionTemplate:
      config?.descriptionTemplate ??
      (descriptionColumn ? asTemplate([descriptionColumn]) : undefined),
    detailColumns: config?.detailColumns
      ? findColumnsByIds(columns, config.detailColumns)
      : defaultDetailColumns(columns, usedColumnIds),
    logoColumn: config?.logoColumn
      ? columns.find((column) => column.id === config.logoColumn)
      : undefined,
  };
}

function defaultTitleColumns(columns: IColumn[]): IColumn[] {
  return columns
    .filter((column) => column.key === 1)
    .sort((a, b) => (a.position ?? 0) - (b.position ?? 0));
}

function defaultDescriptionColumn(columns: IColumn[]): IColumn | undefined {
  return columns.find((column) => column.columnType === "TEXT");
}

function defaultDetailColumns(
  columns: IColumn[],
  usedColumnIds: Set<string>
): IColumn[] {
  return columns
    .filter(
      (column) =>
        !column.key &&
        column.columnType !== "HEADING" &&
        column.columnType !== "SECTION" &&
        !column.id.startsWith("mg_") &&
        !usedColumnIds.has(column.id)
    )
    .slice(0, MAX_DETAIL_COLUMNS);
}

function findColumnsByIds(columns: IColumn[], ids: string[]): IColumn[] {
  return ids
    .map((id) => columns.find((column) => column.id === id))
    .filter((column): column is IColumn => column !== undefined);
}

function asTemplate(columns: IColumn[]): string {
  return columns.map((column) => `\${${column.id}}`).join(" ");
}
