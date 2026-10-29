import type { IColumn, IRow } from "../../metadata-utils/src/types";
import type { DisplayConfig } from "../app/types/display";

export interface ColumnConfig {
  display?: Partial<DisplayConfig>;
  route?: string;
}

export interface TableConfig {
  display?: DisplayConfig;
  columns?: Record<string, ColumnConfig>;
}

export interface ListSeed {
  columns: IColumn[];
  rows: IRow[];
  count: number;
}

export interface RecordList {
  displayConfig: DisplayConfig;
  schemaId: string;
  tableId: string;
  filter: Record<string, unknown>;
  pageSize: number;
  seed?: ListSeed;
  linkTo?: (row: IRow) => string;
}
