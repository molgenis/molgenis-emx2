import type {
  ColumnDisplay,
  ColumnRole,
  IColumn,
  IRow,
} from "../../metadata-utils/src/types";
import type { Layout } from "../app/types/display";

export interface ColumnConfig {
  role?: ColumnRole;
  display?: ColumnDisplay;
  route?: string;
}

export interface TableConfig {
  columns?: Record<string, ColumnConfig>;
}

export interface ListSeed {
  columns: IColumn[];
  rows: IRow[];
  count: number;
}

export interface RecordList {
  layout: Layout;
  schemaId: string;
  tableId: string;
  filter: Record<string, unknown>;
  pageSize: number;
  seed?: ListSeed;
  linkTo?: (row: IRow) => string;
}
