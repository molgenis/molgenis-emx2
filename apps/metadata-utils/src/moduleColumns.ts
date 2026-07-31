import type {
  columnId,
  columnValue,
  IColumn,
  ISchemaMetaData,
  ITableMetaData,
} from "./types";

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
  rootTable: ITableMetaData,
  schema?: ISchemaMetaData
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

  if (schema) {
    activateAncestorModules(active, schema);
  }

  return active;
}

function activateAncestorModules(
  active: Set<string>,
  schema: ISchemaMetaData
): void {
  const tablesByName = new Map(
    schema.tables.map((table) => [table.name, table])
  );

  const pending = [...active];
  while (pending.length > 0) {
    const moduleName = pending.pop()!;
    const moduleTable = tablesByName.get(moduleName);
    if (!moduleTable) {
      continue;
    }
    for (const parentName of moduleTable.inheritNames ?? []) {
      const parentTable = tablesByName.get(parentName);
      if (
        parentTable &&
        parentTable.tableType === "MODULE" &&
        !active.has(parentName)
      ) {
        active.add(parentName);
        pending.push(parentName);
      }
    }
  }
}
