import {
  isColumnValueObject,
  type IColumn,
  type IRow,
  type ISchemaMetaData,
  type ITableMetaData,
  type columnValue,
  type nonEmptyColumnValue,
} from "../../../metadata-utils/src/types";
import { isPartsType } from "../../../metadata-utils/src";
import type { Crumb } from "../../types/types";

export interface RecordPathLevel {
  tableSegment: string;
  keySegment: string;
  tableId: string;
  key: Record<string, nonEmptyColumnValue>;
}

export interface ContextualList {
  levels: RecordPathLevel[];
  tableId: string;
  partsColumnId: string;
  partOfColumnId: string;
  contextColumnIds: string[];
}

interface PartsDeclaration {
  declaringTableId: string;
  partsColumnId: string;
  partOfColumnId: string;
}

export function buildNestedRecordPath(
  schema: ISchemaMetaData,
  tableId: string,
  row: IRow
): string | null {
  const levels = buildRecordLevels(schema, tableId, row);
  if (!levels || levels.length < 2) return null;
  return levelsToPath(schema.id, levels);
}

export function parseRecordPath(
  schema: ISchemaMetaData,
  path: string
): RecordPathLevel[] | null {
  const pathname = path.split("?")[0] ?? "";
  const segments = pathname
    .split("/")
    .filter(Boolean)
    .map((segment) => decodeURIComponent(segment));
  const dataSegments = segments.slice(1);
  if (dataSegments.length < 2 || dataSegments.length % 2 !== 0) return null;

  const keysParam = new URLSearchParams(path.split("?")[1] ?? "").get("keys");
  if (dataSegments.length === 2 && keysParam) {
    return parseFlatLevel(
      schema,
      dataSegments[0]!,
      dataSegments[1]!,
      keysParam
    );
  }

  const levels: RecordPathLevel[] = [];
  for (let index = 0; index < dataSegments.length; index += 2) {
    const tableSegment = dataSegments[index]!;
    const keySegment = dataSegments[index + 1]!;
    const parent = levels[levels.length - 1];
    const level = parent
      ? parseNestedLevel(schema, parent, tableSegment, keySegment)
      : parseRootLevel(schema, tableSegment, keySegment);
    if (!level) return null;
    levels.push(level);
  }
  return levels;
}

export function parseContextualListPath(
  schema: ISchemaMetaData,
  path: string
): ContextualList | null {
  const pathname = path.split("?")[0] ?? "";
  const segments = pathname.split("/").filter(Boolean);
  if (segments.length < 4 || segments.length % 2 !== 0) return null;

  const levels = parseRecordPath(schema, `/${segments.slice(0, -1).join("/")}`);
  if (!levels) return null;

  const parent = levels[levels.length - 1]!;
  const partsColumnId = decodeURIComponent(segments[segments.length - 1]!);
  const partsColumn = findTable(schema, parent.tableId)?.columns.find(
    (column) => column.id === partsColumnId && isPartsType(column.columnType)
  );
  if (!partsColumn?.refTableId || !partsColumn.refBackId) return null;

  const table = findTable(schema, partsColumn.refTableId);
  if (!table) return null;

  return {
    levels,
    tableId: table.id,
    partsColumnId,
    partOfColumnId: partsColumn.refBackId,
    contextColumnIds: pinnedByParentColumnIds(table, partsColumn.refBackId),
  };
}

export function buildContextualListPath(
  schema: ISchemaMetaData,
  parentTableId: string,
  parentRow: IRow,
  partsColumnId: string
): string | null {
  const parentTable = findTable(schema, parentTableId);
  const partsColumn = parentTable?.columns.find(
    (column) => column.id === partsColumnId && isPartsType(column.columnType)
  );
  if (!partsColumn) return null;
  const levels = buildRecordLevels(schema, parentTableId, parentRow);
  if (!levels) return null;
  return `${levelsToPath(schema.id, levels)}/${partsColumnId}`;
}

export function buildCrumbs(
  schema: ISchemaMetaData,
  levels: RecordPathLevel[],
  recordLabels: (string | undefined)[] = []
): Crumb[] {
  const crumbs: Crumb[] = [
    { url: `/${schema.id}`, label: schema.label || schema.id },
  ];
  levels.forEach((level, index) => {
    const listUrl = `${crumbs[crumbs.length - 1]!.url}/${level.tableSegment}`;
    crumbs.push({
      url: listUrl,
      label: findTable(schema, level.tableId)?.label || level.tableId,
    });
    crumbs.push({
      url: `${listUrl}/${encodeURIComponent(level.keySegment)}`,
      label: recordLabels[index] || level.keySegment,
    });
  });
  return crumbs;
}

