import { ITableMetaData } from "./ITableMetaData";

export type IRow = Record<string, string | ITableMetaData | any>;
