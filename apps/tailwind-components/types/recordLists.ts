import type { DisplayConfig } from "../app/types/display";

export type RecordListConfig = Record<string, DisplayConfig>;

export interface RecordList {
  displayConfig: DisplayConfig;
  schemaId: string;
  tableId: string;
  filter: Record<string, unknown>;
}
