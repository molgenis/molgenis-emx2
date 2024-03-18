export interface ITreeNode {
  name: string;
  description?: string;
  children: ITreeNode[];
}
