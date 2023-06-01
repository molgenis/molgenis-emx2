import { Axios } from "axios";
import { IRow } from "../Interfaces/IRow";
import { ISetting } from "../Interfaces/ISetting";
import { ITableMetaData } from "../Interfaces/ITableMetaData";
import { IQueryMetaData } from "./IQueryMetaData";

export interface IClient {
  newClient: (schemaName?: string, externalAxios?: Axios) => INewClient;
}

export interface INewClient {
  insertDataRow: (
    rowData: IRow,
    tableName: string,
    schemaName: string
  ) => Promise<any>;
  updateDataRow: (
    rowData: IRow,
    tableName: string,
    schemaName: string
  ) => Promise<any>;
  deleteRow: (rowKey: IRow, tableName: string) => Promise<any>;
  deleteAllTableData: (tableName: string) => Promise<any>;
  fetchSchemaMetaData: () => Promise<any>;
  fetchTableMetaData: (tableName: string) => Promise<ITableMetaData>;
  fetchTableData: (
    stableId: string,
    properties: IQueryMetaData
  ) => Promise<any>;
  fetchTableDataValues: (
    tableName: string,
    properties: IQueryMetaData
  ) => Promise<any>;
  fetchRowData: (
    tableName: string,
    rowId: IRow,
    expandLevel: number
  ) => Promise<any>;
  fetchAggregateData: (
    tableName: string,
    selectedColumn: { name: string; column: string },
    selectedRow: { name: string; column: string },
    filter: Object
  ) => Promise<any>;
  fetchSettings: () => Promise<any>;
  fetchSettingValue: (name: string) => Promise<any>;
  saveSetting: (key: string, value: any) => Promise<any>;
  saveTableSettings: (settings: ISetting[]) => Promise<any>;
}
