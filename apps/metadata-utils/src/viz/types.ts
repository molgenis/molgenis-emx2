import type { IOrganisations, IComponents } from "./ErnDashboard";
import type { IDashboardPages, ICharts } from "./UiDashboard";

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

export interface IDashboardPagesResponse {
  data?: {
    DashboardPages?: IDashboardPages[];
  };
}

export interface IChartsResponse {
  data?: {
    Charts?: ICharts[];
  };
}

export interface IFetchBody {
  query: string;
  variables?: {
    filter: { name?: { equals: string }; chartId?: { equals: string } };
  };
}

export type IRecordStringNumber = Record<string, number>;
export type IRecordString = Record<string, string>;
export type IArrayStringNumber = [string, number];
