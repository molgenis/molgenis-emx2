import type { columnValue, IColumn } from "../../metadata-utils/src/types";

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

export interface IValueLabel {
  value: any;
  label?: string;
}

export interface ITreeNode extends INode {
  children: ITreeNode[];
}

export interface ITreeNodeState extends ITreeNode {
  /* if a node should be shown, used for search filter */
  visible?: boolean;
  /* label will be shown if provided instead of name */
  label?: string;
  /* code from a code system */
  code?: string;
  /* code system if provided */
  codesystem?: string;
  /* uri where the code comes from */
  uri?: string;
  /* if a node is selected, intermediate or unselected*/
  selected?: SelectionState; //'unselected','selected','intermediate'
  /* if a node should be shown expanded */
  expanded?: boolean;
  /* helper to quickly navigate to parent node */
  parent?: string;
  /* helper to quickly navigate to parent node */
  parentNode?: ITreeNodeState;
  /* extension of children */
  children: ITreeNodeState[];
  /* if a node is selectable */
  selectable: boolean;
}

export type SelectionState = "selected" | "intermediate" | "unselected";

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
    direction: sortDirection;
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
  filename?: string;
  extension?: string;
  url?: string;
}

export interface IDocumentation {
  name: string;
  description: string;
  url: string;
  file: IFile;
}

export interface IRadioOptionsData {
  value: columnValue;
  label?: string;
  checked?: boolean | undefined;
}

export interface IInputProps {
  id: string;
  placeholder?: string;
  describedBy?: string;
  invalid?: boolean;
  valid?: boolean | undefined;
  disabled?: boolean | undefined;
}
