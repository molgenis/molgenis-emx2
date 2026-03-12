// Generated (on: 2026-03-12T13:19:30.267073) from Generator.java for schema: ErnDashboard

export interface IMgTableClass {
  mg_tableclass: string;
}

export interface IFile {
  id?: string;
  size?: number;
  extension?: string;
  url?: string;
}

export interface ITreeNode {
  name: string;
  children?: ITreeNode[];
  parent?: {
    name: string;
  };
}

export interface IOntologyNode extends ITreeNode {
  code?: string;
  definition?: string;
  ontologyTermURI?: string;
  order?: number;
}

export interface IComponents extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IComponents;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IComponents[];
  statistics?: IStatistics[];
}

export interface IComponents_agg {
  count: number;
}

export interface IDataproviders extends IMgTableClass {
  providerIdentifier: string;
  organisation?: IOrganisations;
  hasSubmittedData?: boolean;
}

export interface IDataproviders_agg {
  count: number;
}

export interface IFiles extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IFiles;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IFiles[];
  file?: IFile;
}

export interface IFiles_agg {
  count: number;
}

export interface IInclusionCriteria extends IMgTableClass {
  id: string;
  name?: string;
  type?: string;
  value?: string;
  label?: string;
}

export interface IInclusionCriteria_agg {
  count: number;
}

export interface IOrganisations extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IOrganisations;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IOrganisations[];
  city?: string;
  country?: string;
  latitude?: number;
  longitude?: number;
  organisationType?: string;
  providerInformation?: IDataproviders[];
  image?: IFile;
  hasSchema?: boolean;
  schemaName?: string;
}

export interface IOrganisations_agg {
  count: number;
}

export interface IPublications extends IMgTableClass {
  title: string;
  doi: string;
}

export interface IPublications_agg {
  count: number;
}

export interface IStatistics extends IMgTableClass {
  id: string;
  label?: string;
  value?: number;
  valueOrder?: number;
  component?: IComponents;
  description?: string;
}

export interface IStatistics_agg {
  count: number;
}

export interface IStudies extends IMgTableClass {
  acronym: string;
  title: string;
  condition: string;
  coordinator: string;
  objective: string;
  patients: string;
  dataUsed: string;
  dataErras: string;
  patientData: string;
  dataOther: string;
  registrationNr: string;
  status: string;
  dateStart: string;
  dateEnd: string;
}

export interface IStudies_agg {
  count: number;
}

export interface IUsers extends IMgTableClass {
  order?: number;
  name: string;
  label?: string;
  tags?: string[];
  parent?: IUsers;
  codesystem?: string;
  code?: string;
  ontologyTermURI?: string;
  definition?: string;
  children?: IUsers[];
  organisation?: IOrganisations;
}

export interface IUsers_agg {
  count: number;
}
