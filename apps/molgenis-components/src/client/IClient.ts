import { Axios } from "axios";
import { IRow } from "../Interfaces/IRow";
import { ISetting } from "../Interfaces/ISetting";
import { ITableMetaData } from "../Interfaces/ITableMetaData";
import { IQueryMetaData } from "./IQueryMetaData";

export interface IClient {
  newClient: (schemaId?: string, externalAxios?: Axios) => INewClient;
}

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
    selectedColumn: { name: string; column: string },
    selectedRow: { name: string; column: string },
    filter: Object
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
}
