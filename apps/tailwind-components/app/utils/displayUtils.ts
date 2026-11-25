import type {
  ColumnRole,
  IColumn,
  IRow,
} from "../../../metadata-utils/src/types";
import type { ResolvedDisplay } from "../types/display";

const MAX_DETAIL_COLUMNS = 5;

export function resolveDisplay(allColumns: IColumn[]): ResolvedDisplay {
  const columns = allColumns.filter((column) => column.role !== "INTERNAL");
  if (columns.some((column) => column.role)) {
    const withRole = (role: ColumnRole) =>
      columns.find((column) => column.role === role);
    return {
      titleColumn: withRole("TITLE"),
      subtitleColumn: withRole("SUBTITLE"),
      descriptionColumn: withRole("DESCRIPTION"),
      logoColumn: withRole("LOGO"),
      detailColumns: columns.filter((column) => column.role === "DETAIL"),
    };
  }
  const titleColumn = defaultTitleColumn(columns);
  const descriptionColumn = defaultDescriptionColumn(columns);
  return {
    titleColumn,
    descriptionColumn,
    detailColumns: defaultDetailColumns(
      columns,
      new Set(
        [titleColumn, descriptionColumn].flatMap((column) =>
          column ? [column.id] : []
        )
      )
    ),
  };
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
