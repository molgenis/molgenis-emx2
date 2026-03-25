import type {
  IComponents,
  IOrganisations,
  IDataproviders,
  IStatistics,
} from "./ernskin";

export interface OrganisationsResponse {
  Organisations: IOrganisations[];
}

export interface ComponentsResponse {
  Components: IComponents[];
}

export interface Components extends IComponents {
  statistics: IStatistics[];
}

export interface Organisations extends IOrganisations {
  code?: string;
  city?: string;
  country?: string;
  latitude?: number;
  longitude?: number;
  providerInformation?: IDataproviders;
  hasSubmittedData?: string;
  providerIdentifier?: string;
}

export interface sexAtBirthData {
  Female?: number;
  Intersex?: number;
  Male?: number;
  Undetermined?: number;
}

export interface DashboardHighlights {
  Patients: number;
  "Member countries": number;
  "Healthcare providers": number;
}

export interface IKeyValuePair {
  [key: string]: number;
}

export interface PatientsByGroup extends IStatistics {
  "thematic disease group": string;
  patients: number;
}

export interface IAgeByGroup extends IStatistics {
  category: string;
}
