export interface TreeNode {
  name: string;
  children?: TreeNode[];
  parent?: {
    name: string;
  };
}

export interface OntologyNode extends TreeNode {
  code?: string;
  definition?: string;
  ontologyTermURI?: string;
  order?: number;
}

export interface Dependencies {
  name: string;
  url?: string;
  fetchPriority?: OntologyNode;
}

export interface DependenciesCSS {
  name: string;
  url?: string;
  fetchPriority?: OntologyNode;
  type?: OntologyNode;
}

export interface DependenciesJS {
  name: string;
  url?: string;
  fetchPriority?: OntologyNode;
  type?: OntologyNode;
  async?: boolean;
  defer?: boolean;
}

export interface DeveloperPage {
  name: string;
  description?: string;
  html?: string;
  css?: string;
  javascript?: string;
  dependencies?: Dependencies[];
  enableBaseStyles?: boolean;
  enableButtonStyles?: boolean;
  enableFullScreen?: boolean;
}

export interface Pages {
  name: string;
  description?: string;
}
