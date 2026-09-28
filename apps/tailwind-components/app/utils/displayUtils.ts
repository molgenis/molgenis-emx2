import type { IColumn, IRow } from "../../../metadata-utils/src/types";
import type { DisplayConfig, Layout, ResolvedDisplay } from "../types/display";

import { columnValueToString } from "./columnValueToString";

const DEFAULT_LAYOUT: Layout = "TABLE";
const MAX_DETAIL_COLUMNS = 5;

export function resolveLayout(settings?: DisplayConfig): Layout {
  return settings?.layout ?? DEFAULT_LAYOUT;
}

export function resolveDisplay(
  columns: IColumn[],
  settings?: DisplayConfig
): ResolvedDisplay {
  const titleColumns = defaultTitleColumns(columns);
  const descriptionColumn = settings?.descriptionColumnId
    ? columns.find((column) => column.id === settings.descriptionColumnId)
    : defaultDescriptionColumn(columns);

  const usedColumnIds = new Set([
    ...titleColumns.map((column) => column.id),
    ...(descriptionColumn ? [descriptionColumn.id] : []),
  ]);

  return {
    layout: resolveLayout(settings),
    titleTemplate: settings?.titleTemplate ?? asTemplate(titleColumns),
    subtitleTemplate: settings?.subtitleTemplate,
    descriptionColumn,
    detailColumns: settings?.detailColumnIds
      ? findColumnsByIds(columns, settings.detailColumnIds)
      : defaultDetailColumns(columns, usedColumnIds),
    logoColumn: settings?.logoColumnId
      ? columns.find((column) => column.id === settings.logoColumnId)
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

export function resolveTitleAndSubtitle(
  row: IRow,
  titleTemplate: string,
  subtitleTemplate?: string
): { title: string; subtitle?: string } {
  const title = titleTemplate
    ? columnValueToString(row, titleTemplate) ?? ""
    : "";
  const subtitle = subtitleTemplate
    ? columnValueToString(row, subtitleTemplate) || undefined
    : undefined;

  if (!title && subtitle) {
    return { title: subtitle };
  }
  return { title, subtitle };
}
