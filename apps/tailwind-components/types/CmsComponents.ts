import type {
  IHeaders,
  ISections,
  IHeadings,
  IParagraphs,
  IImages,
  INavigationGroups,
  IDeveloperPages,
  IConfigurablePages,
} from "./cms.ts";

import type {
  ITableMetaData,
  ISchemaMetaData,
} from "../../metadata-utils/src/types.js";

export interface IPageComponent
  extends IHeaders,
    ISections,
    IHeadings,
    IParagraphs,
    IImages,
    INavigationGroups {}

export interface IContainerMetadata {
  page: IDeveloperPages | IConfigurablePages;
  metadata?: ITableMetaData[];
}

export interface IContainerResponse {
  data: {
    Containers: IDeveloperPages[] | IConfigurablePages[];
    _schema: ISchemaMetaData;
  };
}

export type ICmsJsFetchPriority = "high" | "low" | "auto";
