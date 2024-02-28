export interface ITreeNode {
  name: string;
  description?: string;
  selected: boolean;
  expanded: boolean;
  children?: ITreeNode[];
}
