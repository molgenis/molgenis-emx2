import type { IColumn } from "metadata-utils";

export interface EditorColumn extends Omit<Partial<IColumn>, "table"> {
  table: string;
}

export interface EditorTable {
  name: string;
  oldName?: string;
  drop?: boolean;
  tableType?: string;
  inheritName?: string;
  inheritSchemaName?: string;
  columns?: EditorColumn[];
  subclasses?: EditorTable[];
}

export interface EditorSchema {
  name: string;
  tables?: EditorTable[];
  ontologies?: EditorTable[];
}

// The fields that where computed and should not be saved to the database
const COMPUTED_FIELDS = ["inherited"];

export function applyTableRename(
  rootTable: EditorTable,
  previousName: string,
  newName: string
): void {
  if (previousName === newName) {
    return;
  }
  (rootTable.columns || [])
    .filter((column) => column.table === previousName)
    .forEach((column) => {
      column.table = newName;
    });
}

export function findRootTable(
  tables: EditorTable[] | undefined,
  tableName: string | undefined
): EditorTable | undefined {
  if (tableName === undefined) {
    return undefined;
  }
  return (tables || []).find((table) =>
    withSubclasses(table).some((owner) => owner.name === tableName)
  );
}

export function isColumnOwnerDropped(
  rootTable: EditorTable,
  column: EditorColumn
): boolean {
  const owner = withSubclasses(rootTable).find(
    (table) => table.name === column.table
  );
  return Boolean(owner && owner.drop);
}

export function toSaveTables(schema: EditorSchema): EditorTable[] {
  const tables = schema.tables || [];
  const tableMap: Record<string, EditorTable> = {};
  tables.forEach((table) => {
    withSubclasses(table).forEach((owner) => {
      tableMap[owner.name] = owner;
    });
    delete table.subclasses;
  });
  tables.forEach((table) => {
    (table.columns || []).forEach((column) => {
      if (column.table !== table.name) {
        const owner =
          tableMap[column.table] || ({ columns: [] } as EditorTable);
        owner.columns = owner.columns || [];
        owner.columns.push(column);
        tableMap[column.table] = owner;
      }
    });
  });
  tables.forEach((table) => {
    table.columns = (table.columns || []).filter(
      (column) => column.table === table.name
    );
  });
  const toSave = [...Object.values(tableMap), ...(schema.ontologies || [])];
  return toSave.map(removeComputedFields);
}

function removeComputedFields(table: EditorTable): EditorTable {
  table.columns = table.columns?.filter((col) => {
    COMPUTED_FIELDS.forEach((field) => delete col[field]);
    return col;
  });
  return table;
}

function withSubclasses(rootTable: EditorTable): EditorTable[] {
  return [rootTable, ...(rootTable.subclasses || [])];
}
