import { Axios } from "axios";
import type {
  ISetting,
  ITableMetaData,
} from "../../../metadata-utils/src/types";
import type { IRow } from "../Interfaces/IRow";
import type { IQueryMetaData } from "../../../metadata-utils/src/IQueryMetaData";

export interface IClient {
  newClient: (schemaId?: string, externalAxios?: Axios) => INewClient;
}

export type AggFunction = "count" | "_sum";

export interface INewClient {
  insertDataRow: (
    rowData: IRow,
    tableId: string,
    schemaId: string
  ) => Promise<unknown>;
  updateDataRow: (
    rowData: IRow,
    tableId: string,
    schemaId: string
  ) => Promise<unknown>;
  deleteRow: (rowKey: IRow, tableId: string) => Promise<unknown>;
  deleteAllTableData: (tableId: string) => Promise<unknown>;
  fetchSchemaMetaData: () => Promise<unknown>;
  fetchTableMetaData: (tableId: string) => Promise<ITableMetaData>;
  fetchTableData: (
    tableId: string,
    properties: IQueryMetaData
  ) => Promise<unknown>;
  fetchTableDataValues: (
    tableId: string,
    properties: IQueryMetaData
  ) => Promise<unknown>;
  fetchRowData: (
    tableId: string,
    rowId: IRow,
    expandLevel?: number
  ) => Promise<unknown>;
  fetchAggregateData: (
    tableId: string,
    selectedColumn: { id: string; column: string },
    selectedRow: { id: string; column: string },
    filter: Record<string, unknown>,
    aggFunction?: AggFunction,
    aggField?: string
  ) => Promise<unknown>;
  fetchSettings: () => Promise<unknown>;
  fetchSettingValue: (name: string) => Promise<unknown>;
  saveSetting: (key: string, value: unknown) => Promise<unknown>;
  saveTableSettings: (settings: ISetting[]) => Promise<unknown>;
  clearCache: () => void;
  convertRowToPrimaryKey: (
    row: IRow,
    tableId: string
  ) => Promise<Record<string, unknown>>;
  fetchOntologyOptions: (tableName: string) => Promise<unknown>;
  getPrimaryKeyFields: (schemaId: string, tableId: string) => Promise<string[]>;
}
