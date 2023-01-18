export interface IColumn {
  columnType: string;
  conditions: string[];
  description: string;
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
