// Generated (on: 2026-01-28T08:41:05.654599) from Generator.java for schema: cms

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

export interface IBlockOrders {
  id: string;
  configurablePage?: any;
  block?: any;
  order?: number;
}

export interface IBlockOrders_agg {
  count: number;
}

export interface IBlocks {
  mg_tableclass?: string;
  id: string;
  inContainer?: any;
  components?: IComponents[];
  componentOrder?: IComponentOrders[];
  enableFullScreenWidth?: boolean;
}

export interface IBlocks_agg {
  count: number;
}

export interface IComponentOrders {
  id: string;
  block?: any;
  component?: any;
  order?: number;
}

export interface IComponentOrders_agg {
  count: number;
}

export interface IComponents {
  mg_tableclass?: string;
  id: string;
  inBlock?: any;
}

export interface IComponents_agg {
  count: number;
}

export interface IConfigurablePages {
  mg_tableclass?: string;
  name: string;
  description?: string;
  blocks?: IBlocks[];
  blockOrder?: IBlockOrders[];
}

export interface IConfigurablePages_agg {
  count: number;
}

export interface IContainers {
  mg_tableclass?: string;
  name: string;
  description?: string;
}

export interface IContainers_agg {
  count: number;
}

export interface IDependencies {
  mg_tableclass?: string;
  name: string;
  url?: string;
  fetchPriority?: IOntologyNode;
}

export interface IDependencies_agg {
  count: number;
}

export interface IDependenciesCSS {
  mg_tableclass?: string;
  name: string;
  url?: string;
  fetchPriority?: IOntologyNode;
}

export interface IDependenciesCSS_agg {
  count: number;
}

export interface IDependenciesJS {
  mg_tableclass?: string;
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
  mg_tableclass?: string;
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
  mg_tableclass?: string;
  id: string;
  inContainer?: any;
  components?: IComponents[];
  componentOrder?: IComponentOrders[];
  enableFullScreenWidth?: boolean;
  title?: string;
  subtitle?: string;
  backgroundImage?: any;
  titleIsCentered?: boolean;
}

export interface IHeaders_agg {
  count: number;
}

export interface IHeadings {
  mg_tableclass?: string;
  id: string;
  inBlock?: any;
  text?: string;
  level?: number;
  headingIsCentered?: boolean;
}

export interface IHeadings_agg {
  count: number;
}

export interface IImages {
  mg_tableclass?: string;
  id: string;
  inBlock?: any;
  displayName?: string;
  image?: IFile;
  alt?: string;
  width?: string;
  height?: string;
  imageIsCentered?: boolean;
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
  mg_tableclass?: string;
  id: string;
  inBlock?: any;
  text?: string;
  paragraphIsCentered?: boolean;
}

export interface IParagraphs_agg {
  count: number;
}

export interface ISections {
  mg_tableclass?: string;
  id: string;
  inContainer?: any;
  components?: IComponents[];
  componentOrder?: IComponentOrders[];
  enableFullScreenWidth?: boolean;
}

export interface ISections_agg {
  count: number;
}

export interface ITextElements {
  mg_tableclass?: string;
  id: string;
  inBlock?: any;
  text?: string;
}

export interface ITextElements_agg {
  count: number;
}

export interface IUnorderedLists {
  mg_tableclass?: string;
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
