import {
  isColumnValueObject,
  type columnValue,
  type IColumn,
  type IRow,
  type ISchemaMetaData,
  type ITableMetaData,
} from "../../../metadata-utils/src/types";
import type { Crumb } from "../../types/types";
import type { TableConfig } from "../../types/recordConfig";

export interface ResolvedRecordPath {
  tableId: string;
  filter: Record<string, unknown>;
  crumbs: Crumb[];
}

export function resolveRecordPath(
  schema: ISchemaMetaData,
  configs: Record<string, TableConfig>,
  basePath: string,
  segments: string[]
): ResolvedRecordPath {
  const [rootId, rootKey, ...nested] = segments;
  if (rootId === undefined || rootKey === undefined || nested.length % 2) {
    throw new Error(`Not a record path: ${segments.join("/")}`);
  }
  let table = findTable(schema, rootId);
  let key = keyOf(keyColumns(table), rootKey);
  let url = `${basePath}/${urlSegment(rootId)}/${urlSegment(rootKey)}`;
  const crumbs: Crumb[] = [
    { label: rootId, url: "" },
    { label: keyLabel(rootKey), url },
  ];
  for (let i = 0; i < nested.length; i += 2) {
    const route = nested[i] ?? "";
    const keyText = nested[i + 1] ?? "";
    const column = table.columns.find(
      (candidate) =>
        candidate.refTableId &&
        configs[table.id]?.columns?.[candidate.id]?.route === route
    );
    if (!column?.refTableId) {
      throw new Error(`Table ${table.id} has no nested route ${route}`);
    }
    const child = findTable(schema, column.refTableId);
    const parentKey = key;
    key = {
      ...(column.columnType === "REFBACK" && column.refBackId
        ? { [column.refBackId]: parentKey }
        : {}),
      ...keyOf(keyColumns(child, column), keyText),
    };
    url = `${url}/${urlSegment(route)}/${urlSegment(keyText)}`;
    crumbs.push({ label: route, url: "" }, { label: keyLabel(keyText), url });
    table = child;
  }
  return { tableId: table.id, filter: equalsFilter(key), crumbs };
}

export function nestedRecordLink(
  recordPath: string,
  column: IColumn,
  route: string,
  target: ITableMetaData,
  row: IRow
): string {
  const values = keyColumns(target, column).map((keyColumn) =>
    encodeValue(String(row[keyColumn.id]))
  );
  return `${recordPath}/${urlSegment(route)}/${urlSegment(values.join(","))}`;
}

export function equalsFilter(value: columnValue): Record<string, unknown> {
  return isColumnValueObject(value)
    ? Object.fromEntries(
        Object.entries(value).map(([name, nested]) => [
          name,
          equalsFilter(nested),
        ])
      )
    : { equals: value };
}

function findTable(schema: ISchemaMetaData, tableId: string): ITableMetaData {
  const table = schema.tables.find((candidate) => candidate.id === tableId);
  if (!table) {
    throw new Error(`Table ${tableId} not in schema ${schema.id}`);
  }
  return table;
}

function keyColumns(table: ITableMetaData, via?: IColumn): IColumn[] {
  const filled = via?.columnType === "REFBACK" ? via.refBackId : undefined;
  const columns = table.columns.filter(
    (column) => column.key === 1 && column.id !== filled
  );
  const reference = columns.find((column) => column.refTableId);
  if (reference) {
    throw new Error(
      `Key column ${table.id}.${reference.id} is a reference, so ${table.id} has no nested route`
    );
  }
  return columns;
}

function keyOf(columns: IColumn[], keyText: string): IRow {
  const values = keyText.split(",").map(decodeValue);
  if (values.length !== columns.length) {
    throw new Error(
      `Key "${keyText}" needs ${columns.length} value(s): ${columns
        .map((column) => column.id)
        .join(", ")}`
    );
  }
  return Object.fromEntries(
    columns.map((column, index) => [column.id, values[index]])
  );
}

function keyLabel(keyText: string): string {
  return keyText.split(",").map(decodeValue).join(", ");
}

// "%" in a path does not survive the server render, so a comma inside a value is "!c" and "!" is "!!".
function encodeValue(value: string): string {
  return value.replace(/[!,]/g, (char) => (char === "," ? "!c" : "!!"));
}

function decodeValue(value: string): string {
  return value.replace(/!(.)/g, (_, char) => (char === "c" ? "," : char));
}

function urlSegment(segment: string): string {
  return segment.split(",").map(encodeURIComponent).join(",");
}
