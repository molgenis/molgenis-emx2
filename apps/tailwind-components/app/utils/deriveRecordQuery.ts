import type {
  IColumn,
  ISchemaMetaData,
  ITableMetaData,
} from "../../../metadata-utils/src/types";
import type { RecordListConfig } from "../../types/recordLists";
import { resolveDisplay } from "./displayUtils";

export const NESTED_LIST_LIMIT = 10;

const FILE_FRAGMENT = "{ id, size, filename, extension, url }";
const PAGED_TYPES = ["REF_ARRAY", "REFBACK"];

export function deriveRecordQuery(
  schema: ISchemaMetaData,
  tableId: string,
  lists: RecordListConfig = {}
): string {
  const fields = selectableColumns(findTable(schema, tableId).columns)
    .map((column) => rootSelection(schema, column, lists))
    .join(" ");
  return `query ($filter: ${tableId}Filter) { ${tableId}(filter: $filter, limit: 1) { ${fields} } }`;
}

function findTable(schema: ISchemaMetaData, tableId: string): ITableMetaData {
  const table = schema.tables.find((candidate) => candidate.id === tableId);
  if (!table) {
    throw new Error(`Table ${tableId} not in schema ${schema.id}`);
  }
  return table;
}

function selectableColumns(columns: IColumn[]): IColumn[] {
  return columns.filter(
    (column) =>
      column.columnType !== "SECTION" &&
      column.columnType !== "HEADING" &&
      !column.id.startsWith("mg_")
  );
}

function rootSelection(
  schema: ISchemaMetaData,
  column: IColumn,
  lists: RecordListConfig
): string {
  const entry = lists[column.id];
  if (!entry || !column.refTableId) {
    return columnSelection(schema, column);
  }
  const target = findTable(schema, column.refTableId);
  const shown = resolveDisplay(selectableColumns(target.columns), entry);
  const shownIds = new Set(
    [
      shown.titleColumn,
      shown.subtitleColumn,
      shown.descriptionColumn,
      shown.logoColumn,
      ...shown.detailColumns,
    ].flatMap((shownColumn) => (shownColumn ? [shownColumn.id] : []))
  );
  const fields = selectableColumns(target.columns)
    .filter(
      (candidate) =>
        (candidate.key === 1 || shownIds.has(candidate.id)) &&
        !PAGED_TYPES.includes(candidate.columnType)
    )
    .map((candidate) => columnSelection(schema, candidate))
    .join(" ");
  const paged = PAGED_TYPES.includes(column.columnType);
  return `${column.id}${
    paged ? `(limit: ${NESTED_LIST_LIMIT})` : ""
  } { ${fields} }${paged ? ` ${column.id}_agg { count }` : ""}`;
}

function columnSelection(schema: ISchemaMetaData, column: IColumn): string {
  if (column.columnType === "FILE") {
    return `${column.id} ${FILE_FRAGMENT}`;
  }
  if (!column.refTableId) {
    return column.id;
  }
  const paths = labelPaths(column);
  if (column.columnType.startsWith("ONTOLOGY")) {
    paths.push(["name"], ["label"]);
  }
  return `${column.id} { ${pathSelection(schema, column.refTableId, paths)} }`;
}

function labelPaths(column: IColumn): string[][] {
  const template = column.refLabel ?? column.refLabelDefault ?? "";
  const expressions = [...template.matchAll(/\$\{([^}]*)\}/g)].map(
    (match) => match[1] ?? ""
  );
  return expressions.flatMap((expression) =>
    [...expression.matchAll(/[A-Za-z_$][\w$]*(?:\??\.[A-Za-z_$][\w$]*)*/g)].map(
      (match) => match[0].split(/\??\./)
    )
  );
}

function pathSelection(
  schema: ISchemaMetaData,
  tableId: string,
  paths: string[][]
): string {
  const columns = selectableColumns(findTable(schema, tableId).columns);
  // A path segment that is no column is JS in the template, such as toUpperCase; it drops out here.
  return columns
    .flatMap((column) => {
      const subPaths = paths
        .filter(([head, ...rest]) => head === column.id && rest.length > 0)
        .map(([, ...rest]) => rest);
      const named = paths.some(([head]) => head === column.id);
      if (column.key !== 1 && !named) {
        return [];
      }
      if (column.refTableId && column.columnType !== "FILE") {
        return [
          `${column.id} { ${pathSelection(
            schema,
            column.refTableId,
            subPaths
          )} }`,
        ];
      }
      return [columnSelection(schema, column)];
    })
    .join(" ");
}
