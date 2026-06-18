import type { IOrganisations } from "./ErnDashboard";
import type { IComponents } from "./ErnDashboard";

export interface IComponentsResponse {
  data?: {
    Components?: IComponents[];
  };
}

export interface IOrganisationsResponse {
  data?: {
    Organisations?: IOrganisations[];
  };
}

export interface IFetchBody {
  query: string;
  variables?: { filter: { name: { equals: string } } };
}
