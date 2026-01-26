export interface IFile {
  id?: string;
  size?: number;
  extension?: string;
  url?: string;
}

export interface ITreeNode {
  name: string;
  children?: ITreeNode[];
  parent?: {
    name: string;
  };
}

export interface IOntologyNode extends ITreeNode {
  code?: string;
  definition?: string;
  ontologyTermURI?: string;
  order?: number;
}

export interface IBlocks {
  id: string;
  inContainer?: any;
  components?: IComponents[];
  enableFullScreenWidth?: boolean;
  mg_tableclass?: string;
}

export interface IBlocks_agg {
  count: number;
}

export interface IComponents {
  id: string;
  inBlock?: any;
  mg_tableclass?: string;
}

export interface IComponents_agg {
  count: number;
}

export interface IConfigurablePages extends IContainers {
  blocks?: IBlocks[];
}

export interface IConfigurablePages_agg {
  count: number;
}

export interface IContainers {
  name: string;
  description?: string;
  mg_tableclass?: string;
}

export interface IContainers_agg {
  count: number;
}

export interface IDependencies {
  name: string;
  url?: string;
  fetchPriority?: IOntologyNode;
  mg_tableclass?: string;
}

export interface IDependencies_agg {
  count: number;
}

export interface IDependenciesCSS extends IDependencies {}

export interface IDependenciesCSS_agg {
  count: number;
}

export interface IDependenciesJS extends IDependencies {
  async?: boolean;
  defer?: boolean;
}

export interface IDependenciesJS_agg {
  count: number;
}

export interface IDeveloperPages extends IContainers {
  html?: string;
  css?: string;
  javascript?: string;
  dependencies?: IDependencies[];
  enableBaseStyles?: boolean;
  enableButtonStyles?: boolean;
  enableFullScreen?: boolean;
}

export interface IDeveloperPages_agg {
  count: number;
}

export interface IHeaders extends IBlocks {
  id: string;
  inContainer?: any;
  components?: IComponents[];
  enableFullScreenWidth?: boolean;
  title?: string;
  subtitle?: string;
  backgroundImage?: any;
  isCentered?: boolean;
}

export interface IHeaders_agg {
  count: number;
}

export interface IHeadings extends IComponents {
  id: string;
  inBlock?: any;
  text?: string;
  isCentered?: boolean;
  level?: number;
}

export interface IHeadings_agg {
  count: number;
}

export interface IImages {
  id: string;
  displayName?: string;
  image?: IFile;
  alt?: string;
  width?: string;
  height?: string;
  isCentered?: boolean;
}

export interface IImages_agg {
  count: number;
}

export interface IOrderedLists {
  id: string;
  inBlock?: any;
  items?: string[];
}

export interface IOrderedLists_agg {
  count: number;
}

export interface IParagraphs extends IComponents {
  id: string;
  inBlock?: any;
  text?: string;
  isCentered?: boolean;
}

export interface IParagraphs_agg {
  count: number;
}

export interface ISections extends IBlocks {
  id: string;
  inContainer?: any;
  components?: IComponents[];
  enableFullScreenWidth?: boolean;
}

export interface ISections_agg {
  count: number;
}

export interface ITextElements extends IComponents {
  id: string;
  inBlock?: any;
  text?: string;
  isCentered?: boolean;
}

export interface ITextElements_agg {
  count: number;
}

export interface IUnorderedLists {
  id: string;
  inBlock?: any;
  items?: string[];
}

export interface IUnorderedLists_agg {
  count: number;
}

export interface IWebFetchPriorities {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IWebFetchPriorities;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IWebFetchPriorities[];
}

export interface IWebFetchPriorities_agg {
  count: number;
}
