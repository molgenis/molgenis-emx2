import type { ISchemaMetaData } from "../../../metadata-utils/src/types";
import type { TableConfig } from "../../types/recordConfig";

export function enhanceMetadata(
  schema: ISchemaMetaData,
  configs: Record<string, TableConfig>
): ISchemaMetaData {
  return {
    ...schema,
    tables: schema.tables.map((table) => ({
      ...table,
      columns: table.columns.map((column) => {
        const { role, display } = configs[table.id]?.columns?.[column.id] ?? {};
        return {
          ...column,
          ...(role && { role }),
          ...(display && { display }),
        };
      }),
    })),
  };
}
