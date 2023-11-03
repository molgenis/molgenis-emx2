import { ITableMetaData } from "meta-data-utils/dist/types";

export type IRow = Record<string, string | ITableMetaData | any>;