export function ownLabelColumns(
  schema: ISchemaMetaData,
  level: RecordPathLevel,
  parent?: RecordPathLevel
): IColumn[] {
  const table = findTable(schema, level.tableId);
  if (!table) return [];
  if (!parent) return table.columns;
  const partsColumn = findTable(schema, parent.tableId)?.columns.find(
    (column) =>
      column.id === level.tableSegment && isPartsType(column.columnType)
  );
  if (!partsColumn?.refBackId) return table.columns;
  const contextColumnIds = pinnedByParentColumnIds(
    table,
    partsColumn.refBackId
  );
  return table.columns.filter(
    (column) => !contextColumnIds.includes(column.id)
  );
}

export function subclassTableId(
  schema: ISchemaMetaData,
  row: IRow
): string | undefined {
  const tableClass = row.mg_tableclass;
  if (typeof tableClass !== "string") return undefined;
  const tableName = tableClass.slice(tableClass.indexOf(".") + 1);
  return schema.tables.find(
    (table) => table.name === tableName || table.id === tableName
  )?.id;
}

function parseFlatLevel(
  schema: ISchemaMetaData,
  tableSegment: string,
  keySegment: string,
  keysParam: string
): RecordPathLevel[] | null {
  const table = findTable(schema, tableSegment);
  if (!table) return null;
  try {
    return [
      {
        tableSegment,
        keySegment,
        tableId: table.id,
        key: JSON.parse(keysParam),
      },
    ];
  } catch (error) {
    console.error(`Cannot decode keys of record path ${tableSegment}`, error);
    return null;
  }
}

function parseRootLevel(
  schema: ISchemaMetaData,
  tableSegment: string,
  keySegment: string
): RecordPathLevel | null {
  const table = findTable(schema, tableSegment);
  if (!table) return null;
  const ownKeyColumn = findOwnKeyColumn(table, undefined);
  if (!ownKeyColumn) return null;
  return {
    tableSegment,
    keySegment,
    tableId: table.id,
    key: { [ownKeyColumn.id]: keySegment },
  };
}

function parseNestedLevel(
  schema: ISchemaMetaData,
  parent: RecordPathLevel,
  tableSegment: string,
  keySegment: string
): RecordPathLevel | null {
  const parentTable = findTable(schema, parent.tableId);
  const partsColumn = parentTable?.columns.find(
    (column) => column.id === tableSegment && isPartsType(column.columnType)
  );
  if (!partsColumn?.refTableId || !partsColumn.refBackId) return null;
  const table = findTable(schema, partsColumn.refTableId);
  if (!table) return null;
  const ownKeyColumn = findOwnKeyColumn(table, partsColumn.refBackId);
  if (!ownKeyColumn) return null;
  return {
    tableSegment,
    keySegment,
    tableId: table.id,
    key: composeKey(
      table,
      partsColumn.refBackId,
      ownKeyColumn.id,
      keySegment,
      parent.key
    ),
  };
}

function buildRecordLevels(
  schema: ISchemaMetaData,
  declaredTableId: string,
  row: IRow
): RecordPathLevel[] | null {
  const tableId = subclassTableId(schema, row) ?? declaredTableId;
  const table = findTable(schema, tableId);
  if (!table) return null;

  const declaration = findPartsDeclaration(schema, tableId);
  const ownKeyColumn = findOwnKeyColumn(table, declaration?.partOfColumnId);
  if (!ownKeyColumn) return null;

  const ownKeyValue = row[ownKeyColumn.id];
  if (!isScalarKeyValue(ownKeyValue)) return null;

  if (!declaration) {
    return [
      {
        tableSegment: tableId,
        keySegment: String(ownKeyValue),
        tableId,
        key: { [ownKeyColumn.id]: ownKeyValue },
      },
    ];
  }

  const parentValue = row[declaration.partOfColumnId];
  if (!isColumnValueObject(parentValue)) return null;
  const parentLevels = buildRecordLevels(
    schema,
    declaration.declaringTableId,
    parentValue
  );
  if (!parentLevels) return null;
  const parentKey = parentLevels[parentLevels.length - 1]!.key;

  return [
    ...parentLevels,
    {
      tableSegment: declaration.partsColumnId,
      keySegment: String(ownKeyValue),
      tableId,
      key: composeKey(
        table,
        declaration.partOfColumnId,
        ownKeyColumn.id,
        ownKeyValue,
        parentKey
      ),
    },
  ];
}

