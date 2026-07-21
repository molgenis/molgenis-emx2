import type {
  columnId,
  columnValue,
  IColumn,
  ISchemaMetaData,
  ITableMetaData,
} from "./types";

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
      // moduleName is a table NAME (from the MODULE_ARRAY discriminator's values), matched against table.name
      const moduleTable = schema.tables.find(
        (table) => table.name === moduleName
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

export function isModuleColumn(col: IColumn, tableName: string): boolean {
  return col.inherited !== true && !!col.table && col.table !== tableName;
}

export function omitInactiveModuleValues(
  values: Record<columnId, columnValue>,
  table: ITableMetaData,
  activeModuleNames: Set<string>
): Record<columnId, columnValue> {
  return Object.fromEntries(
    Object.entries(values).filter(([key]) => {
      const col = table.columns.find((c) => c.id === key);
      if (
        col &&
        isModuleColumn(col, table.name) &&
        !activeModuleNames.has(col.table!)
      ) {
        return false;
      }
      return true;
    })
  );
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

  const moduleColumns = rootTable.columns.filter(
    (col) => col.columnType === "MODULE"
  );

  for (const axisColumn of moduleColumns) {
    const discriminatorValue = row[axisColumn.id];
    if (typeof discriminatorValue === "string" && discriminatorValue !== "") {
      active.add(discriminatorValue);
    }
  }

  return active;
}
