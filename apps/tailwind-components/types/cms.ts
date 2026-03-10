// Generated (on: 2026-03-10T16:07:22.720993) from Generator.java for schema: cms

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
  enableFullScreenWidth?: boolean;
  inContainer?: any;
  components?: IComponents[];
  componentOrder?: IComponentOrders[];
  id: string;
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
  inBlock?: any;
  id: string;
}

export interface IComponents_agg {
  count: number;
}

export interface IConfigurablePages {
  name: string;
  description?: string;
  blocks?: IBlocks[];
  blockOrder?: IBlockOrders[];
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
  title?: string;
  subtitle?: string;
  backgroundImage?: any;
  titleIsCentered?: boolean;
  enableFullScreenWidth?: boolean;
  inContainer?: any;
  components?: IComponents[];
  componentOrder?: IComponentOrders[];
  id: string;
}

export interface IHeaders_agg {
  count: number;
}

export interface IHeadings {
  text?: string;
  level?: number;
  headingIsCentered?: boolean;
  headingIsHidden?: boolean;
  inBlock?: any;
  id: string;
}

export interface IHeadings_agg {
  count: number;
}

export interface IImages {
  displayName?: string;
  image?: IFile;
  alt?: string;
  width?: string;
  height?: string;
  imageIsCentered?: boolean;
  inBlock?: any;
  id: string;
}

export interface IImages_agg {
  count: number;
}

export interface INavigationCards {
  title?: string;
  description?: string;
  url?: string;
  urlLabel?: string;
  urlIsExternal?: boolean;
  displayedInNavigationGroup?: any;
  inBlock?: any;
  id: string;
}

export interface INavigationCards_agg {
  count: number;
}

export interface INavigationGroups {
  links?: INavigationCards[];
  inBlock?: any;
  id: string;
}

export interface INavigationGroups_agg {
  count: number;
}

export interface IOrderedLists {
  items?: string[];
  inBlock?: any;
  id: string;
}

export interface IOrderedLists_agg {
  count: number;
}

export interface IParagraphs {
  text?: string;
  paragraphIsCentered?: boolean;
  inBlock?: any;
  id: string;
}

export interface IParagraphs_agg {
  count: number;
}

export interface ISections {
  enableFullScreenWidth?: boolean;
  inContainer?: any;
  components?: IComponents[];
  componentOrder?: IComponentOrders[];
  id: string;
}

export interface ISections_agg {
  count: number;
}

export interface ITextElements {
  text?: string;
  inBlock?: any;
  id: string;
}

export interface ITextElements_agg {
  count: number;
}

export interface IUnorderedLists {
  items?: string[];
  inBlock?: any;
  id: string;
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
