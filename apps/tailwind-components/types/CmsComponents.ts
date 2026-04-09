import type {
  IHeadings,
  IParagraphs,
  IImages,
  INavigationGroups,
} from "./cms.ts";

export interface IPageComponent
  extends IHeadings,
    IParagraphs,
    IImages,
    INavigationGroups {}
