export interface InheritableTable {
  name: string;
  tableType?: string;
  drop?: boolean;
  inheritName?: string;
  inheritSchemaName?: string;
  subclasses?: InheritableTable[];
}

export interface TableHolder {
  tables?: InheritableTable[];
}

export interface InheritableSchema extends TableHolder {
  name: string;
}

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
  table: InheritableTable,
  currentSchemaName: string
): boolean {
  return (
    Boolean(table.inheritName) &&
    Boolean(toInheritSchemaName(table.inheritSchemaName, currentSchemaName))
  );
}

export function isRootTable(
  table: InheritableTable,
  currentSchemaName: string
): boolean {
  return !table.inheritName || isCrossSchemaSubclass(table, currentSchemaName);
}

export function findLocalRootTable(
  schema: InheritableSchema,
  table: InheritableTable
): InheritableTable | undefined {
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

export function extendableTableNames(
  schema: TableHolder | null | undefined
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