function levelsToPath(schemaId: string, levels: RecordPathLevel[]): string {
  return levels.reduce(
    (path, level) =>
      `${path}/${level.tableSegment}/${encodeURIComponent(level.keySegment)}`,
    `/${schemaId}`
  );
}

function composeKey(
  table: ITableMetaData,
  partOfColumnId: string,
  ownKeyColumnId: string,
  ownKeyValue: nonEmptyColumnValue,
  parentKey: Record<string, nonEmptyColumnValue>
): Record<string, nonEmptyColumnValue> {
  const key: Record<string, nonEmptyColumnValue> = {};
  for (const keyColumn of primaryKeyColumns(table)) {
    if (keyColumn.id === partOfColumnId) {
      key[keyColumn.id] = parentKey;
    } else if (keyColumn.id === ownKeyColumnId) {
      key[keyColumn.id] = ownKeyValue;
    } else if (parentKey[keyColumn.id] !== undefined) {
      key[keyColumn.id] = parentKey[keyColumn.id]!;
    }
  }
  return key;
}

function findPartsDeclaration(
  schema: ISchemaMetaData,
  childTableId: string
): PartsDeclaration | null {
  const childTableIds = inheritanceChain(schema, childTableId);
  const candidates = schema.tables.flatMap((table) =>
    table.columns
      .filter(
        (candidate) =>
          isPartsType(candidate.columnType) &&
          candidate.refTableId !== undefined &&
          childTableIds.includes(candidate.refTableId)
      )
      .map((column) => ({ table, column }))
  );
  const declared = candidates.find(({ column }) => !column.inherited);
  const declaration = declared ?? candidates[0];
  if (!declaration?.column.refBackId) return null;
  return {
    declaringTableId: declaration.table.id,
    partsColumnId: declaration.column.id,
    partOfColumnId: declaration.column.refBackId,
  };
}

function inheritanceChain(schema: ISchemaMetaData, tableId: string): string[] {
  const chain: string[] = [];
  let currentTableId: string | undefined = tableId;
  while (currentTableId && !chain.includes(currentTableId)) {
    chain.push(currentTableId);
    currentTableId = findTable(schema, currentTableId)?.inheritId;
  }
  return chain;
}

function findOwnKeyColumn(
  table: ITableMetaData,
  partOfColumnId: string | undefined
): IColumn | null {
  const inheritedFromParent = partOfColumnId
    ? refLinkClosure(table, partOfColumnId)
    : new Set<string>();
  const ownKeyColumns = primaryKeyColumns(table).filter(
    (column) => !inheritedFromParent.has(column.id)
  );
  return ownKeyColumns.length === 1 ? ownKeyColumns[0]! : null;
}

function pinnedByParentColumnIds(
  table: ITableMetaData,
  partOfColumnId: string
): string[] {
  const fixedByParent = refLinkClosure(table, partOfColumnId);
  return primaryKeyColumns(table)
    .filter((column) => fixedByParent.has(column.id))
    .map((column) => column.id);
}

function refLinkClosure(
  table: ITableMetaData,
  partOfColumnId: string
): Set<string> {
  const closure = new Set<string>();
  let columnId: string | undefined = partOfColumnId;
  while (columnId && !closure.has(columnId)) {
    closure.add(columnId);
    columnId = table.columns.find(
      (column) => column.id === columnId
    )?.refLinkId;
  }
  return closure;
}

function primaryKeyColumns(table: ITableMetaData): IColumn[] {
  return table.columns.filter((column) => column.key === 1);
}

function findTable(
  schema: ISchemaMetaData,
  tableId: string
): ITableMetaData | undefined {
  return schema.tables.find((table) => table.id === tableId);
}

function isScalarKeyValue(value: columnValue): value is string | number {
  return typeof value === "string" || typeof value === "number";
}
