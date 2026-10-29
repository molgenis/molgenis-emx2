import type { ITableMetaData } from "../../../metadata-utils/src/types";
import type { DisplayConfig } from "../types/display";
import type { ColumnConfig, TableConfig } from "../../types/recordConfig";

export function resolveTableConfig(
  configs: Record<string, TableConfig>,
  table: ITableMetaData
): TableConfig {
  const columns: Record<string, ColumnConfig> = {};
  for (const column of table.columns) {
    const entry = configs[table.id]?.columns?.[column.id];
    if (entry && column.refTableId) {
      const base = configs[column.refTableId]?.display;
      const layout = entry.display?.layout ?? base?.layout;
      columns[column.id] = {
        ...entry,
        display: layout ? { ...base, ...entry.display, layout } : undefined,
      };
    }
  }
  return { columns };
}

export function listDisplay(
  config: TableConfig | undefined,
  columnId: string
): DisplayConfig | undefined {
  const display = config?.columns?.[columnId]?.display;
  return display?.layout ? { ...display, layout: display.layout } : undefined;
}
