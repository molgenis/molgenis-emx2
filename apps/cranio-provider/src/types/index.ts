import type { IOrganisations } from "../../../metadata-utils/src/viz/ErnDashboard";

export interface IKeyValuePair {
  [key: string]: string;
}

export interface IMgError {
  message: string;
}

export interface IMgErrorResponse {
  response: {
    errors: IMgError[];
  };
}

export interface ICranioSchemas {
  CRANIO_PUBLIC_SCHEMA: string;
  CRANIO_PROVIDER_SCHEMA: string;
}

export interface IValueLabel {
  value: string;
  label: string;
}

export interface ICleftTypes {
  CL: number;
  CP: number;
  CLA: number;
  CLAP: number;
}

export interface ISiteErnCleftTypeCounts {
  center: ICleftTypes;
  ern: ICleftTypes;
}

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
