import { ILocale } from "./ILocales";

export interface IColumn {
  columnType: string;
  id: string;
  label: string;
  description?: string;
  computed?: string;
  conditions?: string[];
  key?: number;
  position?: number;
  readonly?: string;
  defaultValue?: string;
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
