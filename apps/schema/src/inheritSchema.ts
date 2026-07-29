import type { EditorSchema, EditorTable } from "./tableModel";

export function toInheritSchemaName(
  selectedSchemaName: string | null | undefined,
  currentSchemaName: string
): string | undefined {
  if (!selectedSchemaName || selectedSchemaName === currentSchemaName) {
    return undefined;
  }
  return selectedSchemaName;
}

export function isCrossSchemaSubclass(
  table: EditorTable,
  currentSchemaName: string
): boolean {
  return (
    Boolean(table.inheritName) &&
    Boolean(toInheritSchemaName(table.inheritSchemaName, currentSchemaName))
  );
}

export function isRootTable(
  table: EditorTable,
  currentSchemaName: string
): boolean {
  return !table.inheritName || isCrossSchemaSubclass(table, currentSchemaName);
}

export function findLocalRootTable(
  schema: EditorSchema,
  table: EditorTable
): EditorTable | undefined {
  if (isRootTable(table, schema.name)) {
    return undefined;
  }
  return (schema.tables || [])
    .filter((candidate) => isRootTable(candidate, schema.name))
    .find(
      (candidate) =>
        candidate.name === table.inheritName ||
        (candidate.subclasses || []).some(
          (subclass) => subclass.name === table.inheritName
        )
    );
}

export function parentTableOptions(
  rootTable: EditorTable,
  tableName: string | undefined
): string[] {
  const siblingNames = (rootTable.subclasses || [])
    .map((subclass) => subclass.name)
    .filter((name) => name !== tableName);
  return [rootTable.name, ...siblingNames];
}

export function resolveInheritName(
  currentInheritName: string | undefined,
  options: string[]
): string | undefined {
  return options.some((option) => option === currentInheritName)
    ? currentInheritName
    : options[0];
}

export function extendableTableNames(
  schema: { tables?: EditorTable[] } | null | undefined
): string[] {
  const tables = schema && schema.tables ? schema.tables : [];
  const names = tables
    .filter((table) => table.tableType !== "ONTOLOGIES" && !table.drop)
    .flatMap((table) => [
      table.name,
      ...(table.subclasses ? table.subclasses.map((sub) => sub.name) : []),
    ]);
  return [...new Set(names)];
}
