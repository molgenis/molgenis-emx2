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

import type { ISchemaMetaData } from "../../metadata-utils/src/types.js";

export interface IPageComponent
  extends IHeaders,
    ISections,
    IHeadings,
    IParagraphs,
    IImages,
    INavigationGroups {}

export interface IContainerMetadata {
  page: IDeveloperPages | IConfigurablePages;
  metadata?: ISchemaMetaData[];
}
