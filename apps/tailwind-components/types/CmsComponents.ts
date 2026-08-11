import type {
  IHeaders,
  ISections,
  IHeadings,
  IParagraphs,
  IImages,
  INavigationGroups,
  IDeveloperPages,
  IConfigurablePages,
  IComponentOrders,
  IFile,
} from "./cms.ts";

import type { ITableMetaData } from "../../metadata-utils/src/types.js";

export interface IHeadersExtended extends IHeaders {
  image?: IFile;
}

export interface IPageComponent
  extends IHeadersExtended,
    ISections,
    IHeadings,
    IParagraphs,
    IImages,
    INavigationGroups {}

export interface IContainerMetadata {
  page: IDeveloperPages | IConfigurablePages;
  metadata?: ITableMetaData[];
}

export type ICmsJsFetchPriority = "high" | "low" | "auto";

export interface FetchGraphqlBody {
  status?: string;
  message: string;
}

export interface FetchGraphqlResponse {
  data?: {
    insert?: FetchGraphqlBody;
    delete?: FetchGraphqlBody;
    query?: FetchGraphqlBody;
    ComponentOrders?: IComponentOrders[];
  };
  errors?: FetchGraphqlBody[];
}
