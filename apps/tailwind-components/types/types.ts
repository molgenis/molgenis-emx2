import type { IColumn } from "../../metadata-utils/dist";

export type Resp<T> = {
  data: Record<string, T[]>;
};

export interface Schema {
  id: string;
  label: string;
  description: string;
}
export interface INode {
  name: string;
  description?: string;
}

export interface ITreeNode extends INode {
  children: ITreeNode[];
  parent?: string;
}

export type ButtonType =
  | "primary"
  | "secondary"
  | "tertiary"
  | "outline"
  | "disabled"
  | "filterWell";

export type ButtonSize = "tiny" | "small" | "medium" | "large";

export type ButtonIconPosition = "left" | "right";

export type INotificationType =
  | "light"
  | "dark"
  | "success"
  | "error"
  | "warning"
  | "info";

export type sortDirection = "ASC" | "DESC";
export interface ITableSettings {
  page: number;
  pageSize: number;
  orderby: {
    column: string;
    direction: sortDirection
  };
  search: string;
}

export interface ISectionField {
  meta: IColumn;
  value: any;
}

export interface ISection {
  meta: IColumn;
  fields: ISectionField[];
}

export interface IFile {
  id?: string;
  size?: number;
  extension?: string;
  url?: string;
}

export interface IDocumentation {
  name: string;
  description: string;
  url: string;
  file: IFile;
}

  