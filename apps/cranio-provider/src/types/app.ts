import type { ICranioSchemas } from ".";
import type { IDashboardPages, ICharts } from "./schema";
import type { IOrganisations } from "./ErnDashboard";

export interface IAppPage {
  organisation: IOrganisations;
  schemaNames: ICranioSchemas;
  api: {
    graphql: {
      current: string;
      public: string;
      providers: string;
    };
  };
}

export interface IOrganisationsResponse {
  Organisations: IOrganisations[];
}

export interface IDashboardPagesResponse {
  DashboardPages: IDashboardPages[];
}

export interface IChartsResponse {
  Charts: ICharts[];
}
