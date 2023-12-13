import { ITableMetaData } from "meta-data-utils";

export type IRow = Record<string, string | ITableMetaData | any>;
