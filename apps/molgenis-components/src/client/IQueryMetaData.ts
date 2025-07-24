export interface IQueryMetaData {
  limit?: number;
  offset?: number;
  searchTerms?: string;
  filter?: Object;
  orderby?: Record<string, string>;
  expandLevel?: number;
}
