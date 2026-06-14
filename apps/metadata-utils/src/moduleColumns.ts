import type { IColumn, ISchemaMetaData, ITableMetaData } from "./types";

export interface IModuleColumnGroup {
  moduleName: string;
  columns: IColumn[];
}

export function expandModuleColumns(
  rootTable: ITableMetaData,
  schema: ISchemaMetaData
): IModuleColumnGroup[] {
  const rootColumnIds = new Set(rootTable.columns.map((col) => col.id));

  const moduleArrayColumns = rootTable.columns.filter(
    (col) => col.columnType === "MODULE_ARRAY"
  );

  const groups: IModuleColumnGroup[] = [];

  for (const axisColumn of moduleArrayColumns) {
    const moduleNames = axisColumn.values ?? [];
    for (const moduleName of moduleNames) {
      const moduleTable = schema.tables.find(
        (table) => table.id === moduleName || table.name === moduleName
      );
      if (!moduleTable) {
        continue;
      }
      const localColumns = moduleTable.columns.filter(
        (col) => !rootColumnIds.has(col.id)
      );
      groups.push({ moduleName, columns: localColumns });
    }
  }

  return groups;
}

export function activeModules(
  row: Record<string, unknown>,
  rootTable: ITableMetaData
): Set<string> {
  const active = new Set<string>();

  const moduleArrayColumns = rootTable.columns.filter(
    (col) => col.columnType === "MODULE_ARRAY"
  );

  for (const axisColumn of moduleArrayColumns) {
    const discriminatorValue = row[axisColumn.id];
    if (!Array.isArray(discriminatorValue)) {
      continue;
    }
    for (const entry of discriminatorValue) {
      if (typeof entry === "string") {
        active.add(entry);
      }
    }
  }

  return active;
}
