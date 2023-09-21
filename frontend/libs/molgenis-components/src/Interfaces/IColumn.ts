import { ILocale } from "./ILocales";

export interface IColumn {
  columnType: string;
  id: string;
  name: string;
  computed?: string;
  conditions?: string[];
  descriptions?: ILocale[];
  key?: number;
  labels?: ILocale[];
  position?: number;
  readonly?: string;
  refBack?: string;
  refLabel?: string;
  refLabelDefault?: string;
  refLink?: string;
  refSchema?: string;
  refTable?: string;
  required?: boolean;
  semantics?: string[];
  validation?: string;
  visible?: string;
}
