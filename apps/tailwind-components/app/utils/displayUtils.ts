import type { IColumn, IRow } from "../../../metadata-utils/src/types";
import type { DisplayConfig, ResolvedDisplay } from "../types/display";
import { columnValueToString } from "./columnValueToString";

const DEFAULT_LAYOUT = "TABLE";
const MAX_DETAIL_COLUMNS = 5;

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
    layout: settings?.layout ?? DEFAULT_LAYOUT,
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

// Mirrors catalogue's `resource.acronym || resource.name` /
// `resource.acronym ? resource.name : ""`: when the title is empty, the
// subtitle carries the record's identity, so it promotes into the title slot
// and leaves no subtitle behind.
export function resolveTitleAndSubtitle(
  row: IRow,
  resolvedDisplay: Pick<ResolvedDisplay, "titleTemplate" | "subtitleTemplate">
): { title: string; subtitle?: string } {
  const title = resolvedDisplay.titleTemplate
    ? columnValueToString(row, resolvedDisplay.titleTemplate) ?? ""
    : "";
  const subtitle = resolvedDisplay.subtitleTemplate
    ? columnValueToString(row, resolvedDisplay.subtitleTemplate) || undefined
    : undefined;

  if (!title && subtitle) {
    return { title: subtitle };
  }
  return { title, subtitle };
}
