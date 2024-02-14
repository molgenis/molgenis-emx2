import { Axios } from "axios";
import { IRow } from "../Interfaces/IRow";
import type { ISetting, ITableMetaData } from "meta-data-utils";
import { IQueryMetaData } from "./IQueryMetaData";

export interface IClient {
  newClient: (schemaId?: string, externalAxios?: Axios) => INewClient;
}

export type aggFunction = "count" | "_sum";

export interface INewClient {
  insertDataRow: (
    rowData: IRow,
    tableId: string,
    schemaId: string
  ) => Promise<any>;
  updateDataRow: (
    rowData: IRow,
    tableId: string,
    schemaId: string
  ) => Promise<any>;
  deleteRow: (rowKey: IRow, tableId: string) => Promise<any>;
  deleteAllTableData: (tableId: string) => Promise<any>;
  fetchSchemaMetaData: () => Promise<any>;
  fetchTableMetaData: (tableId: string) => Promise<ITableMetaData>;
  fetchTableData: (tableId: string, properties: IQueryMetaData) => Promise<any>;
  fetchTableDataValues: (
    tableId: string,
    properties: IQueryMetaData
  ) => Promise<any>;
  fetchRowData: (
    tableId: string,
    rowId: IRow,
    expandLevel?: number
  ) => Promise<any>;
  fetchAggregateData: (
    tableId: string,
    selectedColumn: { id: string; column: string },
    selectedRow: { id: string; column: string },
    filter: Object,
    aggFunction?: aggFunction,
    aggField?: string
  ) => Promise<any>;
  fetchSettings: () => Promise<any>;
  fetchSettingValue: (name: string) => Promise<any>;
  saveSetting: (key: string, value: any) => Promise<any>;
  saveTableSettings: (settings: ISetting[]) => Promise<any>;
  clearCache: () => void;
  convertRowToPrimaryKey: (
    row: IRow,
    tableId: string
  ) => Promise<Record<string, any>>;
  fetchOntologyOptions: (tableName: string) => Promise<any>;
}
