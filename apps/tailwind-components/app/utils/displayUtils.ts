import type { IColumn, IRow } from "../../../metadata-utils/src/types";
import type { DisplayConfig, Layout, ResolvedDisplay } from "../types/display";

const DEFAULT_LAYOUT: Layout = "TABLE";
const MAX_DETAIL_COLUMNS = 5;

export function resolveLayout(settings?: DisplayConfig): Layout {
  return settings?.layout ?? DEFAULT_LAYOUT;
}

export function resolveDisplay(
  columns: IColumn[],
  settings?: DisplayConfig
): ResolvedDisplay {
  const titleColumn = settings?.titleColumnId
    ? findColumn(columns, settings.titleColumnId)
    : defaultTitleColumn(columns);
  const subtitleColumn = findColumn(columns, settings?.subtitleColumnId);
  const descriptionColumn = settings?.descriptionColumnId
    ? columns.find((column) => column.id === settings.descriptionColumnId)
    : defaultDescriptionColumn(columns);

  const usedColumnIds = new Set([
    ...(titleColumn ? [titleColumn.id] : []),
    ...(subtitleColumn ? [subtitleColumn.id] : []),
    ...(descriptionColumn ? [descriptionColumn.id] : []),
  ]);

  return {
    layout: resolveLayout(settings),
    titleColumn,
    subtitleColumn,
    descriptionColumn,
    detailColumns: settings?.detailColumnIds
      ? findColumnsByIds(columns, settings.detailColumnIds)
      : defaultDetailColumns(columns, usedColumnIds),
    logoColumn: settings?.logoColumnId
      ? columns.find((column) => column.id === settings.logoColumnId)
      : undefined,
  };
}

function findColumn(columns: IColumn[], id?: string): IColumn | undefined {
  return id ? columns.find((column) => column.id === id) : undefined;
}

function defaultTitleColumn(columns: IColumn[]): IColumn | undefined {
  const keyColumns = columns.filter((column) => column.key === 1);
  return keyColumns.length === 1 ? keyColumns[0] : undefined;
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

export function plainValue(row: IRow, column?: IColumn): string {
  const value = column ? row[column.id] : undefined;
  return typeof value === "object" || value == null ? "" : String(value);
}

function isEmptyValue(value: unknown): boolean {
  return (
    value == null || value === "" || (Array.isArray(value) && !value.length)
  );
}

export function titleSlots(
  row: IRow,
  titleColumn?: IColumn,
  subtitleColumn?: IColumn
): { title?: IColumn; subtitle?: IColumn } {
  const hasTitle = titleColumn && !isEmptyValue(row[titleColumn.id]);
  const hasSubtitle = subtitleColumn && !isEmptyValue(row[subtitleColumn.id]);
  if (hasTitle) {
    return {
      title: titleColumn,
      subtitle: hasSubtitle ? subtitleColumn : undefined,
    };
  }
  return { title: hasSubtitle ? subtitleColumn : titleColumn };
}
