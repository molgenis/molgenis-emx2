type selectStatus = "complete" | "partial" | "none";

export interface ITreeNode {
  name: string;
  description?: string;
  selected: selectStatus;
  expanded: boolean;
  children?: ITreeNode[];
}
