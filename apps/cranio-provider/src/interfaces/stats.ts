// Generated (on: 2024-11-20T08:59:53.448123) from Generator.java for schema: Stats

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

export interface IComponents {
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

export interface IDataproviders {
  providerIdentifier: string;
  organisation?: IOrganisations;
  hasSubmittedData?: boolean;
}

export interface IFiles {
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

export interface IInclusionCriteria {
  id: string;
  name?: string;
  type?: string;
  value?: string;
  label?: string;
}

export interface IOrganisations {
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

export interface IStatistics {
  id: string;
  label?: string;
  value?: number;
  valueOrder?: number;
  component?: IComponents;
  description?: string;
}

export interface IUsers {
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
