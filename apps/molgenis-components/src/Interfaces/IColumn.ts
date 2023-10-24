import { ILocale } from "./ILocales";

export interface IColumn {
  columnType: string;
  id: string;
  label: string;
  description?: string;
  computed?: string;
  conditions?: string[];
  descriptions?: ILocale[];
  key?: number;
  labels?: ILocale[];
  position?: number;
  readonly?: string;
  defaultValue?: string;
  refBackId?: string;
  refLabel?: string;
  refLabelDefault?: string;
  refLinkId?: string;
  refSchemaId?: string;
  refTableId?: string;
  required?: boolean;
  semantics?: string[];
  validation?: string;
  visible?: string;
}
