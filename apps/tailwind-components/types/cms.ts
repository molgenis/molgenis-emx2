// Generated (on: 2026-01-23T15:05:52.322381) from Generator.java for schema: cms

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
}

export interface IBlocks_agg {
  count: number;
}

export interface IComponents {
  id: string;
  inBlock?: any;
}

export interface IComponents_agg {
  count: number;
}

export interface IConfigurablePages {
  name: string;
  description?: string;
  blocks?: IBlocks[];
}

export interface IConfigurablePages_agg {
  count: number;
}

export interface IContainers {
  name: string;
  description?: string;
}

export interface IContainers_agg {
  count: number;
}

export interface IDependencies {
  name: string;
  url?: string;
  fetchPriority?: IOntologyNode;
}

export interface IDependencies_agg {
  count: number;
}

export interface IDependenciesCSS {
  name: string;
  url?: string;
  fetchPriority?: IOntologyNode;
}

export interface IDependenciesCSS_agg {
  count: number;
}

export interface IDependenciesJS {
  name: string;
  url?: string;
  fetchPriority?: IOntologyNode;
  async?: boolean;
  defer?: boolean;
}

export interface IDependenciesJS_agg {
  count: number;
}

export interface IDeveloperPages {
  name: string;
  description?: string;
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

export interface IHeaders {
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

export interface IHeadings {
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
  inBlock?: any;
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

export interface IParagraphs {
  id: string;
  inBlock?: any;
  text?: string;
  isCentered?: boolean;
}

export interface IParagraphs_agg {
  count: number;
}

export interface ISections {
  id: string;
  inContainer?: any;
  components?: IComponents[];
  enableFullScreenWidth?: boolean;
}

export interface ISections_agg {
  count: number;
}

export interface ITextElements {
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
