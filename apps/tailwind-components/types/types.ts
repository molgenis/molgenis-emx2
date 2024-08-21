export interface INode {
  name: string;
  description?: string;
}

export interface ITreeNode extends INode {
  children: 
  ITreeNode[];
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
  orderby: {
    column: string;
    direction: sortDirection
  };
}
  