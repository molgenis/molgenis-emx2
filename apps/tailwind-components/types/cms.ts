export interface IFile {
  id: string;
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

export interface IContainers {
  mg_tableclass?: string;
  name: string;
  description?: string;
}

// developer page defintions
export interface IDeveloperPages extends IContainers {
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

export interface IDependencies {
  mg_tableclass?: string;
  name: string;
  url?: string;
  fetchPriority?: IOntologyNode;
}

export interface IDependenciesCSS {
  mg_tableclass?: string;
  name: string;
  url?: string;
  fetchPriority?: IOntologyNode;
}

export interface IDependenciesJS {
  mg_tableclass?: string;
  name: string;
  url?: string;
  fetchPriority?: IOntologyNode;
  async?: boolean;
  defer?: boolean;
}

// configurable page definitions
export interface IConfigurablePages extends IContainers {
  mg_tableclass?: string;
  name: string;
  description?: string;
  blockOrder: IBlockOrders[];
}

export interface IBlockOrders {
  id: string;
  block: IBlocks;
  order: number;
}

export interface IBlocks extends IHeaders, ISections {
  mg_tableclass: string;
  id: string;
  componentOrder?: IComponentOrders[];
  enableFullScreenWidth?: boolean;
}

export interface IComponentOrders {
  id: string;
  component: IComponents;
  order?: number;
}

export interface IComponents
  extends IFile,
    IHeadings,
    IImages,
    IOrderedLists,
    IParagraphs,
    ITextElements,
    IUnorderedLists {
  mg_tableclass?: string;
  id: string;
  inBlock?: IBlocks;
}

// block defintions
export interface IHeaders {
  id: string;
  title?: string;
  subtitle?: string;
  backgroundImage?: IFile;
  titleIsCentered?: boolean;
  enableFullScreenWidth?: boolean;
}

export interface ISections {
  mg_tableclass?: string;
  id: string;
  enableFullScreenWidth?: boolean;
}

export interface IHeadings {
  mg_tableclass?: string;
  id: string;
  inBlock?: IBlocks;
  text?: string;
  level?: number;
  headingIsCentered?: boolean;
}

export interface IImages {
  mg_tableclass?: string;
  id: string;
  inBlock?: IBlocks;
  displayName?: string;
  image?: IFile;
  alt?: string;
  width?: string;
  height?: string;
  imageIsCentered?: boolean;
}

export interface IOrderedLists {
  id: string;
  inBlock?: IBlocks;
  items?: string[];
}

export interface IParagraphs {
  mg_tableclass?: string;
  id: string;
  inBlock?: IBlocks;
  text?: string;
  paragraphIsCentered?: boolean;
}

export interface ITextElements {
  mg_tableclass?: string;
  id: string;
  inBlock?: IBlocks;
  text?: string;
}

export interface IUnorderedLists {
  mg_tableclass?: string;
  id: string;
  inBlock?: IBlocks;
  items?: string[];
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
