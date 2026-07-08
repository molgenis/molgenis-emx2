export type SchemaTableType = "DATA" | "ONTOLOGIES";

export interface SchemaTableEntry {
  id: string;
  label: string;
  tableType: SchemaTableType;
  role?: string | null;
  description: string;
}

export function filterTablesByTypeAndRole(
  tables: SchemaTableEntry[],
  tableType: SchemaTableType,
  role: "MAIN" | "DETAIL"
): SchemaTableEntry[] {
  return tables
    .filter((t) => {
      if (t.tableType !== tableType) return false;
      const normalizedRole = t.role?.toUpperCase();
      if (role === "MAIN") {
        return !normalizedRole || normalizedRole === "MAIN";
      }
      return normalizedRole === "DETAIL";
    })
    .sort((a, b) => a.label.localeCompare(b.label));
}
