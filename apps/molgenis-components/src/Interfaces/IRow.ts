import { ITableMetaData } from "metadata-utils";

export type IRow = Record<string, string | ITableMetaData | any>;
