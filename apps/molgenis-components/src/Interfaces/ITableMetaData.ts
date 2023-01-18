import { IColumn } from "./IColumn";
import { ISetting } from "./ISetting";
import { ILocale } from "./ILocales";

export interface ITableMetaData {
  columns?: IColumn[];
  name: string;
  tableType: string;
  id: string;
  descriptions: [ILocale];
  labels: [ILocale];
  externalSchema: string;
  semantics: string;
  settings: ISetting[];
}
