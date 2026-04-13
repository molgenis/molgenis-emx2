// Generated (on: 2026-04-13T14:16:49.810009) from Generator.java for schema: cms

export interface IMgTableClass {
  mg_tableclass?: string;
}

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

export interface IBlockOrders extends IMgTableClass {
  id: string;
  configurablePage?: any;
  block?: any;
  order?: number;
}

export interface IBlockOrders_agg {
  count: number;
}

export interface IBlocks extends IMgTableClass {
  enableFullScreenWidth?: boolean;
  inContainer?: any;
  components?: IComponents[];
  componentOrder?: IComponentOrders[];
  id: string;
  title: string;
  subtitle?: string;
  backgroundImage?: any;
  titleIsCentered?: boolean;
}

export interface IBlocks_agg {
  count: number;
}

export interface IComponentOrders extends IMgTableClass {
  id: string;
  block?: any;
  component?: any;
  order?: number;
}

export interface IComponentOrders_agg {
  count: number;
}

export interface IComponents extends IMgTableClass {
  inBlock?: any;
  id: string;
  displayName?: string;
  image: IFile;
  alt?: string;
  width?: string;
  height?: string;
  imageIsCentered?: boolean;
  links?: INavigationCards[];
  text: string;
  paragraphIsCentered?: boolean;
  level: number;
  headingIsCentered?: boolean;
  headingIsHidden?: boolean;
  title?: string;
  description?: string;
  url: string;
  urlLabel: string;
  urlIsExternal?: boolean;
  displayedInNavigationGroup?: any;
  order: number;
  items?: string[];
}

export interface IComponents_agg {
  count: number;
}

export interface IConfigurablePages extends IMgTableClass {
  name: string;
  description?: string;
  blocks?: IBlocks[];
  blockOrder?: IBlockOrders[];
}

export interface IConfigurablePages_agg {
  count: number;
}

export interface IContainers extends IMgTableClass {
  name: string;
  description?: string;
  html?: string;
  css?: string;
  javascript?: string;
  dependencies?: IDependencies[];
  enableBaseStyles?: boolean;
  enableButtonStyles?: boolean;
  enableFullScreen?: boolean;
  blocks?: IBlocks[];
  blockOrder?: IBlockOrders[];
}

export interface IContainers_agg {
  count: number;
}

export interface IDependencies extends IMgTableClass {
  name: string;
  url?: string;
  fetchPriority?: IOntologyNode;
  async?: boolean;
  defer?: boolean;
}

export interface IDependencies_agg {
  count: number;
}

export interface IDependenciesCSS extends IMgTableClass {
  name: string;
  url?: string;
  fetchPriority?: IOntologyNode;
}

export interface IDependenciesCSS_agg {
  count: number;
}

export interface IDependenciesJS extends IMgTableClass {
  name: string;
  url?: string;
  fetchPriority?: IOntologyNode;
  async?: boolean;
  defer?: boolean;
}

export interface IDependenciesJS_agg {
  count: number;
}

export interface IDeveloperPages extends IMgTableClass {
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

export interface IHeaders extends IMgTableClass {
  title: string;
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

export interface IHeadings extends IMgTableClass {
  text: string;
  level: number;
  headingIsCentered?: boolean;
  headingIsHidden?: boolean;
  inBlock?: any;
  id: string;
}

export interface IHeadings_agg {
  count: number;
}

export interface IImages extends IMgTableClass {
  displayName?: string;
  image: IFile;
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

export interface INavigationCards extends IMgTableClass {
  title?: string;
  description?: string;
  url: string;
  urlLabel: string;
  urlIsExternal?: boolean;
  displayedInNavigationGroup?: any;
  order: number;
  inBlock?: any;
  id: string;
}

export interface INavigationCards_agg {
  count: number;
}

export interface INavigationGroups extends IMgTableClass {
  links?: INavigationCards[];
  inBlock?: any;
  id: string;
}

export interface INavigationGroups_agg {
  count: number;
}

export interface IOrderedLists extends IMgTableClass {
  items?: string[];
  inBlock?: any;
  id: string;
}

export interface IOrderedLists_agg {
  count: number;
}

export interface IParagraphs extends IMgTableClass {
  text: string;
  paragraphIsCentered?: boolean;
  inBlock?: any;
  id: string;
}

export interface IParagraphs_agg {
  count: number;
}

export interface ISections extends IMgTableClass {
  enableFullScreenWidth?: boolean;
  inContainer?: any;
  components?: IComponents[];
  componentOrder?: IComponentOrders[];
  id: string;
}

export interface ISections_agg {
  count: number;
}

export interface ITextElements extends IMgTableClass {
  text: string;
  inBlock?: any;
  id: string;
  paragraphIsCentered?: boolean;
  level: number;
  headingIsCentered?: boolean;
  headingIsHidden?: boolean;
}

export interface ITextElements_agg {
  count: number;
}

export interface IUnorderedLists extends IMgTableClass {
  items?: string[];
  inBlock?: any;
  id: string;
}

export interface IUnorderedLists_agg {
  count: number;
}

export interface IWebFetchPriorities extends IMgTableClass {
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
