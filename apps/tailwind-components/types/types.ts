export interface INode {
  name: string;
  description?: string;
}

export interface ITreeNode extends INode {
  children: ITreeNode[];
}
