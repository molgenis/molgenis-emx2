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

export type ButtonPosition = "left" | "center" | "right";

export type ButtonIconPosition = "left" | "right";

export type INotificationType =
  | "light"
  | "dark"
  | "success"
  | "error"
  | "warning"
  | "info";