import { ILocale } from "./ILocales";

export interface IColumn {
  columnType: string;
  conditions: string[];
  labels: [ILocale];
  descriptions: [ILocale];
  id: string;
  key: number;
  name: string;
  position: string;
  readonly: string;
  refBack: string;
  refLabel: string;
  refLink: string;
  refSchema: string;
  refTable: string;
  required: string;
  semantics: string;
  visible: string;
  validation: string;
}
